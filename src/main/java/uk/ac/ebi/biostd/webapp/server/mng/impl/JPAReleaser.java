package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.ReleaseManager;

public class JPAReleaser implements ReleaseManager
{
 private static Logger log;

 public JPAReleaser()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }
 
 @Override
 public void doHourlyCheck()
 {
  EntityManager em = null;
  
  em = BackendConfig.getEntityManagerFactory().createEntityManager();
  EntityTransaction trn = em.getTransaction();
  
  try
  {
   
   trn.begin();
  
   long now = System.currentTimeMillis()/1000;
   
   Query q = em.createQuery("SELECT s FROM Submission s where s.RTime > 0 AND s.RTime < :end AND s.version > 0 AND s.released = false");
   
   q.setParameter("end", now);
   
   @SuppressWarnings("unchecked")
   List<Submission> res = q.getResultList();
   
   AccessTag pubTag = null;
   
   subs: for( Submission s : res )
   {
    if( s.getAccessTags() != null )
    {
     for( AccessTag t : s.getAccessTags() )
      if( BackendConfig.PublicTag.equals( t.getName() ) )
       continue subs;
    }
    
    if( pubTag == null )
    {
     Query tq = em.createNamedQuery("AccessTag.getByName");
     
     tq.setParameter("name", BackendConfig.PublicTag);
     
     try
     {
      pubTag = (AccessTag)tq.getSingleResult();
     }
     catch( Exception e )
     {
      e.printStackTrace();
      log.error("Can't find 'Public' access tag. Check DB initialization. "+e.getMessage());
      trn.rollback();
      return;
     }
     
    }
    
    s.addAccessTag(pubTag);
    s.setReleased(true);
    
    Path ftpDir = BackendConfig.getPublicFTPPath().resolve(s.getAccNo());
    
    if( Files.exists(ftpDir) )
    {
     try
     {
      Files.setPosixFilePermissions(ftpDir, BackendConfig.rwxrwxr_x);
     }
     catch(UnsupportedOperationException ex)
     {
     }
     catch(IOException e1)
     {
      log.error("Submission dir (" + ftpDir + ") set permissions error. " + e1.getMessage());
      e1.printStackTrace();
     }
    }
   }
   
   trn.commit();
   
  }
  catch(Exception e)
  {
   try
   {
    if( trn.isActive() )
     trn.rollback();
   }
   catch(Exception e2)
   {
    e2.printStackTrace();
    log.error("Can't rollback transaction: "+e2.getMessage());
   }
  }
  finally
  {
   if( em !=null )
    em.close();
  }

 }

 /*
 @Override
 public void doDailyCheck()
 {
  EntityManager em = null;
  
  try
  {
   em = BackendConfig.getEntityManagerFactory().createEntityManager();
   
   em.getTransaction().begin();
  
   long now = System.currentTimeMillis()/1000;
   
   Query q = em.createQuery("SELECT s FROM Submission s WHERE s.RTime > 0 AND s.RTime < :end1 AND s.version > 0 AND s.id NOT IN "
     + "(SELECT ss.id FROM Submission ss JOIN ss.accessTags t WHERE ss.RTime > 0 AND ss.RTime < :end2 AND ss.version > 0 AND t.name=:tagname)");
   
   q.setParameter("end1", now);
   q.setParameter("end2", now);
   q.setParameter("tagname", BackendConfig.PublicTag);
   
   @SuppressWarnings("unchecked")
   List<Submission> res = q.getResultList();
   
   AccessTag pubTag = null;
   
   subs: for( Submission s : res )
   {
    if( s.getAccessTags() != null )
    {
     for( AccessTag t : s.getAccessTags() )
      if( BackendConfig.PublicTag.equals( t.getName() ) )
       continue subs;
    }
    
    if( pubTag == null )
    {
     Query tq = em.createNamedQuery("AccessTag.getByName");
     
     tq.setParameter("name", BackendConfig.PublicTag);
     
     try
     {
      pubTag = (AccessTag)tq.getSingleResult();
     }
     catch( Exception e )
     {
      e.printStackTrace();
      log.error("Can't find 'Public' access tag. Check DB initialization. "+e.getMessage());
      em.getTransaction().rollback();
      return;
     }
     
    }
    
    s.addAccessTag(pubTag);
    s.setRTime(0);
   }
   
   em.getTransaction().commit();
   
  }
  catch(Exception e)
  {
   try
   {
    if( em.getTransaction().isActive() )
     em.getTransaction().rollback();
   }
   catch(Exception e2)
   {
    e2.printStackTrace();
    log.error("Can't rollback transaction: "+e2.getMessage());
   }
  }
  finally
  {
   if( em !=null )
    em.close();
  }
 }
*/
}
