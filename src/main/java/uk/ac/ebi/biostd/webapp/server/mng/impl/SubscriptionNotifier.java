package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.io.Charsets;

import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.model.SubmissionTagRef;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class SubscriptionNotifier implements Runnable
{
 public static final int IDLE_TIME_SEC=30;
 
 static class NotificationRequest
 {
  Set<Long> tagIds;
  String accNo;
  String text;
 }
 
 private static LinkedBlockingQueue<NotificationRequest> queue;
 
 public static void notifyByTags(Collection<SubmissionTagRef> tags, Submission subm )
 {
  
  Set<Long> tgIds = new HashSet<Long>();
  
  for( SubmissionTagRef tr : tags )
  {
   Tag t = tr.getTag();
   
   tgIds.add(t.getId());
   
   Tag pt = t.getParentTag();
   
   while( pt != null )
   {
    tgIds.add(pt.getId());
    
    pt = pt.getParentTag();
   } 
  }
  
  NotificationRequest nreq = new NotificationRequest();
  nreq.tagIds = tgIds;
  nreq.text = subm.getTitle();
  nreq.accNo=subm.getAccNo();
  
  while( true )
  {
   try
   {
    getQueue().put(nreq);
    break;
   }
   catch(InterruptedException e)
   {
   }
  }

 }
 
 private static synchronized BlockingQueue<NotificationRequest> getQueue()
 {
  if( queue == null )
  {
   queue  = new LinkedBlockingQueue<SubscriptionNotifier.NotificationRequest>();
   
   new Thread( new SubscriptionNotifier(), "TagNotifier" ).start();
  }
  
  return queue;
 }
 
 private static synchronized void destroyQueue()
 {
  if( queue != null )
   queue.clear();
  
  queue = null;
 }

 @Override
 public void run()
 {
  NotificationRequest req = null;
  
  String htmlBody = null;
  String textBody = null;
  
  try
  {
   htmlBody = FileUtil.readFile(BackendConfig.getSubscriptionEmailHtmlFile().toFile(), Charsets.UTF_8);
  }
  catch(IOException e1)
  {
   // TODO Auto-generated catch block
   e1.printStackTrace();
   
   return;
  }

  try
  {
   textBody = FileUtil.readFile(BackendConfig.getSubscriptionEmailPlainTextFile().toFile(), Charsets.UTF_8);
  }
  catch(IOException e1)
  {
   // TODO Auto-generated catch block
   e1.printStackTrace();
   return;
  }

  

  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  Query q = em.createNamedQuery(TagSubscription.GetUsersByTagIdsQuery);
  
  while( true )
  {

   while( true )
   {
    try
    {
     req = queue.poll(IDLE_TIME_SEC, TimeUnit.SECONDS);
     break;
    }
    catch(InterruptedException e)
    {
    }
   }
   
   if( req == null )
   {
    destroyQueue();
    break;
   }
   
   q.setParameter(TagSubscription.TagIdQueryParameter, req.tagIds);
   
   List<User> res = q.getResultList();
   
   for( User u : res )
   {
    if( ! u.isActive() || u.getEmail() == null || u.getEmail().length() < 6 )
     continue;
    
    String tBody = textBody;
    String hBody = htmlBody;
    
    if(u.getFullName() != null )
    {
     tBody = tBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
     hBody = hBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
    }
    
    tBody = tBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);
    hBody = hBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);

    tBody = tBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);
    hBody = hBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);

    BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getSubscriptionEmailSubject(), tBody, hBody);
    
   }
    
   
  }
  
  em.close();
  
 }
}
