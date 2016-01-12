package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;

public interface UserManager
{

 User getUserByLogin(String uName);
 User getUserByEmail(String email);

 void addUser(User u, boolean validateEmail) throws ServiceException;

 UserData getUserData(User user, String key);

 void storeUserData(UserData ud);


}
