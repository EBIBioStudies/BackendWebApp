package uk.ac.ebi.biostd.webapp.server.mng.security;

import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.AuthzObject;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;

public interface ACLDriver
{

 boolean checkChangeAccessPermission(AuthzObject aObj, User user);

 ACR findACR(SystemAction act, boolean pAction, User usr);

 void addRule(SystemAction act, boolean pAction, User usr);

}
