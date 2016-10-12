package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.AuthorizationTemplate;
import uk.ac.ebi.biostd.authz.AuthzObject;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public interface SecurityManager
{
 void applyTemplate(AuthzObject gen, AuthorizationTemplate authorizationTemplate);

 boolean mayUserReadSubmission(Submission sub, User user);

 boolean mayUserCreateSubmission(User usr);

 boolean mayUserUpdateSubmission(Submission oldSbm, User usr);
 boolean mayUserDeleteSubmission(Submission sbm, User usr);
 boolean mayUserAttachToSubmission(Submission s, User usr);

 boolean mayEveryoneReadSubmission(Submission submission);

 void init();

 boolean mayUserCreateIdGenerator(User usr);

 User addUser(User u) throws ServiceException;

 User getUserById( long id );
 User getUserByLogin( String login );
 User getUserByEmail( String email );
 
 User getAnonymousUser();

 boolean mayUserManageTags(User user);

 void removeExpiredUsers();

}
