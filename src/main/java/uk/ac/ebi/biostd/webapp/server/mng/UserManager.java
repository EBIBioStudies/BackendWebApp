package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;

public interface UserManager
{

 Session login(String login, String password) throws SecurityException;
 
 User getUserByLogin(String uName);
 User getUserByEmail(String email);

 void addUser(User u, boolean validateEmail, String actvURL) throws ServiceException;

 UserData getUserData(User user, String key);

 void storeUserData(UserData ud);
 
 boolean activateUser(ActivationInfo ainf);

}
