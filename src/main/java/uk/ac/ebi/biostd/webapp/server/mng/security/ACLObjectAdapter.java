package uk.ac.ebi.biostd.webapp.server.mng.security;

import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.PermissionProfile;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;

public interface ACLObjectAdapter
{

 boolean checkChangeAccessPermission(User user);

 ACR findACR(SystemAction act, boolean pAction, User usr);
 ACR findACR(SystemAction act, boolean pAction, UserGroup grp);

 ACR findACR(PermissionProfile prof, User usr);
 ACR findACR(PermissionProfile prof, UserGroup grp);

 void addRule(SystemAction act, boolean pAction, User usr);
 void addRule(SystemAction act, boolean pAction, UserGroup grp);

 void addRule(PermissionProfile prof, User usr);
 void addRule(PermissionProfile prof, UserGroup grp);

 void removeRule(ACR rule);

 boolean isObjectOk();

}
