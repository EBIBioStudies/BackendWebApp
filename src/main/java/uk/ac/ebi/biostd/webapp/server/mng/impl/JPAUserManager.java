package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.DBInitializer;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SessionListener;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;


public class JPAUserManager implements UserManager, SessionListener
{
 
 public JPAUserManager()
 {
 }

 @Override
 public User getUserByName(String uName)
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
 public synchronized void addUser(User u)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  
  Query q = em.createNamedQuery("User.getCount");

  if( (Long)q.getSingleResult() == 0)
  {
   DBInitializer.init();
   u.setSuperuser(true);
   BackendConfig.getServiceManager().getSecurityManager().init();
  }
  
  u.setSecret( UUID.randomUUID().toString() );
  
  trn.begin();

  em.persist( u );
  
  trn.commit();
  
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
