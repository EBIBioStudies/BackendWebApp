package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
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

  if( validateEmail )
  {
   u.setActive(false);
   u.setActivationKey(UUID.randomUUID().toString());
   
   if( ! sendActivationRequest(u) )
    throw new ServiceException("Email confirmation request can't be sent. Please try later");
  }
  else
   u.setActive(true);
  
  BackendConfig.getServiceManager().getSecurityManager().addUser(u);
  
 }

 private boolean sendActivationRequest(User u)
 {
  Path txtFile = BackendConfig.getActivationEmailPlainTextFile();

  String textBody = null;
  
  Path htmlFile = BackendConfig.getActivationEmailHtmlFile();
  
  String htmlBody = null;
  
   try
   {
    if( BackendConfig.getActivationEmailPlainTextFile() != null )
     textBody = FileUtil.readFile(BackendConfig.getActivationEmailPlainTextFile().toFile(), Charsets.UTF_8);

    if( BackendConfig.getActivationEmailHtmlFile()!= null )
     htmlBody = FileUtil.readFile(BackendConfig.getActivationEmailHtmlFile().toFile(), Charsets.UTF_8);
   }
   catch(IOException e)
   {
    e.printStackTrace();
    return false;
   }
  
  return BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getLogin(), BackendConfig.getActivationEmailSubject(), textBody, htmlBody);
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

}
