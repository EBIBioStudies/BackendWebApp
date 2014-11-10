package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.mng.ServiceManager;
import uk.ac.ebi.biostd.mng.SessionManager;
import uk.ac.ebi.biostd.mng.UserManager;

public class ServiceManagerImpl implements ServiceManager
{
 private String serviceName;
 
 private ServiceConfig config;
 
 private UserManager userManager;
 private SessionManager sessionManager;
 
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
 
 

}
