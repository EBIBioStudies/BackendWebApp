package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.mng.SessionListener;
import uk.ac.ebi.biostd.mng.UserManager;

public class JPAUserManager implements UserManager, SessionListener
{
 private EntityManagerFactory emf;
 private EntityManager em;
 
 public JPAUserManager(EntityManagerFactory emf)
 {
  this.emf=emf;
 }

 @Override
 public User getUserByName(String uName)
 {
  if(em == null)
   em = emf.createEntityManager();

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
  if(em == null)
   em = emf.createEntityManager();

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
  if(em == null)
   em = emf.createEntityManager();
  
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
