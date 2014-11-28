package uk.ac.ebi.biostd.webapp.client;

import uk.ac.ebi.biostd.authz.User;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface BioStdServiceAsync
{

 void getCurrentUser(AsyncCallback<User> callback);

 void login(String login, String pass, AsyncCallback<User> asyncCallback);

}
