package uk.ac.ebi.biostd.webapp.server.export;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.model.Submission;

public class SbmIDBagManager
{
 private Logger log;

 private long[] submissionIds;
 
 private int offset = 0;
 
 private int blockSize; 
 
 @SuppressWarnings("unchecked")
 public SbmIDBagManager( EntityManagerFactory emf, int blSz, long since )
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
  
  blockSize = blSz;
  
  EntityManager em = emf.createEntityManager();

  Query idSelQuery=null;
  
  if( since > 0 )
  {
   idSelQuery = em.createQuery("select sbm.id from "+Submission.class.getCanonicalName()+ " sbm WHERE sbm.mTime > :upDate");
   idSelQuery.setParameter("upDate", new Date(since));
  }
  else
   idSelQuery = em.createQuery("select id from "+Submission.class.getCanonicalName());
  
  Collection<Long> sids = idSelQuery.getResultList();
  
  submissionIds = new long[ sids.size() ];
  
  int i=0;
  for( Long l : sids )
   submissionIds[i++] = l.longValue();
  
  Arrays.sort(submissionIds);
  
  log.debug("Retrieved {} submission IDs",submissionIds.length);

  em.close();
 }
 
  
 public int getSubmissionCount()
 {
  return submissionIds.length;
 }
 

 public Range getSubmissionRange()
 {
  synchronized(submissionIds)
  {
   if( offset >= submissionIds.length )
    return null;
   
   if( log.isDebugEnabled() )
    log.debug("Requested submissions range. Offset {} out of {} ({}%)", new Object[]{offset,submissionIds.length,offset*100/submissionIds.length});

   Range r = new Range(submissionIds[offset], 0);
   
   offset += blockSize;
   
   if( offset > submissionIds.length )
    offset = submissionIds.length;
   
   r.setMax(submissionIds[offset - 1]);
   
   return r;
  }
 }

 
 public int getChunkSize()
 {
  return blockSize;
 }

}
