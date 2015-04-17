package uk.ac.ebi.biostd.webapp.server.mng.impl;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;

public class SecurityManagerImpl implements SecurityManager
{

 @Override
 public boolean mayUserCreateSubmission(User usr)
 {
  return true;
 }

 @Override
 public boolean mayUserUpdateSubmission(Submission oldSbm, User usr)
 {
  return oldSbm.getOwner().getLogin().equals( usr.getLogin() );
 }

 @Override
 public boolean mayUserDeleteSubmission(Submission sbm, User usr)
 {
  return sbm.getOwner().getLogin().equals( usr.getLogin() ) || usr.isSuperuser();
 }

 @Override
 public boolean mayUserReadSubmission(Submission sub, User user)
 {
  return sub.getOwner().getLogin().equals( user.getLogin() ) || user.isSuperuser();
 }

}
