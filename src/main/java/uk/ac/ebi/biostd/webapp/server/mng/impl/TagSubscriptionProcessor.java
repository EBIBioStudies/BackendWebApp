/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute <p> Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at <p> http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>, Andrew Tikhonov andrew.tikhonov@gmail.com
 **/

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.authz.TagSubscriptionMatchEvent;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.model.SubmissionTagRef;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityManager;

public class TagSubscriptionProcessor implements Runnable {

    public static final int IDLE_TIME_SEC = 30;
    public static final String TagPlaceHolderRx = "\\{TAG\\}";
    public static final String SubscriptionPlaceHolderRx = "\\{SUBSCRIPTIONS\\}";
    public static final String ResultsPlaceHolderRx = "\\{RESULTS\\}";
    public static final String SubscriptionStartTag = "{SUBSCRIPTIONS}";
    public static final String SubscriptionEndTag = "{/SUBSCRIPTIONS}";
    public static final String ResultsStartTag = "{RESULTS}";
    public static final String ResultsEndTag = "{/RESULTS}";
    private static boolean isProcessorRunning = true;
    private static Logger log = null;
    private static LinkedBlockingQueue<NotificationRequest> queue;

    private static Logger logger() {
        if (log == null) {
            log = LoggerFactory.getLogger(NotificationRequest.class);
        }

        return log;
    }

    public static void notifyByTags(Collection<SubmissionTagRef> tags, Submission subm) {

        if (BackendConfig.getServiceManager().getEmailService() == null) {
            return;
        }

        Set<Long> tgIds = new HashSet<>();

        for (SubmissionTagRef tr : tags) {
            Tag t = tr.getTag();

            tgIds.add(t.getId());

            Tag pt = t.getParentTag();

            while (pt != null) {
                tgIds.add(pt.getId());

                pt = pt.getParentTag();
            }
        }

        NotificationRequest nreq = new NotificationRequest();
        nreq.tagIds = tgIds;
        nreq.text = subm.getTitle();
        nreq.accNo = subm.getAccNo();
        nreq.sbmId = subm.getId();

        while (true) {
            try {
                getQueue().put(nreq);
                break;
            } catch (InterruptedException e) {
            }
        }

    }

    private static synchronized BlockingQueue<NotificationRequest> getQueue() {
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();

            new Thread(new TagSubscriptionProcessor(), "TagNotifier").start();
        }

        return queue;
    }

    private static synchronized void destroyQueue() {
        if (queue != null) {
            queue.clear();
        }

        queue = null;
    }

    public static EmailTemplates parseMessage(String text) {
        EmailTemplates parts = new EmailTemplates();

        int resultStart = text.indexOf(ResultsStartTag);
        int resultEnd = text.indexOf(ResultsEndTag);

        if (resultStart != -1 && resultEnd != -1) {
            parts.result = text.substring(resultStart +
                    ResultsStartTag.length(), resultEnd);

            text = text.substring(0, resultStart + ResultsStartTag.length()) +
                    text.substring(resultEnd + ResultsEndTag.length());
        }

        int subscriptionStart = text.indexOf(SubscriptionStartTag);
        int subscriptionEnd = text.indexOf(SubscriptionEndTag);

        if (subscriptionStart != -1 && subscriptionEnd != -1) {
            parts.subscription = text.substring(subscriptionStart +
                    SubscriptionStartTag.length(), subscriptionEnd);

            text = text.substring(0, subscriptionStart + SubscriptionStartTag.length()) +
                    text.substring(subscriptionEnd + SubscriptionEndTag.length());
        }

        parts.mainBody = text;

        return parts;
    }

    public static void processEvents() {

        EmailTemplates htmlTemplates;
        EmailTemplates textTemplates;

        try {
            htmlTemplates = parseMessage(BackendConfig.getTagSubscriptionEmailHtmlFile().
                    readToString(Charsets.UTF_8));
        } catch (IOException e1) {
            log.error("Error!", e1);
            return;
        }

        try {
            textTemplates = parseMessage(BackendConfig.getTagSubscriptionEmailPlainTextFile().
                    readToString(Charsets.UTF_8));
        } catch (IOException e1) {
            log.error("Error!", e1);
            return;
        }

        try {

            SecurityManager secMan = BackendConfig.getServiceManager().getSecurityManager();

            EntityManager entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();

            // get all users with events
            //
            TypedQuery<User> userQuery = entityMan.createNamedQuery(
                    TagSubscriptionMatchEvent.GetAllUsersWithEventsQuery, User.class);
            List<User> users = userQuery.getResultList();

            for (User u : users) {

                // check user is activated and has valid email
                //
                if (!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6) {
                    continue;
                }

                HashMap<Long, SubscriptionBatch> subscriptionResultMap = new HashMap<>();

                // get all subscriptions events
                //
                TypedQuery<TagSubscriptionMatchEvent> eventQuery = entityMan.createNamedQuery(
                        TagSubscriptionMatchEvent.GetEventsByUserIdQuery, TagSubscriptionMatchEvent.class);

                eventQuery.setParameter(TagSubscriptionMatchEvent.UserIdQueryParameter, u.getId());

                List<TagSubscriptionMatchEvent> events = eventQuery.getResultList();

                for (TagSubscriptionMatchEvent event : events) {
                    // skip submission if user may not "see" it
                    //
                    if (!secMan.mayUserReadSubmission(event.getSubmission(), u)) {
                        continue;
                    }

                    long id = event.getSubscription().getId();

                    // batch results for each subscription
                    //
                    SubscriptionBatch batchData = subscriptionResultMap.get(id);
                    if (batchData == null) {

                        // init stuffs
                        //
                        batchData = new SubscriptionBatch();

                        String tagName = event.getSubscription().getTag().getName();

                        batchData.htmlSummary = htmlTemplates.subscription;
                        batchData.textSummary = textTemplates.subscription;

                        // Submissions have tag: <i>{TAG}</i>:<br/>
                        batchData.htmlSummary = batchData.htmlSummary.replaceAll(TagPlaceHolderRx,
                                tagName);
                        batchData.textSummary = batchData.textSummary.replaceAll(TagPlaceHolderRx,
                                tagName);

                        batchData.htmlList = new StringBuilder();
                        batchData.textList = new StringBuilder();
                        subscriptionResultMap.put(id, batchData);
                    }

                    // <b>{TITLE}</b> (<a href="https://www.ebi.ac.uk/biostudies/studies/{ACCNO}">https://www.ebi.ac
                    // .uk/biostudies/studies/{ACCNO}</a>)<br/>
                    String accession = event.getSubmission().getAccNo();
                    String title = event.getSubmission().getTitle();

                    String htmlTesultLine = htmlTemplates.result;
                    String textTesultLine = textTemplates.result;

                    htmlTesultLine = htmlTesultLine.replaceAll(BackendConfig.AccNoPlaceHolderRx, accession);
                    htmlTesultLine = htmlTesultLine.replaceAll(BackendConfig.TitlePlaceHolderRx, title);

                    textTesultLine = textTesultLine.replaceAll(BackendConfig.AccNoPlaceHolderRx, accession);
                    textTesultLine = textTesultLine.replaceAll(BackendConfig.TitlePlaceHolderRx, title);

                    batchData.htmlList.append(htmlTesultLine);
                    batchData.textList.append(textTesultLine);
                }

                Collection<SubscriptionBatch> resultSet = subscriptionResultMap.values();

                // check user has got any matches
                //
                if (resultSet.size() == 0) {
                    continue;
                }

                String htmlMessage = htmlTemplates.mainBody;
                String textMessage = textTemplates.mainBody;

                // assemble everything together
                //
                if (u.getFullName() != null) {
                    htmlMessage = htmlMessage.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
                    textMessage = textMessage.replaceAll(BackendConfig.UserNamePlaceHolderRx, u.getFullName());
                }

                String htmlTotalBatch = "";
                String textTotalBatch = "";

                for (SubscriptionBatch batch : subscriptionResultMap.values()) {

                    batch.htmlSummary = batch.htmlSummary.replaceAll(ResultsPlaceHolderRx,
                            batch.htmlList.toString());
                    batch.textSummary = batch.textSummary.replaceAll(ResultsPlaceHolderRx,
                            batch.textList.toString());

                    htmlTotalBatch = htmlTotalBatch + batch.htmlSummary;
                    textTotalBatch = textTotalBatch + batch.textSummary;
                }

                htmlMessage = htmlMessage.replaceAll(SubscriptionPlaceHolderRx, htmlTotalBatch);
                textMessage = textMessage.replaceAll(SubscriptionPlaceHolderRx, textTotalBatch);

                BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(),
                        BackendConfig.getSubscriptionEmailSubject(), textMessage, htmlMessage);

                // remove events
                //
                Query deleteQuery = entityMan.createNamedQuery(
                        TagSubscriptionMatchEvent.DeleteEventsByUserIdQuery);

                deleteQuery.setParameter(TagSubscriptionMatchEvent.UserIdQueryParameter, u.getId());

                EntityTransaction transation = entityMan.getTransaction();

                transation.begin();

                deleteQuery.executeUpdate();

                transation.commit();
            }

        } catch (Exception ex) {
            log.error("Error!", ex);
        }
    }

    @Override
    public void run() {
        NotificationRequest req = null;

        EntityManager entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();

        TypedQuery<Submission> sbmq = entityMan.createNamedQuery(Submission.GetByIdQuery,
                Submission.class);

        while (isProcessorRunning) {
            while (isProcessorRunning) {
                try {
                    req = null;
                    req = queue.poll(IDLE_TIME_SEC, TimeUnit.SECONDS);
                    break;
                } catch (InterruptedException e) {
                }
            }

            if (req == null) {
                destroyQueue();
                break;
            }

            sbmq.setParameter("id", req.sbmId);

            List<Submission> subms = sbmq.getResultList();

            if (subms.size() != 1) {
                logger().warn("TagSubscriptionProcessor: submission not found or multiple results id=" + req.sbmId);
                continue;
            }

            Submission subm = subms.get(0);

            procOneSubmission(entityMan, req, subm);
        }

        entityMan.close();

    }

    private void procOneSubmission(EntityManager entityMan, NotificationRequest req, Submission submission) {

        try {

            SecurityManager securityMan = BackendConfig.getServiceManager().getSecurityManager();

            TypedQuery<TagSubscription> q = entityMan.createNamedQuery(TagSubscription.GetSubsByTagIdsQuery,
                    TagSubscription.class);

            q.setParameter(TagSubscription.TagIdQueryParameter, req.tagIds);

            List<TagSubscription> res = q.getResultList();

            for (TagSubscription subscription : res) {
                User user = subscription.getUser();
                if (!user.isActive() || user.getEmail() == null || user.getEmail().length() < 6 ||
                        !securityMan.mayUserReadSubmission(submission, user)) {
                    continue;
                }

                // create event
                //
                EntityTransaction transaction = entityMan.getTransaction();

                TagSubscriptionMatchEvent event = new TagSubscriptionMatchEvent();
                event.setSubmission(submission);
                event.setSubscription(subscription);
                event.setUser(user);

                transaction.begin();

                entityMan.persist(event);

                transaction.commit();

                // subscription matched
                break;
            }
        } catch (Exception ex) {
            log.error("Error!", ex);
        }

        /*
        new Thread( new Runnable() {
            public void run() {
                processEvents();
            }
        }).start();
        */


        /*
        new Thread( () -> processEvents() ).start();
        new Thread( AttributeSubscriptionProcessor::processEvents ).start();
        */

    }

    static class NotificationRequest {

        Set<Long> tagIds;
        long sbmId;
        String accNo;
        String text;
    }

    public static class EmailTemplates {

        public String mainBody;
        public String subscription;
        public String result;
    }

    public static class SubscriptionBatch {

        public String htmlSummary;
        public String textSummary;
        public StringBuilder htmlList;
        public StringBuilder textList;
    }

}
