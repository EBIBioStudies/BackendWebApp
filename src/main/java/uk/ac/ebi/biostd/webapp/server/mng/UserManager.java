package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;

public interface UserManager
{

 User getUserByName(String uName);

 User getUserByEmail(String prm);

 void addUser(User u);

 UserData getUserData(User user, String key);

 void storeUserData(UserData ud);

}
