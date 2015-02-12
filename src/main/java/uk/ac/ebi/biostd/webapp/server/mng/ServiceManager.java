package uk.ac.ebi.biostd.webapp.server.mng;




public interface ServiceManager
{
 UserManager getUserManager();

 SessionManager getSessionManager();

 String getServiceName();

 SubmissionManager getSubmissionManager();

 RemoteRequestManager getRemoteRequestManager();

 FileManager getFileManager();
 
 SecurityManager getSecurityManager();

}
