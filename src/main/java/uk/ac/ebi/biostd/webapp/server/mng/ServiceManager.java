package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.webapp.server.email.EmailService;





public interface ServiceManager
{
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

 void shutdown();
}
