package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;

public interface SecurityManager
{
 boolean mayUserReadSubmission(Submission sub, User user);

 boolean mayUserCreateSubmission(User usr);

 boolean mayUserUpdateSubmission(Submission oldSbm, User usr);
 boolean mayUserDeleteSubmission(Submission sbm, User usr);

 boolean mayEveryoneReadSubmission(Submission submission);


}
