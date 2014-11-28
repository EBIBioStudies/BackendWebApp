package uk.ac.ebi.biostd.webapp.client;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.ui.admin.RootWidget;
import uk.ac.ebi.biostd.webapp.client.ui.mng.LoginManager;

public class ClientConfig
{
 private static BioStdServiceAsync service;
 private static LoginManager loginManager;
 private static RootWidget rootWidget;
 private static User loggedUser;

 public static void setService(BioStdServiceAsync svc)
 {
  service = svc;
 }

 public static BioStdServiceAsync getService()
 {
  return service;
 }

 public static LoginManager getLoginManager()
 {
  return loginManager;
 }

 public static void setLoginManager(LoginManager loginManager)
 {
  ClientConfig.loginManager = loginManager;
 }

 public static RootWidget getRootWidget()
 {
  return rootWidget;
 }

 public static void setRootWidget(RootWidget rootWidget)
 {
  ClientConfig.rootWidget = rootWidget;
 }

 public static User getLoggedUser()
 {
  return loggedUser;
 }

 public static void setLoggedUser(User loggedUser)
 {
  ClientConfig.loggedUser = loggedUser;
 }

}
