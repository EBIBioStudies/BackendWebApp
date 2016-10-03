package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.List;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserMngException;

public interface UserManager
{

 Session login(String login, String password) throws SecurityException;
 
 User getUserByLogin(String uName);
 User getUserByEmail(String email);

 void addUser(User u, List<String[]> aux,  boolean validateEmail, String actvURL)  throws UserMngException;

 UserData getUserData(User user, String key);

 void storeUserData(UserData ud);
 
 boolean activateUser(ActivationInfo ainf) throws UserMngException;

 void passwordResetRequest(User usr, String resetURL) throws UserMngException;

 void resetPassword(ActivationInfo ainf, String pass) throws UserMngException;

 List<UserData> getAllUserData(User user);

 List<UserData> getUserDataByTopic(User user, String topic);


}
