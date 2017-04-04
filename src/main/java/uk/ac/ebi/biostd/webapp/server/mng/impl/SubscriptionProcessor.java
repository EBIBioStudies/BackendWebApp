/**

Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Andrew Tikhonov <andrew@gmail.com>

**/
package uk.ac.ebi.biostd.webapp.server.mng.impl;

import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.*;
import uk.ac.ebi.biostd.model.*;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.List;


/**
 * Created by andrew on 24/03/2017.
 */

public class SubscriptionProcessor implements Runnable {

    private static boolean isProcessorRunning = true;

    public static final int IDLE_TIME_SEC = 30;

    static class ProcessorRequest {
        long sbmId;
        String accNo;
        String text;
    }

    static class AttributeContainer {
        Map<String, String> map;
        Set<String> nameset;
    }

    private static Logger log = null;

    private static LinkedBlockingQueue<ProcessorRequest> queue;

    private static Logger logger() {
        if (log == null)
            log = LoggerFactory.getLogger(SubscriptionProcessor.class);

        return log;
    }

    public static void processAsync(Submission submission) {

        ProcessorRequest request = new ProcessorRequest();
        request.text = submission.getTitle();
        request.accNo = submission.getAccNo();
        request.sbmId = submission.getId();

        while (true) {
            try {
                getQueue().put(request);
                break;
            } catch (InterruptedException e) {
            }
        }

    }

    private static synchronized BlockingQueue<ProcessorRequest> getQueue() {
        if (queue == null) {
            queue = new LinkedBlockingQueue<ProcessorRequest>();

            new Thread(new SubscriptionProcessor(), "SubscriptionProcessor").start();
        }

        return queue;
    }

    private static synchronized void destroyQueue() {
        if (queue != null)
            queue.clear();

        queue = null;
    }

    @Override
    public void run() {
        ProcessorRequest request = null;

        EntityManager entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();
        TypedQuery<Submission> query = entityMan.createNamedQuery(Submission.GetByIdQuery, Submission.class);

        while (isProcessorRunning) {

            while (isProcessorRunning) {
                try {
                    request = null;
                    request = queue.poll(IDLE_TIME_SEC, TimeUnit.SECONDS);
                    break;
                } catch (InterruptedException e) {
                }
            }

            if (request == null) {
                destroyQueue();
                break;
            }

            query.setParameter("id", request.sbmId);
            List<Submission> submissions = query.getResultList();

            if (submissions.size() != 1) {
                logger().warn("SubscriptionNotifier: submission not found or multiple results id=" + request.sbmId);
                continue;
            }

            Submission subm = submissions.get(0);

            processOneSubmission(entityMan, subm);
        }

        entityMan.close();

    }

    private AttributeContainer transformAttributes(List<? extends AbstractAttribute> submissionAttributes,
                                                   List<? extends AbstractAttribute> sectionAttributes) {
        AttributeContainer container = new AttributeContainer();
        container.map = new HashMap<>();
        container.nameset = new HashSet<>();

        for(AbstractAttribute a : submissionAttributes) {
            container.map.put(a.getName(), a.getValue());
            container.nameset.add(a.getName());
        }

        if (sectionAttributes != null) {
            for(AbstractAttribute a : sectionAttributes) {
                container.map.put(a.getName(), a.getValue());
                container.nameset.add(a.getName());
            }
        }

        return container;
    }

    private void processOneSubmission(EntityManager entityMan, Submission submission) {

        AttributeContainer container = transformAttributes(submission.getAttributes(),
                submission.getRootSection() != null ?
                        submission.getRootSection().getAttributes() :  null);

        // get defined subscriptions
        //
        TypedQuery<TextSubscription> subscriptionQuery = entityMan.createNamedQuery(
                TextSubscription.GetAllByAttributeQuery, TextSubscription.class);
        subscriptionQuery.setParameter(TextSubscription.AttributeQueryParameter, container.nameset);
        List<TextSubscription> subscriptionList = subscriptionQuery.getResultList();

        String value0;

        for (TextSubscription subscription : subscriptionList) {

            // check map contains our attribute
            //
            value0 = container.map.get(subscription.getAttribute());
            if (value0 != null) {

                // check value matches the pattern
                //
                if (value0.contains(subscription.getPattern())) {

                    EntityTransaction transaction = entityMan.getTransaction();

                    transaction.begin();

                    SubscriptionMatchEvent event = new SubscriptionMatchEvent();
                    event.setSubmission(submission);
                    event.setSubscription(subscription);
                    event.setUser(subscription.getUser());

                    entityMan.persist(event);

                    transaction.commit();
                }
            }
        }

        /*
        new Thread(new Runnable() {
            public void run() {
                proceccEvents();
            }
        }).start();
        */
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

    public static final String AttributePlaceHolderRx = "\\{ATTRIBUTE\\}";
    public static final String PatternPlaceHolderRx = "\\{PATTERN\\}";
    public static final String SubscriptionPlaceHolderRx = "\\{SUBSCRIPTIONS\\}";
    public static final String ResultsPlaceHolderRx = "\\{RESULTS\\}";

    public static final String SubscriptionStartTag = "{SUBSCRIPTIONS}";
    public static final String SubscriptionEndTag   = "{/SUBSCRIPTIONS}";
    public static final String ResultsStartTag = "{RESULTS}";
    public static final String ResultsEndTag   = "{/RESULTS}";

    public static EmailTemplates parseMessage(String text) {
        EmailTemplates parts = new EmailTemplates();

        int resultStart = text.indexOf(ResultsStartTag);
        int resultEnd   = text.indexOf(ResultsEndTag);

        if (resultStart != -1 && resultEnd != -1) {
            parts.result = text.substring(resultStart +
                    ResultsStartTag.length(), resultEnd);

            text = text.substring(0, resultStart + ResultsStartTag.length()) +
                    text.substring(resultEnd + ResultsEndTag.length());
        }


        int subscriptionStart = text.indexOf(SubscriptionStartTag);
        int subscriptionEnd   = text.indexOf(SubscriptionEndTag);

        if (subscriptionStart != -1 && subscriptionEnd != -1) {
            parts.subscription = text.substring(subscriptionStart +
                    SubscriptionStartTag.length(), subscriptionEnd);

            text = text.substring(0, subscriptionStart + SubscriptionStartTag.length()) +
                    text.substring(subscriptionEnd + SubscriptionEndTag.length());

        }

        parts.mainBody = text;

        return parts;
    }

    public static void proceccEvents() {

        EmailTemplates htmlTemplates;
        EmailTemplates textTemplates;

        try {
            //htmlTemplates = parseMessage(FileUtil.readFile(BackendConfig.getSubscriptionEmailHtmlFile().toFile(),
            htmlTemplates = parseMessage(FileUtil.readFile(
                    new java.io.File("/Users/andrew/project/EBIBioStudies/biostd/misc/textSubscriptionMail.html"),
                    Charsets.UTF_8));
        } catch (IOException e1) {
            log.error("Error!", e1);
            return;
        }

        try {
            //textTemplates = parseMessage(FileUtil.readFile(BackendConfig.getSubscriptionEmailPlainTextFile().toFile(),
            textTemplates = parseMessage(FileUtil.readFile(
                    new java.io.File("/Users/andrew/project/EBIBioStudies/biostd/misc/textSubscriptionMail.txt"),
                    Charsets.UTF_8));
        } catch (IOException e1) {
            log.error("Error!", e1);
            return;
        }

        SecurityManager secMan = BackendConfig.getServiceManager().getSecurityManager();

        EntityManager entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();

        // get all users with events
        //
        TypedQuery<User> userQuery = entityMan.createNamedQuery(
                SubscriptionMatchEvent.GetAllUsersWithEventsQuery, User.class);
        List<User> users = userQuery.getResultList();

        for (User u : users) {

            // check user is activated and has valid email
            //
            if (!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6)
                continue;

            HashMap<Long, SubscriptionBatch> subscriptionResultMap = new HashMap<>();

            // get all subscriptions events
            //
            TypedQuery<SubscriptionMatchEvent> eventQquery = entityMan.createNamedQuery(
                    SubscriptionMatchEvent.GetEventsByUserIdQuery, SubscriptionMatchEvent.class);

            eventQquery.setParameter(SubscriptionMatchEvent.UserIdQueryParameter, u.getId());

            List<SubscriptionMatchEvent> events = eventQquery.getResultList();

            for (SubscriptionMatchEvent event : events) {
                // skip submission if user may not "see" it
                //
                if (!secMan.mayUserReadSubmission(event.getSubmission(), u))
                    continue;

                long id = event.getSubscription().getId();

                // batch results for each subscription
                //
                SubscriptionBatch batchData = subscriptionResultMap.get(id);
                if (batchData == null) {

                    // init stuffs
                    //
                    batchData = new SubscriptionBatch();

                    String attribute = event.getSubscription().getAttribute();
                    String pattern = event.getSubscription().getPattern();

                    batchData.htmlSummary = htmlTemplates.subscription;
                    batchData.textSummary = textTemplates.subscription;

                    try {
                        // <h4>{ATTRIBUTE} matches {PATTERN}:</h4>
                        batchData.htmlSummary = batchData.htmlSummary.replaceAll(AttributePlaceHolderRx,
                                attribute);
                        batchData.textSummary = batchData.textSummary.replaceAll(AttributePlaceHolderRx,
                                attribute);

                        // <h4>{ATTRIBUTE} matches {PATTERN}:</h4>
                        batchData.htmlSummary = batchData.htmlSummary.replaceAll(PatternPlaceHolderRx,
                                pattern);
                        batchData.textSummary = batchData.textSummary.replaceAll(PatternPlaceHolderRx,
                                pattern);
                    } catch (Exception ex) {
                    }

                    batchData.htmlList = new StringBuilder();
                    batchData.textList = new StringBuilder();
                    subscriptionResultMap.put(id, batchData);
                }

                // <b>{TITLE}</b> (<a href="https://www.ebi.ac.uk/biostudies/studies/{ACCNO}">https://www.ebi.ac.uk/biostudies/studies/{ACCNO}</a>)<br/>
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
            if (resultSet.size() == 0)
                continue;

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

        }
    }

    /// ---------------------

    /*
    private void procSingleTagSubmission(EntityManager em, NotificationRequest req, Submission subm, String textBody, String htmlBody) {
        String sbTitle = subm.getTitle();
        String rsType = "";
        String rsTitle = "";

        SubmissionTagRef tr = subm.getTagRefs().iterator().next();

        String tagsName = tr.getTag().getClassifier().getName() + ":" + tr.getTag().getName();

        if (subm.getRootSection() != null) {
            rsType = subm.getRootSection().getType();

            rsTitle = Submission.getNodeTitle(subm.getRootSection());
        }

        SecurityManager smng = BackendConfig.getServiceManager().getSecurityManager();

        TypedQuery<User> q = em.createNamedQuery(TagSubscription.GetUsersByTagIdsQuery, User.class);

        q.setParameter(TagSubscription.TagIdQueryParameter, req.tagIds);

        List<User> res = q.getResultList();

        for (User u : res) {
            if (!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6 || !smng.mayUserReadSubmission(subm, u))
                continue;


            String tBody = textBody;
            String hBody = htmlBody;

            if (u.getFullName() != null) {
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


            BackendConfig.getServiceManager().getEmailService().sendMultipartEmail(u.getEmail(), BackendConfig.getSubscriptionEmailSubject(), tBody, hBody);
        }

    }   */

    /*
    private void procMultyTagSubmission(EntityManager em, NotificationRequest req, Submission subm, String textBody, String htmlBody) {
        String sbTitle = subm.getTitle();
        String rsType = "";
        String rsTitle = "";

        Map<Long, Tag> tmap = new HashMap<Long, Tag>();

        for (Long tgId : req.tagIds) {
            Tag t = em.find(Tag.class, tgId);

            tmap.put(t.getId(), t);
        }


        if (subm.getRootSection() != null) {
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

        class Subst {
            Matcher tagsMtch;
            final StringBuffer bodySb = new StringBuffer();
            final StringBuilder tagsSb = new StringBuilder();

            void subst(User u, Collection<Tag> tags) {
                String tBody = textBody;
                String hBody = htmlBody;

                if (u.getFullName() != null) {
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

                String[] body = new String[]{tBody, hBody};

                for (int i = 0; i < body.length; i++) {
                    bodySb.setLength(0);

                    if (tagsMtch == null)
                        tagsMtch = Pattern.compile(BackendConfig.TagsPlaceHolderRx).matcher("");

                    tagsMtch.reset(body[i]);

                    while (tagsMtch.find()) {
                        String sep = tagsMtch.group(1);

                        if (sep == null)
                            sep = ", ";
                        else
                            sep = sep.substring(1);

                        tagsSb.setLength(0);

                        for (Tag t : tags)
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

        for (Object[] pair : res) {
            if (u == null || ((Long) pair[0]).longValue() != u.getId()) {
                if (u != null)
                    subst.subst(u, tags);

                tags.clear();
                u = em.find(User.class, pair[0]);

                if (!u.isActive() || u.getEmail() == null || u.getEmail().length() < 6 || !smng.mayUserReadSubmission(subm, u))
                    userOk = false;
                else
                    userOk = true;

            } else if (!userOk)
                continue;


            tags.add(tmap.get(pair[1]));

        }

        if (userOk)
            subst.subst(u, tags);

    }  */


}
