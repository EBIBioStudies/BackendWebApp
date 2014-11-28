package uk.ac.ebi.biostd.webapp.client;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.ui.admin.AdminPanel;
import uk.ac.ebi.biostd.webapp.client.ui.admin.RootWidget;
import uk.ac.ebi.biostd.webapp.client.ui.mng.LoginManager;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class BioStdWebApp implements EntryPoint
{

 @Override
 public void onModuleLoad()
 {
  
  ClientConfig.setService( (BioStdServiceAsync)GWT.create(BioStdService.class) );
  
  AdminPanel apc = new AdminPanel();
  
  RootWidget rw = new RootWidget(apc);
  
  ClientConfig.setRootWidget(rw);
  
  ClientConfig.setLoginManager( new LoginManager()
  {
   
   @Override
   public void userLoggedIn(User u)
   {
    ClientConfig.getRootWidget().showApplication();
    ClientConfig.setLoggedUser( u );
   }
   
   @Override
   public void askForLogin()
   {
    ClientConfig.setLoggedUser( null );
    ClientConfig.getRootWidget().askForLogin();
   }
  });
  
  ClientConfig.getService().getCurrentUser(new AsyncCallback<User>()
  {
   
   @Override
   public void onSuccess(User u)
   {
    if( u == null )
     ClientConfig.getLoginManager().askForLogin();
    else
     ClientConfig.getRootWidget().showApplication();
   }
   
   @Override
   public void onFailure(Throwable arg0)
   {
    ClientConfig.getLoginManager().askForLogin();
   }
  });
  
  rw.draw();

 }
}
