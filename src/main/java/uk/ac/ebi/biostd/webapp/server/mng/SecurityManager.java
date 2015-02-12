package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;

public interface SecurityManager
{

 boolean mayUserCreateSubmission(User usr);

 boolean mayUserUpdateSubmission(Submission oldSbm, User usr);

}
