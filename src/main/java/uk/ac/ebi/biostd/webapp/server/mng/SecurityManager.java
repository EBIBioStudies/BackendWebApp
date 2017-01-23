package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.AuthorizationTemplate;
import uk.ac.ebi.biostd.authz.AuthzObject;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public interface SecurityManager
{
 void init();
 void applyTemplate(AuthzObject gen, AuthorizationTemplate authorizationTemplate);

 void removeExpiredUsers();
 
 void refreshUserCache();

 User checkUserLogin(String login, String pass, boolean passHash) throws SecurityException;
 
 boolean mayUserReadSubmission(Submission sub, User user);

 boolean mayUserCreateSubmission(User usr);

 boolean mayUserUpdateSubmission(Submission oldSbm, User usr);
 boolean mayUserDeleteSubmission(Submission sbm, User usr);
 boolean mayUserAttachToSubmission(Submission s, User usr);

 boolean mayEveryoneReadSubmission(Submission submission);


 boolean mayUserCreateIdGenerator(User usr);

 User addUser(User u) throws ServiceException;
 UserGroup addGroup(UserGroup ug) throws ServiceException;
 boolean addUserToGroup(User usr, UserGroup grp) throws ServiceException;
 boolean removeUserFromGroup(User usr, UserGroup grp) throws ServiceException;

 User getUserById( long id );
 User getUserByLogin( String login );
 User getUserByEmail( String email );
 
 User getAnonymousUser();

 UserGroup getGroup(String name);

 boolean mayUserManageTags(User user);


 boolean mayUserCreateGroup(User usr);

 boolean mayUserReadGroupFiles(User user, UserGroup g);
 boolean mayUserWriteGroupFiles(User user, UserGroup group);
 boolean mayUserChangeGroup(User usr, UserGroup grp);

}
