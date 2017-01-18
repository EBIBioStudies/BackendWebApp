package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityException;
import uk.ac.ebi.biostd.webapp.server.mng.SessionListener;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;
import uk.ac.ebi.biostd.webapp.server.mng.UserAuxXMLFormatter;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.InvalidKeyException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.KeyExpiredException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.SystemUserMngException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserAlreadyActiveException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserMngException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserNotActiveException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserNotFoundException;

import com.pri.log.Log;


public class JPAUserManager implements UserManager, SessionListener
{

 
 public JPAUserManager()
 {
 }

 private User getUserByLoginDB(String uName)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = null;
  
  try
  {
   trn = em.getTransaction();
   
   trn.begin();
   
   Query q = em.createNamedQuery(User.GetByLoginQuery);

   q.setParameter("login", uName);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();

   if(res.size() != 0)
    return res.get(0);
  }
  finally
  {
   if( trn != null && trn.isActive() )
    trn.commit();
  }

  return null;

 }

 @Override
 public User getUserByEmail(String email)
 {
  return BackendConfig.getServiceManager().getSecurityManager().getUserByEmail(email);
 }

 @Override
 public User getUserByLogin(String login)
 {
  return BackendConfig.getServiceManager().getSecurityManager().getUserByLogin(login);
 }

 @Override
 public UserGroup getGroup(String name)
 {
  return BackendConfig.getServiceManager().getSecurityManager().getGroup(name);
 }

 
 private User getUserByEmailDB(String prm)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  Query q = em.createNamedQuery(User.GetByEMailQuery);

  q.setParameter("email", prm);

  @SuppressWarnings("unchecked")
  List<User> res = q.getResultList();

  if(res.size() != 0)
   return res.get(0);

  return null;

 }
 
 @Override
 public synchronized void addUser(User u, List<String[]> aux, boolean validateEmail, String validateURL) throws UserMngException
 {
  
  u.setSecret( UUID.randomUUID().toString() );

  UUID actKey = UUID.randomUUID();
  
  if( validateEmail )
  {
   u.setActive(false);
   u.setActivationKey(actKey.toString());
   u.setKeyTime(System.currentTimeMillis());
   
   if( aux != null )
    u.setAuxProfileInfo(UserAuxXMLFormatter.buildXML(aux));
   
   if( !AccountActivation.sendActivationRequest(u,actKey,validateURL) )
    throw new SystemUserMngException("Email confirmation request can't be sent. Please try later");
  }
  else
   u.setActive(true);
  
  try
  {
   BackendConfig.getServiceManager().getSecurityManager().addUser(u);
  }
  catch(ServiceException e)
  {
   throw new SystemUserMngException("System error",e);
  }
  
 }

 
 @Override
 public synchronized void addGroup(UserGroup ug) throws UserMngException
 {
  
  ug.setSecret( UUID.randomUUID().toString() );

  try
  {
   BackendConfig.getServiceManager().getSecurityManager().addGroup(ug);
  }
  catch(ServiceException e)
  {
   throw new SystemUserMngException("System error",e);
  }
  
  if( ! ug.isProject() )
   return;
  
  Path udpth = BackendConfig.getGroupDirPath(ug);
  Path llpth = BackendConfig.getGroupLinkPath(ug);
  
  
  try
  {
   Files.createDirectories(udpth);
   
   if( BackendConfig.isPublicDropboxes() )
   {
    try
    {
     Files.setPosixFilePermissions(udpth.getParent(), BackendConfig.rwx__x__x);
     Files.setPosixFilePermissions(udpth, BackendConfig.rwxrwxrwx);
    }
    catch(Exception e2)
    {
     Log.error("Can't set directory permissions: "+e2.getMessage());
    }
   }
   
   if( llpth != null )
    Files.createDirectories(llpth.getParent());

   try
   {
    if( llpth != null )
     Files.createSymbolicLink(llpth, udpth);
   }
   catch(Exception e2)
   {
    Log.error("System can't create symbolic links: "+e2.getMessage());
   }
   
  }
  catch(IOException e)
  {
   Log.error("Group directories were not created: "+e.getMessage(), e);
   e.printStackTrace();
  }
  
 }

 
 
 @Override
 public void sessionOpened(User u)
 {
 }

 @Override
 public void sessionClosed(User u)
 {
 }

 @Override
 public UserData getUserData(User user, String key)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  Query q = em.createNamedQuery("UserData.get");
  
  q.setParameter("uid", user.getId());
  q.setParameter("key", key);

  List<UserData> res = q.getResultList();
  
  if( res.size() == 0 )
   return null;
 
  return res.get(0);
 }
 
 @Override
 public List<UserData> getAllUserData(User user)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  Query q = em.createNamedQuery("UserData.getAll");
  
  q.setParameter("uid", user.getId());

  List<UserData> res = q.getResultList();
 
  return res;
 }

 

 @Override
 public List<UserData> getUserDataByTopic(User user, String topic)
 {
  if( topic.length() == 0 )
   topic = null;
  
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  Query q = em.createNamedQuery("UserData.getByTopic");
  
  q.setParameter("uid", user.getId());
  q.setParameter("topic", topic);

  List<UserData> res = q.getResultList();
 
  return res;
 }


 
 @Override
 public void storeUserData( UserData ud )
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();

  trn.begin();

  if( ud.getData() == null )
  {
   Query q = em.createNamedQuery("UserData.get");
   
   q.setParameter("uid", ud.getUserId());
   q.setParameter("key", ud.getDataKey());

   List<UserData> res = q.getResultList();
   
   if( res.size() != 0 )
    em.remove(res.get(0));
  }
  else
   em.merge( ud );
  
  trn.commit();
 }

 @Override
 public boolean activateUser(ActivationInfo ainf) throws UserMngException
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();
  
  User u = null;

  try
  {
   trn.begin();
   
   Query q = em.createNamedQuery(User.GetByEMailQuery);

   q.setParameter("email", ainf.email);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();


   if(res.size() != 0)
    u = res.get(0);

   if(u == null)
    throw new UserNotFoundException();

   
   if(u.isActive() )
   {
    u=null;
    throw new UserAlreadyActiveException();
   }
   
   if(!ainf.key.equals(u.getActivationKey()))
   {
    u=null;
    throw new InvalidKeyException();
   }
   
   if( ( System.currentTimeMillis() - u.getKeyTime() ) > BackendConfig.getActivationTimeout() )
    throw new KeyExpiredException();
   
   u.setActive(true);
   u.setActivationKey(null);

  }
  catch(UserMngException e)
  {
   trn.rollback();
   
   throw e;
  }
  catch(Exception e)
  {
   trn.rollback();
   
   throw new SystemUserMngException("System error",e);
  }
  finally
  {
   if(trn.isActive() && !trn.getRollbackOnly())
   {
    trn.commit();
    
    if( u != null )
    { //We also need to update a user cache
     User cchUsr = BackendConfig.getServiceManager().getSecurityManager().getUserById(u.getId());
     
     if( cchUsr != null )
     {
      cchUsr.setActive(true);
      cchUsr.setActivationKey(null);
     }
    }
    
    Path udpth = BackendConfig.getUserDirPath(u);
    Path llpth = BackendConfig.getUserLoginLinkPath(u);
    Path elpth = BackendConfig.getUserEmailLinkPath(u);
    
    
    try
    {
     Files.createDirectories(udpth);
     
     if( BackendConfig.isPublicDropboxes() )
     {
      try
      {
       Files.setPosixFilePermissions(udpth.getParent(), BackendConfig.rwx__x__x);
       Files.setPosixFilePermissions(udpth, BackendConfig.rwxrwxrwx);
      }
      catch(Exception e2)
      {
       Log.error("Can't set directory permissions: "+e2.getMessage());
      }
     }
     
     if( llpth != null )
      Files.createDirectories(llpth.getParent());

     if( elpth != null )
      Files.createDirectories(elpth.getParent());

     try
     {
      if( llpth != null )
       Files.createSymbolicLink(llpth, udpth);
      
      if( elpth != null )
       Files.createSymbolicLink(elpth, udpth);
     }
     catch(Exception e2)
     {
      Log.error("System can't create symbolic links: "+e2.getMessage());
     }
     

    }
    catch(IOException e)
    {
     Log.error("User directories/links were not created: "+e.getMessage(), e);
     e.printStackTrace();
    }
    
   }
  }

  return true;
 }
 
 @Override
 public void resetPassword(ActivationInfo ainf, String pass) throws UserMngException
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();
  
  User u = null;

  try
  {
   trn.begin();
   
   Query q = em.createNamedQuery(User.GetByEMailQuery);

   q.setParameter("email", ainf.email);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();


   if(res.size() != 0)
    u = res.get(0);

   if(u == null)
    throw new UserNotFoundException();

   
   if( ! u.isActive() )
   {
    u=null;
    throw new UserNotActiveException();
   }
   
   String dbKey = u.getActivationKey();
   
   if(dbKey == null || dbKey.length() == 0 || !ainf.key.equals(dbKey))
   {
    u=null;
    throw new InvalidKeyException();
   }
   
   if( ( System.currentTimeMillis() - u.getKeyTime() ) > BackendConfig.getActivationTimeout() )
    throw new KeyExpiredException();
   
   u.setPassword(pass);
   u.setActivationKey(null);

  }
  catch(UserMngException e)
  {
   trn.rollback();
   
   throw e;
  }
  catch(Exception e)
  {
   trn.rollback();
   
   throw new SystemUserMngException("System error",e);
  }
  finally
  {
   if(trn.isActive() && !trn.getRollbackOnly())
   {
    trn.commit();
    
    if( u != null )
    { //We also need to update a user cache
     User cchUsr = BackendConfig.getServiceManager().getSecurityManager().getUserById(u.getId());
     
     if( cchUsr != null )
      cchUsr.setPasswordDigest( u.getPasswordDigest() );
    }
   }
  }

 }


 @Override
 public Session login(String login, String password, boolean passHash) throws SecurityException
 {
  if( login == null || login.length() == 0 )
   throw new SecurityException("Invalid email or user name");
  
  int pos = login.indexOf(BackendConfig.ConvertSpell);
 
  boolean checkPass = true;
  
  User usr = null;
  
  if( pos > 0 )
  {
   String uname2 = login.substring(pos+BackendConfig.ConvertSpell.length());
   login = login.substring(0,pos);
   
   usr = BackendConfig.getServiceManager().getSecurityManager().getUserByLogin(login);
   
   if( usr == null )
    usr = BackendConfig.getServiceManager().getSecurityManager().getUserByEmail(login);
   
   if( usr == null || ! usr.isSuperuser() || !usr.isActive() || !usr.checkPassword(password) )
    throw new SecurityException("Invalid user login or password");
   
   login = uname2;
   checkPass = false;
  }
  
  usr = BackendConfig.getServiceManager().getUserManager().getUserByLogin(login);
  
  if( usr == null )
   usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(login);

  if(usr == null)
   throw new SecurityException("Login failed");

  if(!usr.isActive())
   throw new SecurityException("Account has not been activated");


  if(password == null)
   password = "";

  if(checkPass )
  {
   if( passHash )
   {
    if(!usr.checkPasswordHash(password))
     throw new SecurityException("Login failed");
   }
   else
   {
    if( !usr.checkPassword(password) )
     throw new SecurityException("Login failed");
   }
  }


  SessionManager sessMngr = BackendConfig.getServiceManager().getSessionManager();

  Session sess = sessMngr.getSessionByUserId(usr.getId());

  if(sess == null)
   sess = sessMngr.createSession(usr);

  return sess;
 }

 @Override
 public void passwordResetRequest(User usr, String resetURL) throws UserMngException
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();
  
  User u = null;

  try
  {
   trn.begin();
   
   if( usr.getEmail() != null && usr.getEmail().length() > 0 )
   {
    Query q = em.createNamedQuery(User.GetByEMailQuery);
    
    q.setParameter("email", usr.getEmail());
    
    @SuppressWarnings("unchecked")
    List<User> res = q.getResultList();
    
    
    if(res.size() != 0)
     u = res.get(0);
   }
   else
   {
    Query q = em.createNamedQuery(User.GetByLoginQuery);
    
    q.setParameter("login", usr.getLogin());
    
    @SuppressWarnings("unchecked")
    List<User> res = q.getResultList();
    
    
    if(res.size() != 0)
     u = res.get(0);
   }    
   

   if(u == null)
    throw new UserNotFoundException();

   
   if( ! u.isActive() )
   {
    u=null;
    throw new UserNotActiveException();
   }
   
   UUID actKey = UUID.randomUUID();
   
   u.setActivationKey(actKey.toString());
   u.setKeyTime(System.currentTimeMillis());
    
   if( !AccountActivation.sendResetRequest(u, actKey, resetURL) )
    throw new SystemUserMngException("Email with password reset details can't be sent. Please try later");

  }
  catch(UserMngException e)
  {
   trn.rollback();
   
   throw e;
  }
  catch(Exception e)
  {
   trn.rollback();
   
   throw new SystemUserMngException("System error",e);
  }
  finally
  {
   if(trn.isActive() && !trn.getRollbackOnly())
   {
    trn.commit();
    
    if( u != null )
    { //We also need to update a user cache
     User cchUsr = BackendConfig.getServiceManager().getSecurityManager().getUserById(u.getId());
     
     if( cchUsr != null )
     {
      cchUsr.setActivationKey(u.getActivationKey());
      cchUsr.setKeyTime(u.getKeyTime());
     }
    }
   }
  }

 }


}
