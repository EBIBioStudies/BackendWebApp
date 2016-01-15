package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.io.Charsets;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.SessionListener;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;


public class JPAUserManager implements UserManager, SessionListener
{

 
 public JPAUserManager()
 {
 }

 @Override
 public User getUserByLogin(String uName)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  try
  {
   em.getTransaction().begin();
   
   Query q = em.createNamedQuery("User.getByLogin");

   q.setParameter("login", uName);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();

   if(res.size() != 0)
    return res.get(0);
  }
  finally
  {
   em.getTransaction().commit();
  }

  return null;

 }


 @Override
 public User getUserByEmail(String prm)
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
 public synchronized void addUser(User u, boolean validateEmail) throws ServiceException
 {
  
  u.setSecret( UUID.randomUUID().toString() );

  UUID actKey = UUID.randomUUID();
  
  if( validateEmail )
  {
   u.setActive(false);
   u.setActivationKey(actKey.toString());
   
   if( ! sendActivationRequest(u,actKey) )
    throw new ServiceException("Email confirmation request can't be sent. Please try later");
  }
  else
   u.setActive(true);
  
  BackendConfig.getServiceManager().getSecurityManager().addUser(u);
  
 }

 private boolean sendActivationRequest(User u, UUID key)
 {
  Path txtFile = BackendConfig.getActivationEmailPlainTextFile();

  String textBody = null;
  
  Path htmlFile = BackendConfig.getActivationEmailHtmlFile();
  
  String htmlBody = null;
  
  String actKey = AccountActivation.createActivationKey(u.getEmail(), key);
  
  try
  {
   if( txtFile != null )
   {
    textBody = FileUtil.readFile(txtFile.toFile(), Charsets.UTF_8);
    
    textBody = textBody.replaceAll(BackendConfig.ActivateKeyPlaceHolderRx, actKey);
    textBody = textBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
   }
   
   if( htmlFile != null )
   {
    htmlBody = FileUtil.readFile(htmlFile.toFile(), Charsets.UTF_8);

    htmlBody = htmlBody.replaceAll(BackendConfig.ActivateKeyPlaceHolderRx, actKey);
    htmlBody = htmlBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
   }
  }
  catch(Exception e)
  {
   e.printStackTrace();
   
   return false;
  }
  
  return BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getActivationEmailSubject(), textBody, htmlBody);
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

  try
  {
   trn.begin();
   
   Query q = em.createNamedQuery("User.getByEMail");

   q.setParameter("email", ainf.email);

   @SuppressWarnings("unchecked")
   List<User> res = q.getResultList();

   User u = null;

   if(res.size() != 0)
    u = res.get(0);

   if(u == null)
    return false;

   if(u.isActive() || !ainf.key.equals(u.getActivationKey()))
    return false;

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
    trn.commit();
  }

  return true;
 }

}
