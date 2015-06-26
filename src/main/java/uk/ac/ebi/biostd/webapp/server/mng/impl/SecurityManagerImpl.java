package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.BuiltInUsers;
import uk.ac.ebi.biostd.authz.Permission;
import uk.ac.ebi.biostd.authz.PermissionProfile;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.authz.acr.SystemPermGrpACR;
import uk.ac.ebi.biostd.authz.acr.SystemPermUsrACR;
import uk.ac.ebi.biostd.authz.acr.SystemProfGrpACR;
import uk.ac.ebi.biostd.authz.acr.SystemProfUsrACR;
import uk.ac.ebi.biostd.model.SecurityObject;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;

public class SecurityManagerImpl implements SecurityManager
{
 private static Logger log;

 
 private User anonUser;

 private Collection<ACR> systemACR;
 
 public SecurityManagerImpl()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  loadCache();
 }
 
 private void loadCache()
 {
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  try
  {
   
   systemACR = new ArrayList<ACR>();
   
   boolean listValid = true;

   Query q = em.createQuery("SELECT acr FROM SystemPermGrpACR");
   
   @SuppressWarnings("unchecked")
   List<SystemPermGrpACR> spgACRs = q.getResultList();
   
   
   if( spgACRs.size() > 0 )
   {
    for( SystemPermGrpACR acr : spgACRs )
     listValid = listValid && traverseGroup(acr.getSubject(), true);
   }
   
   if( listValid )
    systemACR.addAll(spgACRs);
   
   
   q = em.createQuery("SELECT acr FROM SystemPermUsrACR");
   
   @SuppressWarnings("unchecked")
   List<SystemPermUsrACR> spuACRs = q.getResultList();
   
   
   if( spuACRs.size() > 0 )
   {
    for( SystemPermUsrACR acr : spuACRs )
     listValid = listValid && acr.getSubject() != null;
   }
   
   if( listValid )
    systemACR.addAll(spuACRs);
   
   
   q = em.createQuery("SELECT acr FROM SystemProfUsrACR");
   
   @SuppressWarnings("unchecked")
   List<SystemProfUsrACR> spruACRs = q.getResultList();
   
   
   if( spruACRs.size() > 0 )
   {
    for( SystemProfUsrACR acr : spruACRs )
    {
     listValid = listValid && acr.getSubject() != null;
    
     listValid = listValid && traverseProfile( acr.getProfile() );
    }
   }
   
   if( listValid )
    systemACR.addAll(spruACRs);
   
   
   q = em.createQuery("SELECT acr FROM SystemProfGrpACR");
   
   @SuppressWarnings("unchecked")
   List<SystemProfGrpACR> sprgACRs = q.getResultList();
   
   
   if( sprgACRs.size() > 0 )
   {
    for( SystemProfGrpACR acr : sprgACRs )
    {
     listValid = listValid && traverseGroup(acr.getSubject(), true);
     listValid = listValid && traverseProfile( acr.getProfile() );
    }
   }
   
   if( listValid )
    systemACR.addAll(sprgACRs);
   
   q = em.createNativeQuery("User.getByLogin");
   q.setParameter("login", BuiltInUsers.Guest.getUserName());
   
   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();
   
   if( res.size() == 0 )
   {
    log.error("Can't get anonymous ("+BuiltInUsers.Guest.getUserName()+") user");
    
    listValid = false;
   }
   else
   {
    anonUser = res.get(0);
    listValid = listValid && traverseUser(anonUser);
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
 
 private boolean traverseUser( User u ) // to pull user structure from the DB. We need this to overcome lazy loading
 {
  boolean ok = true;
  
  if( u.getLogin() == null )
   ok = false;
  
  if( u.getGroups() != null && u.getGroups().size() > 0  )
  {
   for( UserGroup g : u.getGroups() )
    ok = ok && traverseGroup(g, false);
  }
  
  return ok;
 }
 
 private boolean traverseProfile( PermissionProfile pr ) // to pull profile structure from the DB. We need this to overcome lazy loading
 {
  boolean ok = true;

  ok = pr.getId() != 0;
  
  if( pr.getPermissions() != null && pr.getPermissions().size() > 0 )
  {
   for( Permission pm : pr.getPermissions() )
   {
    if( pm.getAction() == null )
     ok = false;
   }
  }
  
  if( pr.getProfiles() != null && pr.getProfiles().size() > 0 )
  {
   for( PermissionProfile pp : pr.getProfiles() )
    ok = ok && traverseProfile(pp);
  }
  
  return ok;
 }
 
 private boolean traverseGroup( UserGroup g, boolean ldUsr ) // to pull group structure from the DB. We need this to overcome lazy loading
 {
  boolean ok = true;
  
  if( g.getName() == null )
   ok = false;
  
  if( g.getUsers() != null && g.getGroups().size() > 0 && ldUsr )
  {
   for( User u : g.getUsers() )
    if( u.getLogin() == null )
     ok = false;
  }

  if( g.getGroups() != null && g.getGroups().size() > 0 )
  {
   for( UserGroup sg : g.getGroups() )
    ok = ok && traverseGroup(sg, ldUsr);
  }
  
  return ok;
 }
 
 
 @Override
 public boolean mayUserCreateSubmission(User usr)
 {
  boolean allow = false;
  
  for( ACR acr : systemACR )
  {
   Permit p = acr.checkPermission(SystemAction.CREATESUBM, usr);
   
   if( p == Permit.DENY )
    return false;
   else if( p == Permit.ALLOW )
    allow = true;
  }
  
  return allow;
 }

 @Override
 public boolean mayUserUpdateSubmission(Submission sbm, User usr)
 {
  if( sbm.getOwner().getLogin().equals( usr.getLogin() ) || usr.isSuperuser() )
   return true;
  
  return checkObjectPermission(sbm, usr, SystemAction.CHANGE);
 }

 @Override
 public boolean mayUserDeleteSubmission(Submission sbm, User usr)
 {
  if( sbm.getOwner().getLogin().equals( usr.getLogin() ) || usr.isSuperuser() )
   return true;
  
  return checkObjectPermission(sbm, usr, SystemAction.DELETE);
 }

 @Override
 public boolean mayUserReadSubmission(Submission sbm, User usr)
 {
  if( sbm.getOwner().getLogin().equals( usr.getLogin() ) || usr.isSuperuser() )
   return true;
  
  return checkObjectPermission(sbm, usr, SystemAction.READ);
 }

 
 @Override
 public boolean mayEveryoneReadSubmission(Submission submission)
 {
  
  if( submission.getOwner().getLogin().equals( anonUser.getLogin() ) )
   return true;
  
  return checkObjectPermission(submission, anonUser, SystemAction.READ);
 }
 
 private boolean checkObjectPermission( SecurityObject obj, User usr, SystemAction act )
 {
  boolean allow = false;
  
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

}
