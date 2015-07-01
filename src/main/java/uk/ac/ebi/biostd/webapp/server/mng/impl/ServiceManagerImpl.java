package uk.ac.ebi.biostd.webapp.server.mng.impl;

import uk.ac.ebi.biostd.webapp.server.mng.FileManager;
import uk.ac.ebi.biostd.webapp.server.mng.ReleaseManager;
import uk.ac.ebi.biostd.webapp.server.mng.RemoteRequestManager;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfig;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;


public class ServiceManagerImpl implements ServiceManager
{

 private String serviceName;
 
 private ServiceConfig config;
 
 private UserManager userManager;
 private SessionManager sessionManager;
 private SubmissionManager submissionManager;
 private FileManager fileManager;
 private SecurityManager authzManager;
 private ReleaseManager releaser;
 
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

 @Override
 public ReleaseManager getReleaseManager()
 {
  return releaser;
 }

 public void setReleaseManager(ReleaseManager releaser)
 {
  this.releaser = releaser;
 }
 

}
