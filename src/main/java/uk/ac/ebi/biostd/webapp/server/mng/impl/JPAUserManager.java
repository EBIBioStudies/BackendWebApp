package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.SessionListener;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;


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
   
   Query q = em.createNamedQuery("User.getByLogin");

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
  return BackendConfig.getServiceManager().getSecurityManager().getuserByEmail(email);
 }

 @Override
 public User getUserByLogin(String login)
 {
  return BackendConfig.getServiceManager().getSecurityManager().getUserByLogin(login);
 }

 
 private User getUserByEmailDB(String prm)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  Query q = em.createNamedQuery("User.getByEMail");

  q.setParameter("email", prm);

  @SuppressWarnings("unchecked")
  List<User> res = q.getResultList();

  if(res.size() != 0)
   return res.get(0);

  return null;

 }
 
 @Override
 public synchronized void addUser(User u, boolean validateEmail, String validateURL) throws ServiceException
 {
  
  u.setSecret( UUID.randomUUID().toString() );

  UUID actKey = UUID.randomUUID();
  
  if( validateEmail )
  {
   u.setActive(false);
   u.setActivationKey(actKey.toString());
   
   if( !AccountActivation.sendActivationRequest(u,actKey,validateURL) )
    throw new ServiceException("Email confirmation request can't be sent. Please try later");
  }
  else
   u.setActive(true);
  
  BackendConfig.getServiceManager().getSecurityManager().addUser(u);
  
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
 public void storeUserData(UserData ud )
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();

  trn.begin();

  em.merge( ud );
  
  trn.commit();
 }

 @Override
 public boolean activateUser(ActivationInfo ainf)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();
  
  User u = null;

  try
  {
   trn.begin();
   
   Query q = em.createNamedQuery("User.getByEMail");

   q.setParameter("email", ainf.email);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();


   if(res.size() != 0)
    u = res.get(0);

   if(u == null)
    return false;

   if(u.isActive() || !ainf.key.equals(u.getActivationKey()))
   {
    u=null;
    return false;
   }
   
   u.setActive(true);
   u.setActivationKey(null);

  }
  catch(Exception e)
  {
   trn.rollback();
  }
  finally
  {
   if(trn.isActive() && !trn.getRollbackOnly())
   {
    trn.commit();
    
    if( u != null )
    {
     User cchUsr = BackendConfig.getServiceManager().getSecurityManager().getUserById(u.getId());
     
     if( cchUsr != null )
     {
      cchUsr.setActive(true);
      cchUsr.setActivationKey(null);
     }
    }
   }
  }

  return true;
 }

 @Override
 public Session login(String login, String password) throws SecurityException
 {
  User usr = null;
  
  if( login == null || login.length() == 0 )
   throw new SecurityException("Invalid email or user name");
  
  usr = BackendConfig.getServiceManager().getUserManager().getUserByLogin(login);
  
  if( usr == null )
   usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(login);

  if(usr == null)
   throw new SecurityException("Login failed");

  if(!usr.isActive())
   throw new SecurityException("Account has not been activated");


  if(password == null)
   password = "";

  if(!usr.checkPassword(password))
   throw new SecurityException("Login failed");

  SessionManager sessMngr = BackendConfig.getServiceManager().getSessionManager();

  Session sess = sessMngr.getSessionByUserId(usr.getId());

  if(sess == null)
   sess = sessMngr.createSession(usr);

  return sess;
 }

}
