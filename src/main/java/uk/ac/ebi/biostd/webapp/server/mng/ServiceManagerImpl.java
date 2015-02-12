package uk.ac.ebi.biostd.webapp.server.mng;


public class ServiceManagerImpl implements ServiceManager
{


 private String serviceName;
 
 private ServiceConfig config;
 
 private UserManager userManager;
 private SessionManager sessionManager;
 private SubmissionManager submissionManager;
 private FileManager fileManager;
 private SecurityManager authzManager;
 
 @Override
 public UserManager getUserManager()
 {
  return userManager;
 }
 
 public void setUserManager(UserManager userManager)
 {
  this.userManager = userManager;
 }
 
 @Override
 public SessionManager getSessionManager()
 {
  return sessionManager;
 }
 
 public void setSessionManager(SessionManager sessionManager)
 {
  this.sessionManager = sessionManager;
 }

 @Override
 public String getServiceName()
 {
  return serviceName;
 }

 public void setServiceName(String serviceName)
 {
  this.serviceName = serviceName;
 }


 public void setConfiguration(ServiceConfig cfg)
 {
  config=cfg;
 } 

 public ServiceConfig getConfiguration()
 {
  return config;
 }

 @Override
 public SubmissionManager getSubmissionManager()
 {
  return submissionManager;
 }

 public void setSubmissionManager(SubmissionManager submissionManager)
 {
  this.submissionManager = submissionManager;
 }

 @Override
 public RemoteRequestManager getRemoteRequestManager()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public FileManager getFileManager()
 {
  return fileManager;
 }

 public void setFileManager(FileManager fileManager)
 {
  this.fileManager = fileManager;
 }

 @Override
 public SecurityManager getSecurityManager()
 {
  return authzManager;
 }

 public void setSecurityManager(SecurityManager authzManager)
 {
  this.authzManager = authzManager;
 }
 

}
