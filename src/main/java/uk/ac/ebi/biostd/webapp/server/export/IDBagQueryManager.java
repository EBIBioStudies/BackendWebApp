package uk.ac.ebi.biostd.webapp.server.export;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.model.Submission;

public class IDBagQueryManager implements QueryManager
{
 private static final int RETRIEVE_ATTEMPTS=3;
 
 private static final boolean useTransaction = false;
 private static Logger log;

 private EntityManagerFactory factory;
 private EntityManager em;
 
 private Query query;

 private int recovers = 0;
 
 private SbmIDBagManager sgidsm;
 
  
 public IDBagQueryManager(EntityManagerFactory emf, SbmIDBagManager slMgr )
 {
  if( log == null )
   log = LoggerFactory.getLogger( getClass() );
  
  factory = emf; 
  
  sgidsm = slMgr;
 }
 
 
 private void createEM()
 {
  if(em != null)
   return;

  em = factory.createEntityManager();

  if(useTransaction)
   em.getTransaction().begin();

  query = em.createQuery("SELECT a FROM " + Submission.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId AND a.version > 0");
 }
 
 
 @Override
 @SuppressWarnings("unchecked")
 public List<Submission> getSubmissions()
 {
  Range r = sgidsm.getSubmissionRange();

  if(r == null)
  {
   log.debug("({}) No more submission ranges", Thread.currentThread().getName());
   return Collections.emptyList();
  }
  else
   log.debug("({}) Processing submission range {}", Thread.currentThread().getName(), r);

  int tries = 0;

  while(true)
  {
   try
   {
    createEM();

    query.setParameter("id", r.getMin());
    query.setParameter("endId", r.getMax());

    List<Submission> res = query.getResultList();

    log.debug("({}) Retrieved submissions: {}", Thread.currentThread().getName(), res.size());

    return res;
   }
   catch(Exception e)
   {
    if(tries >= RETRIEVE_ATTEMPTS)
     throw e;

    tries++;
    recovers++;

    close();
   }
  }

 }
 
 
 @Override
 public void release()
 {
  if( em == null )
   return;
  
  if( useTransaction )
  {
   EntityTransaction trn = em.getTransaction();

   if( trn.isActive() && ! trn.getRollbackOnly() )
   {
    try
    {
     trn.commit();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
  }
  
  em.clear();
 }



 @Override
 public void close()
 {
  if( em == null )
   return;
  
  if( useTransaction )
  {
   EntityTransaction trn = em.getTransaction();

   if( trn.isActive() && ! trn.getRollbackOnly() )
   {
    try
    {
     trn.commit();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
  }
  
  em.close();
  em=null;
  
 }

 @Override
 public int getChunkSize()
 {
  return sgidsm.getChunkSize();
 }


 @Override
 public int getRecovers()
 {
  return recovers;
 }

}
