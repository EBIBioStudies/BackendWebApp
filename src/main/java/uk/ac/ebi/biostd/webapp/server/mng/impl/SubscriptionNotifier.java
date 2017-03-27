package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.model.SubmissionTagRef;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationManager;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;

public class SubscriptionNotifier implements Runnable
{
 public static final int IDLE_TIME_SEC = 30;

 static class NotificationRequest
 {
  Set<Long> tagIds;
  long      sbmId;
  String    accNo;
  String    text;
 }

 private static Logger                                   log = null;

 private static LinkedBlockingQueue<NotificationRequest> queue;

 private static Logger logger()
 {
  if(log == null)
   log = LoggerFactory.getLogger(NotificationRequest.class);

  return log;
 }

 public static void notifyByTags(Collection<SubmissionTagRef> tags, Submission subm)
 {

  Set<Long> tgIds = new HashSet<Long>();

  for(SubmissionTagRef tr : tags)
  {
   Tag t = tr.getTag();

   tgIds.add(t.getId());

   Tag pt = t.getParentTag();

   while(pt != null)
   {
    tgIds.add(pt.getId());

    pt = pt.getParentTag();
   }
  }

  NotificationRequest nreq = new NotificationRequest();
  nreq.tagIds = tgIds;
  nreq.text = subm.getTitle();
  nreq.accNo = subm.getAccNo();
  nreq.sbmId = subm.getId();

  while(true)
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
  if(queue == null)
  {
   queue = new LinkedBlockingQueue<SubscriptionNotifier.NotificationRequest>();

   new Thread(new SubscriptionNotifier(), "TagNotifier").start();
  }

  return queue;
 }

 private static synchronized void destroyQueue()
 {
  if(queue != null)
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
   if(BackendConfig.getSubscriptionEmailHtmlFile() != null)
    htmlBody = BackendConfig.getSubscriptionEmailHtmlFile().readToString(Charsets.UTF_8);
  }
  catch(IOException e1)
  {
   // TODO Auto-generated catch block
   e1.printStackTrace();

   return;
  }

  try
  {
   if(BackendConfig.getSubscriptionEmailPlainTextFile() != null)
    textBody = BackendConfig.getSubscriptionEmailPlainTextFile().readToString(Charsets.UTF_8);
  }
  catch(IOException e1)
  {
   // TODO Auto-generated catch block
   e1.printStackTrace();
   return;
  }

  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

  TypedQuery<Submission> sbmq = em.createNamedQuery(Submission.GetByIdQuery, Submission.class);

  while(true)
  {

   while(true)
   {
    try
    {
     req = null;
     req = queue.poll(IDLE_TIME_SEC, TimeUnit.SECONDS);
     break;
    }
    catch(InterruptedException e)
    {
    }
   }

   if(req == null)
   {
    destroyQueue();
    break;
   }

   sbmq.setParameter("id", req.sbmId);

   List<Submission> subms = sbmq.getResultList();

   if(subms.size() != 1)
   {
    logger().warn("SubscriptionNotifier: submission not found or multiple results id=" + req.sbmId);
    continue;
   }

   Submission subm = subms.get(0);

   if(req.tagIds.size() == 1)
    procSingleTagSubmission(em, req, subm, textBody, htmlBody);
   else if(req.tagIds.size() > 1)
    procMultyTagSubmission(em, req, subm, textBody, htmlBody);
  }

  em.close();

 }

 private void procSingleTagSubmission(EntityManager em, NotificationRequest req, Submission subm, String textBody, String htmlBody)
 {
  String sbTitle = subm.getTitle();
  String rsType = "";
  String rsTitle = "";

  SubmissionTagRef tr = subm.getTagRefs().iterator().next();

  String tagsName = tr.getTag().getClassifier().getName() + ":" + tr.getTag().getName();

  if(subm.getRootSection() != null)
  {
   rsType = subm.getRootSection().getType();

   rsTitle = Submission.getNodeTitle(subm.getRootSection());
  }

  SecurityManager smng = BackendConfig.getServiceManager().getSecurityManager();

  TypedQuery<User> q = em.createNamedQuery(TagSubscription.GetUsersByTagIdsQuery, User.class);

  q.setParameter(TagSubscription.TagIdQueryParameter, req.tagIds);

  List<User> res = q.getResultList();

  for(User u : res)
  {
   if(!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6 || !smng.mayUserReadSubmission(subm, u))
    continue;

   String tBody = textBody;
   String hBody = htmlBody;

   if(u.getFullName() != null)
   {
    tBody = tBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
    hBody = hBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
   }

   tBody = tBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);
   hBody = hBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);

   tBody = tBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);
   hBody = hBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);

   tBody = tBody.replaceAll(BackendConfig.TitlePlaceHolderRx, rsTitle);
   hBody = hBody.replaceAll(BackendConfig.TitlePlaceHolderRx, rsTitle);

   tBody = tBody.replaceAll(BackendConfig.TypePlaceHolderRx, rsType);
   hBody = hBody.replaceAll(BackendConfig.TypePlaceHolderRx, rsType);

   tBody = tBody.replaceAll(BackendConfig.SbmTitlePlaceHolderRx, sbTitle);
   hBody = hBody.replaceAll(BackendConfig.SbmTitlePlaceHolderRx, sbTitle);

   tBody = tBody.replaceAll(BackendConfig.TagsPlaceHolderRx, tagsName);
   hBody = hBody.replaceAll(BackendConfig.TagsPlaceHolderRx, tagsName);

   String from = BackendConfig.getEmailConfig().get(ConfigurationManager.EmailInquiresParameter).toString();

   if(from == null)
    from = BackendConfig.getEmailConfig().get("from").toString();

   if(from == null)
    from = "";

   tBody = tBody.replaceAll(BackendConfig.MailToPlaceHolderRx, from);
   hBody = hBody.replaceAll(BackendConfig.MailToPlaceHolderRx, from);

   String uiURL = BackendConfig.getUIURL();
   
   if(uiURL == null)
    uiURL = "";

   tBody = tBody.replaceAll(BackendConfig.UIURLPlaceHolderRx, uiURL);
   hBody = hBody.replaceAll(BackendConfig.UIURLPlaceHolderRx, uiURL);

   BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getSubscriptionEmailSubject(), tBody, hBody);
  }

 }

 private void procMultyTagSubmission(EntityManager em, NotificationRequest req, Submission subm, String textBody, String htmlBody)
 {
  String sbTitle = subm.getTitle();
  String rsType = "";
  String rsTitle = "";

  Map<Long, Tag> tmap = new HashMap<Long, Tag>();

  for(Long tgId : req.tagIds)
  {
   Tag t = em.find(Tag.class, tgId);

   tmap.put(t.getId(), t);
  }

  if(subm.getRootSection() != null)
  {
   rsType = subm.getRootSection().getType();
   rsTitle = Submission.getNodeTitle(subm.getRootSection());
  }

  SecurityManager smng = BackendConfig.getServiceManager().getSecurityManager();

  TypedQuery<Object[]> q = em.createNamedQuery(TagSubscription.GetSubsByTagIdsQuery, Object[].class);

  q.setParameter(TagSubscription.TagIdQueryParameter, req.tagIds);

  List<Object[]> res = q.getResultList();

  User u = null;
  boolean userOk = false;

  Collection<Tag> tags = new HashSet<Tag>();

  final String secTitle = rsTitle;
  final String secType = rsType;

  class Subst
  {
   Matcher             tagsMtch;
   final StringBuffer  bodySb = new StringBuffer();
   final StringBuilder tagsSb = new StringBuilder();

   void subst(User u, Collection<Tag> tags)
   {
    String tBody = textBody;
    String hBody = htmlBody;

    if(u.getFullName() != null)
    {
     tBody = tBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
     hBody = hBody.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
    }

    tBody = tBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);
    hBody = hBody.replaceAll(BackendConfig.AccNoPlaceHolderRx, req.accNo);

    tBody = tBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);
    hBody = hBody.replaceAll(BackendConfig.TextPlaceHolderRx, req.text);

    tBody = tBody.replaceAll(BackendConfig.TitlePlaceHolderRx, secTitle);
    hBody = hBody.replaceAll(BackendConfig.TitlePlaceHolderRx, secTitle);

    tBody = tBody.replaceAll(BackendConfig.TypePlaceHolderRx, secType);
    hBody = hBody.replaceAll(BackendConfig.TypePlaceHolderRx, secType);

    tBody = tBody.replaceAll(BackendConfig.SbmTitlePlaceHolderRx, sbTitle);
    hBody = hBody.replaceAll(BackendConfig.SbmTitlePlaceHolderRx, sbTitle);

    
    String from = BackendConfig.getEmailConfig().get(ConfigurationManager.EmailInquiresParameter).toString();

    if(from == null)
     from = BackendConfig.getEmailConfig().get("from").toString();

    if(from == null)
     from = "";

    tBody = tBody.replaceAll(BackendConfig.MailToPlaceHolderRx, from);
    hBody = hBody.replaceAll(BackendConfig.MailToPlaceHolderRx, from);

    
    String uiURL = BackendConfig.getUIURL();
    
    if(uiURL == null)
     uiURL = "";

    tBody = tBody.replaceAll(BackendConfig.UIURLPlaceHolderRx, uiURL);
    hBody = hBody.replaceAll(BackendConfig.UIURLPlaceHolderRx, uiURL);

    
    String[] body = new String[] { tBody, hBody };

    for(int i = 0; i < body.length; i++)
    {
     bodySb.setLength(0);

     if(tagsMtch == null)
      tagsMtch = Pattern.compile(BackendConfig.TagsPlaceHolderRx).matcher("");

     tagsMtch.reset(body[i]);

     while(tagsMtch.find())
     {
      String sep = tagsMtch.group(1);

      if(sep == null)
       sep = ", ";
      else
       sep = sep.substring(1);

      tagsSb.setLength(0);

      for(Tag t : tags)
       tagsSb.append(t.getClassifier().getName()).append(':').append(t.getName()).append(sep);

      tagsSb.setLength(tagsSb.length() - sep.length());

      tagsMtch.appendReplacement(bodySb, tagsSb.toString());
     }

     tagsMtch.appendTail(bodySb);

     body[i] = bodySb.toString();

    }

    BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getSubscriptionEmailSubject(), body[0], body[1]);
   }
  }

  Subst subst = new Subst();

  for(Object[] pair : res)
  {
   if(u == null || ((Long) pair[0]).longValue() != u.getId())
   {
    if(u != null)
     subst.subst(u, tags);

    tags.clear();
    u = em.find(User.class, pair[0]);

    if(!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6 || !smng.mayUserReadSubmission(subm, u))
     userOk = false;
    else
     userOk = true;

   }
   else if(!userOk)
    continue;

   tags.add(tmap.get(pair[1]));

  }

  if(userOk)
   subst.subst(u, tags);

 }

}
