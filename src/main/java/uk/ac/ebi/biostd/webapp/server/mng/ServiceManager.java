/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.webapp.server.email.EmailService;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityManager;


public interface ServiceManager {

    UserManager getUserManager();

    SessionManager getSessionManager();

    String getServiceName();

    SubmissionManager getSubmissionManager();

    RemoteRequestManager getRemoteRequestManager();

    FileManager getFileManager();

    SecurityManager getSecurityManager();

    ReleaseManager getReleaseManager();

    AccessionManager getAccessionManager();

    EmailService getEmailService();

    void setEmailService(EmailService emailService);

    TagManager getTagManager();

    SubscriptionManager getSubscriptionManager();

    void setSubscriptionManager(SubscriptionManager subscriptionManager);

    void shutdown();
}
