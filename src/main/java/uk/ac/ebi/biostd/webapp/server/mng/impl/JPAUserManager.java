package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.User;
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

  Query q = em.createQuery("select u from User u where login=?1");

  q.setParameter(1, uName);

  @SuppressWarnings("unchecked")
  List<User> res = q.getResultList();

  if(res.size() != 0)
   return res.get(0);

  return null;

 }

 @Override
 public User getUserByEmail(String prm)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  Query q = em.createQuery("select u from User u where email=?");

  q.setParameter(1, prm);

  @SuppressWarnings("unchecked")
  List<User> res = q.getResultList();

  if(res.size() != 0)
   return res.get(0);

  return null;

 }

 @Override
 public void addUser(User u)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
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

}
