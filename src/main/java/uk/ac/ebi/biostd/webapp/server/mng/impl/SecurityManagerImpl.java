package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.AuthorizationTemplate;
import uk.ac.ebi.biostd.authz.AuthzObject;
import uk.ac.ebi.biostd.authz.BuiltInUsers;
import uk.ac.ebi.biostd.authz.GroupACR;
import uk.ac.ebi.biostd.authz.Permission;
import uk.ac.ebi.biostd.authz.PermissionProfile;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserACR;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.authz.acr.SystemPermGrpACR;
import uk.ac.ebi.biostd.authz.acr.SystemPermUsrACR;
import uk.ac.ebi.biostd.authz.acr.SystemProfGrpACR;
import uk.ac.ebi.biostd.authz.acr.SystemProfUsrACR;
import uk.ac.ebi.biostd.authz.acr.TemplatePermGrpACR;
import uk.ac.ebi.biostd.authz.acr.TemplatePermUsrACR;
import uk.ac.ebi.biostd.authz.acr.TemplateProfGrpACR;
import uk.ac.ebi.biostd.authz.acr.TemplateProfUsrACR;
import uk.ac.ebi.biostd.model.SecurityObject;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.DBInitializer;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public class SecurityManagerImpl implements SecurityManager
{
 private static Logger log;

 
 private User anonUser;

 private Collection<ACR> systemACR;
 private Map<Long, UserGroup> groupMap = new HashMap<Long, UserGroup>();
 private Map<Long, User> userMap = new HashMap<Long, User>();
 private Map<String, User> userEmailMap = new HashMap<String, User>();
 private Map<String, User> userLoginMap = new HashMap<String, User>();
 private Map<String, UserGroup> groupNameMap = new HashMap<String, UserGroup>();
 


 public SecurityManagerImpl()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }
 
 @Override
 public void init()
 {
  loadCache();
 }
 
 @Override
 public void refreshUserCache()
 {
  loadCache();
 }
 
 private void loadCache()
 {
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  try
  {
   systemACR = new ArrayList<ACR>();
   Map<Long, PermissionProfile> profMap = new HashMap<Long, PermissionProfile>();
   
   groupMap.clear();
   userMap.clear();
   userEmailMap.clear();
   userLoginMap.clear();
   groupNameMap.clear();

   Query q = em.createQuery("SELECT usr FROM User usr");
   
   @SuppressWarnings("unchecked")
   List<User> usrs = q.getResultList();
   
   for( User u : usrs )
    detachUser(u);
   
   q = em.createQuery("SELECT grp FROM UserGroup grp");
   
   @SuppressWarnings("unchecked")
   List<UserGroup> grps = q.getResultList();
   
   for( UserGroup ug : grps )
    detachGroup( ug, false );
   
   
   q = em.createQuery("SELECT acr FROM SystemPermGrpACR acr");
   
   @SuppressWarnings("unchecked")
   List<SystemPermGrpACR> spgACRs = q.getResultList();
   
   
   if( spgACRs.size() > 0 )
   {
    for( SystemPermGrpACR acr : spgACRs )
    {
     SystemPermGrpACR nr = new SystemPermGrpACR();
     nr.setAllow( acr.isAllow() );
     nr.setId( acr.getId() );
     nr.setSubject( detachGroup(acr.getSubject(), true) );
     nr.setAction( acr.getAction() );
     
     systemACR.add(nr);
    }
   }
   
   q = em.createQuery("SELECT acr FROM SystemPermUsrACR acr");
   
   @SuppressWarnings("unchecked")
   List<SystemPermUsrACR> spuACRs = q.getResultList();
   
   
   if( spuACRs.size() > 0 )
   {
    for( SystemPermUsrACR acr : spuACRs )
    {
     SystemPermUsrACR nr = new SystemPermUsrACR();
     nr.setAllow( acr.isAllow() );
     nr.setId( acr.getId() );
     nr.setSubject( detachUser(acr.getSubject()) );
     nr.setAction( acr.getAction() );
     
     systemACR.add(nr);
    }
   }
   
   
   q = em.createQuery("SELECT acr FROM SystemProfUsrACR acr");
   
   @SuppressWarnings("unchecked")
   List<SystemProfUsrACR> spruACRs = q.getResultList();
   
   
   if( spruACRs.size() > 0 )
   {
    for( SystemProfUsrACR acr : spruACRs )
    {
     SystemProfUsrACR nr = new SystemProfUsrACR();
     nr.setId( acr.getId() );
     nr.setProfile( detachProfile(acr.getProfile(), profMap ) );
     nr.setSubject( detachUser(acr.getSubject()) );
     
     systemACR.add(nr);
    }
   }
  
   
   q = em.createQuery("SELECT acr FROM SystemProfGrpACR acr");
   
   @SuppressWarnings("unchecked")
   List<SystemProfGrpACR> sprgACRs = q.getResultList();
   
   
   if( sprgACRs.size() > 0 )
   {
    for( SystemProfGrpACR acr : sprgACRs )
    {
     SystemProfGrpACR nr = new SystemProfGrpACR();
     nr.setId( acr.getId() );
     nr.setProfile( detachProfile(acr.getProfile(), profMap ) );
     nr.setSubject( detachGroup(acr.getSubject(), true) );
     
     systemACR.add(nr);
    }
   }
   
   for( User u : userMap.values() )
   {
    if( BuiltInUsers.Guest.getUserName().equals(u.getLogin() ) )
     anonUser = u;
   }
   
   if( anonUser == null )
   {

    q = em.createNamedQuery(User.GetByLoginQuery);
    q.setParameter("login", BuiltInUsers.Guest.getUserName());

    @SuppressWarnings("unchecked")
    List<User> res = q.getResultList();

    if(res.size() == 0)
     log.warn("Can't get anonymous (" + BuiltInUsers.Guest.getUserName() + ") user. Database is not initialized?");
    else
     anonUser = detachUser(res.get(0));
   }
  }
  catch( Exception e )
  {
   e.printStackTrace();
   log.error("Can't load permission cache. "+e.getMessage());
  }
  finally
  {
   em.close();
  }
   
 }
 
 
 @Override
 public synchronized User addUser(User u) throws ServiceException
 {
  EntityManager em = null;
  Collection<UserACR> newSysACR = null;

  em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();

  try
  {
   
   Query q = em.createNamedQuery(User.GetCountQuery);
   
   if( (Long)q.getSingleResult() == 0)
   {
    DBInitializer.init();
    u.setSuperuser(true);
    init();
   }
   
   Query ctq = em.createNamedQuery("AuthorizationTemplate.getByClassName");
   ctq.setParameter("className", User.class.getName());
   
   
   trn.begin();
   
   @SuppressWarnings("unchecked")
   List<AuthorizationTemplate> tpls = ctq.getResultList();
   
   if( tpls.size() == 1 )
   {
    newSysACR = new ArrayList<UserACR>();
    
    AuthorizationTemplate tpl = tpls.get(0);
    
    Collection<TemplatePermUsrACR> p4u =  tpl.getPermissionForUserACRs();
    
    if( p4u != null )
    {
     for( TemplatePermUsrACR acr : p4u )
     {
      SystemPermUsrACR sp = new SystemPermUsrACR();
      sp.setAction(acr.getAction());
      sp.setAllow(acr.isAllow());
      sp.setSubject(u);
      
      em.persist(sp);
      
      newSysACR.add( sp );
     }
    }
    
    Collection<TemplateProfUsrACR> r4u = tpl.getProfileForUserACRs();
    
    if( r4u != null )
    {
     for( TemplateProfUsrACR acr : r4u )
     {
      SystemProfUsrACR sp = new SystemProfUsrACR();
      sp.setSubject(u);
      sp.setProfile(acr.getProfile());
      
      em.persist(sp);
      
      newSysACR.add( sp );
     }
    }
   }
   
   em.persist( u );
   
   trn.commit();
  }
  catch( Exception e )
  {
   try
   {
    if( em != null )
    {
     if( trn.isActive() )
      trn.rollback();
     
    }
   }
   catch(Exception e2)
   {
    log.error("Error during transaction roolback: "+e2.getClass().getName()+": "+e2.getMessage());
    e2.printStackTrace();
   }

   e.printStackTrace();
   throw new ServiceException("JPA exception: "+e.getClass().getName()+": "+e.getMessage(),e);
  }
  
  
  User du = detachUser(u);

  if( newSysACR != null )
  {
   
   for( UserACR acr : newSysACR )
   {
    em.detach(acr);
    acr.setSubject( du );
    systemACR.add(acr);
   }
   
  }
  
  return du;
 }
 
 
 @Override
 public synchronized UserGroup addGroup(UserGroup ug) throws ServiceException
 {
  EntityManager em = null;
  Collection<GroupACR> newSysACR = null;

  em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();

  try
  {
   
   Query ctq = em.createNamedQuery("AuthorizationTemplate.getByClassName");
   ctq.setParameter("className", UserGroup.class.getName());
   
   
   trn.begin();
   
   @SuppressWarnings("unchecked")
   List<AuthorizationTemplate> tpls = ctq.getResultList();
   
   if( tpls.size() == 1 )
   {
    newSysACR = new ArrayList<GroupACR>();
    
    AuthorizationTemplate tpl = tpls.get(0);
    
    Collection<TemplatePermGrpACR> p4u =  tpl.getPermissionForGroupACRs();
    
    if( p4u != null )
    {
     for( TemplatePermGrpACR acr : p4u )
     {
      SystemPermGrpACR sp = new SystemPermGrpACR();
      sp.setAction(acr.getAction());
      sp.setAllow(acr.isAllow());
      sp.setSubject(ug);
      
      em.persist(sp);
      
      newSysACR.add( sp );
     }
    }
    
    Collection<TemplateProfGrpACR> r4u = tpl.getProfileForGroupACRs();
    
    if( r4u != null )
    {
     for( TemplateProfGrpACR acr : r4u )
     {
      SystemProfGrpACR sp = new SystemProfGrpACR();
      sp.setSubject(ug);
      sp.setProfile(acr.getProfile());
      
      em.persist(sp);
      
      newSysACR.add( sp );
     }
    }
   }
   
   em.persist( ug );
   
   trn.commit();
  }
  catch( Exception e )
  {
   try
   {
    if( em != null )
    {
     if( trn.isActive() )
      trn.rollback();
     
    }
   }
   catch(Exception e2)
   {
    log.error("Error during transaction roolback: "+e2.getClass().getName()+": "+e2.getMessage());
    e2.printStackTrace();
   }

   e.printStackTrace();
   throw new ServiceException("JPA exception: "+e.getClass().getName()+": "+e.getMessage(),e);
  }
  
  
  UserGroup du = detachGroup(ug, false);

  if( newSysACR != null )
  {
   
   for( GroupACR acr : newSysACR )
   {
    em.detach(acr);
    acr.setSubject( du );
    systemACR.add(acr);
   }
   
  }
  
  return du;
 }
 
 @Override
 public synchronized void removeExpiredUsers()
 {
  List<Long> expUsers = new ArrayList<Long>();
  
  long now = System.currentTimeMillis();
  
  Iterator<User> uitr = userMap.values().iterator();
  
  while( uitr.hasNext() )
  {
   User u = uitr.next();
   
   if( ! u.isActive() && u.getActivationKey() != null && u.getKeyTime() + BackendConfig.getActivationTimeout() < now )
   {
    expUsers.add(u.getId());
    uitr.remove();
    
    if( u.getGroups() != null )
    {
     for( UserGroup g : u.getGroups() )
      g.getUsers().remove(u);
    }
   }
  }
  
  if( expUsers.size() == 0 )
   return;
  
  uitr = userEmailMap.values().iterator();
  while( uitr.hasNext() )
  {
   if( expUsers.contains( uitr.next().getId() ) )
    uitr.remove();
  }
  
  uitr = userLoginMap.values().iterator();
  while( uitr.hasNext() )
  {
   if( expUsers.contains( uitr.next().getId() ) )
    uitr.remove();
  }

  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

  Query q = em.createNamedQuery(User.DelByIDsQuery);
  
  EntityTransaction trn = em.getTransaction();
  
  q.setParameter("ids", expUsers);
  
  trn.begin();
  
  q.executeUpdate();
  
  trn.commit();
  
 }
 
 private User detachUser( User u ) // to pull user structure from the DB. We need this to overcome lazy loading
 {
  User du = userMap.get(u.getId());
  
  if( du != null )
   return du;
  
  du = User.makeCopy(u);
 
  userMap.put(du.getId(), du);
  
  if( du.getEmail() != null && du.getEmail().length() > 0 )
   userEmailMap.put(du.getEmail(), du);
  
  if( du.getLogin() != null && du.getLogin().length() > 0 )
   userLoginMap.put(du.getLogin(), du);

  if( u.getGroups() != null  )
  {
   Collection<UserGroup> grps = new ArrayList<UserGroup>( u.getGroups().size() );
   
   for( UserGroup g : u.getGroups() )
    grps.add( detachGroup(g, false) );

   du.setGroups(grps);
  }
  
  
  return du;
 }
 
 private PermissionProfile detachProfile( PermissionProfile pr, Map<Long, PermissionProfile> pmap ) // to pull profile structure from the DB. We need this to overcome lazy loading
 {
  PermissionProfile np=pmap.get(pr.getId());
  
  if( np != null )
   return np;

  np = new PermissionProfile();
  
  np.setId( pr.getId() );
  np.setDescription( pr.getDescription() );
  
  pmap.put(np.getId(), np);
  
  if( pr.getPermissions() != null && pr.getPermissions().size() > 0 )
  {
   Collection<Permission> pms = new ArrayList<Permission>(pr.getPermissions().size()); 
   
   for( Permission pm : pr.getPermissions() )
   {
    Permission npm = new Permission();
    npm.setAction( pm.getAction());
    npm.setAllow(pm.isAllow());
    npm.setId( pm.getId() );
    
    pms.add( npm );
   }
   
   np.setPermissions(pms);
  }
  
  if( pr.getProfiles() != null && pr.getProfiles().size() > 0 )
  {
   Collection<PermissionProfile> pps = new ArrayList<PermissionProfile>( pr.getProfiles().size() );
   
   for( PermissionProfile pp : pr.getProfiles() )
    pps.add( detachProfile(pp, pmap) );
   
   np.setProfiles(pps);
  }
  
  return np;
 }
 
 private UserGroup detachGroup( UserGroup g, boolean ldUsr ) // to pull group structure from the DB. We need this to overcome lazy loading
 {
  UserGroup ug = groupMap.get(g.getId());
  
  if( ug != null )
   return ug;
  
  ug = new UserGroup();
  
  ug.setDescription( g.getDescription() );
  ug.setId( g.getId() );
  ug.setName( g.getName() );
  ug.setOwner( detachUser(g.getOwner()) );
  ug.setProject( g.isProject() );
  ug.setSecret(g.getSecret());
  
  groupMap.put( ug.getId(), ug);
  groupNameMap.put( ug.getName(), ug);

  
  if( g.getUsers() != null && g.getUsers().size() > 0 && ldUsr )
  {
   Collection<User> usrs = new ArrayList<User>( g.getUsers().size() );
   
   for( User u : g.getUsers() )
    usrs.add( detachUser(u) );

   ug.setUsers(usrs);
  }

  if( g.getGroups() != null && g.getGroups().size() > 0 )
  {
   Collection<UserGroup> grps = new ArrayList<UserGroup>( g.getGroups().size() );

   for( UserGroup sg : g.getGroups() )
    grps.add( detachGroup(sg, ldUsr) );
   
   ug.setGroups( grps );
  }
  
  return ug;
 }
 
 
 private boolean checkSystemPermission(SystemAction act, User usr)
 {
  if( usr.isSuperuser() )
   return true;
  
  boolean allow = false;
  
  for( ACR acr : systemACR )
  {
   Permit p = acr.checkPermission(act, usr);
   
   if( p == Permit.DENY )
    return false;
   else if( p == Permit.ALLOW )
    allow = true;
  }
  
  return allow;
 } 
 
 @Override
 public boolean mayUserCreateSubmission(User usr)
 {
  return checkSystemPermission(SystemAction.CREATESUBM, usr);
 }
 
 @Override
 public boolean mayUserCreateGroup(User usr)
 {
  return checkSystemPermission(SystemAction.CREATEGROUP, usr);
 }


 public boolean checkSubmissionPermission(Submission sbm, User usr, SystemAction act)
 {
  if( sbm.getOwner().equals( usr ) || usr.isSuperuser() )
   return true;
  
  return checkObjectPermission(sbm, usr, act);
 }

 
 @Override
 public boolean mayUserUpdateSubmission(Submission sbm, User usr)
 {
  return checkSubmissionPermission(sbm, usr, SystemAction.CHANGE);
 }

 @Override
 public boolean mayUserDeleteSubmission(Submission sbm, User usr)
 {
  
  return checkSubmissionPermission(sbm, usr, SystemAction.DELETE);
 }

 @Override
 public boolean mayUserReadSubmission(Submission sbm, User usr)
 {
  return checkSubmissionPermission(sbm, usr, SystemAction.READ);
 }

 @Override
 public boolean mayUserAttachToSubmission(Submission sbm, User usr )
 {
  return checkSubmissionPermission(sbm, usr, SystemAction.ATTACHSUBM);
 }
 

 @Override
 public boolean mayEveryoneReadSubmission(Submission submission)
 {
  if( anonUser.getLogin().equals( submission.getOwner().getLogin() ) )
   return true;
  
  return checkObjectPermission(submission, anonUser, SystemAction.READ);
 }
 
 private boolean checkObjectPermission( SecurityObject obj, User usr, SystemAction act )
 {
  boolean allow = false;
  
  if( obj.getAccessTags() == null )
   return false;
  
  for( AccessTag atg : obj.getAccessTags() )
  {
   Permit p = atg.checkDelegatePermission(act, usr);
   
   if( p == Permit.DENY )
    return false;
   else if( p == Permit.ALLOW )
    allow = true;
  }
  
  return allow;
 }

 @Override
 public boolean mayUserCreateIdGenerator(User usr)
 {
  return checkSystemPermission(SystemAction.CREATEIDGEN, usr);
 }

 @Override
 public void applyTemplate(AuthzObject obj, AuthorizationTemplate tpl)
 {
  Collection<TemplatePermUsrACR> p4u =  tpl.getPermissionForUserACRs();
  
  if( p4u != null )
  {
   for( TemplatePermUsrACR acr : p4u )
    obj.addPermissionForUserACR(acr.getSubject(), acr.getAction(), acr.isAllow());
  }
  
  Collection<TemplatePermGrpACR> p4g = tpl.getPermissionForGroupACRs();
  
  if( p4g != null )
  {
   for( TemplatePermGrpACR acr : p4g )
    obj.addPermissionForGroupACR(acr.getSubject(), acr.getAction(), acr.isAllow());
  }
  
  Collection<TemplateProfUsrACR> r4u = tpl.getProfileForUserACRs();
  
  if( r4u != null )
  {
   for( TemplateProfUsrACR acr : r4u )
    obj.addProfileForUserACR(acr.getSubject(), acr.getProfile());
  }
  
  Collection<TemplateProfGrpACR> r4g = tpl.getProfileForGroupACRs();
  
  if( r4u != null )
  {
   for( TemplateProfGrpACR acr : r4g )
    obj.addProfileForGroupACR(acr.getSubject(), acr.getProfile());
  }

 }

 @Override
 public User getAnonymousUser()
 {
  return anonUser;
 }

 @Override
 public User getUserById(long id)
 {
  return userMap.get(id);
 }

 @Override
 public User getUserByLogin(String login)
 {
  return userLoginMap.get(login);
 }

 @Override
 public User getUserByEmail(String email)
 {
  return userEmailMap.get(email);
 }

 @Override
 public boolean mayUserManageTags(User usr)
 {
  return checkSystemPermission(SystemAction.MANAGETAGS, usr);
 }



 @Override
 public UserGroup getGroup(String name)
 {
  return groupNameMap.get(name);
 }

 @Override
 public boolean mayUserReadGroupFiles(User user, UserGroup g)
 {
  if( user.getGroups() == null )
   return false;
  
  for( UserGroup ug : user.getGroups() )
  {
   if( ug.getId() == g.getId() )
    return true;
  }
  
  return false;
 }
 
 @Override
 public boolean mayUserWriteGroupFiles(User user, UserGroup g)
 {
  return mayUserReadGroupFiles( user,  g ); //To be changed for something smart
 }

}
