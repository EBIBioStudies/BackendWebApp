package uk.ac.ebi.biostd.webapp.client;

import uk.ac.ebi.biostd.authz.User;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("adminServiceGWT")
public interface BioStdService extends RemoteService
{
 User getCurrentUser();

 User login(String login, String pass);
}
