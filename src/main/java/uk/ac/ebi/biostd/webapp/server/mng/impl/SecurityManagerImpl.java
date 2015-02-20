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

}
