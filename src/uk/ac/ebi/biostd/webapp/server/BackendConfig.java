package uk.ac.ebi.biostd.webapp.server;

import uk.ac.ebi.biostd.webapp.Constants;
import uk.ac.ebi.biostd.webapp.server.mng.RemoteRequestManager;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;


public class BackendConfig
{
 private static BackendConfig defaultInstace = new BackendConfig();
 
 
 public static BackendConfig getDefaultConfiguration()
 {
  return defaultInstace;
 }


 private SessionManager sessionPool;
 private RemoteRequestManager remoteReqManager;
 
 public SessionManager getSessionManager()
 {
  return sessionPool;
 }
 
 public void setSessionManager(SessionManager sessionPool)
 {
  this.sessionPool = sessionPool;
 }
 
 public String getSessionCookieName()
 {
  return Constants.sessionKey;
 }
 
 public RemoteRequestManager getRemoteRequestManager()
 {
  return remoteReqManager;
 }

 public void setRemoteRequestManager(RemoteRequestManager uploadManager)
 {
  this.remoteReqManager = uploadManager;
 }
}
