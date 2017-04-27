package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import javax.persistence.EntityManager;

import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.PermissionProfile;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.mng.security.ACLObjectAdapter;

public class IdGenAA implements ACLObjectAdapter
{

 public IdGenAA(EntityManager em, String oId)
 {
  // TODO Auto-generated constructor stub
 }

 @Override
 public boolean checkChangeAccessPermission(User user)
 {
  // TODO Auto-generated method stub
  return false;
 }

 @Override
 public ACR findACR(SystemAction act, boolean pAction, User usr)
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public ACR findACR(SystemAction act, boolean pAction, UserGroup grp)
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public ACR findACR(PermissionProfile prof, User usr)
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public ACR findACR(PermissionProfile prof, UserGroup grp)
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public void addRule(SystemAction act, boolean pAction, User usr)
 {
  // TODO Auto-generated method stub

 }

 @Override
 public void addRule(SystemAction act, boolean pAction, UserGroup grp)
 {
  // TODO Auto-generated method stub

 }

 @Override
 public void addRule(PermissionProfile prof, User usr)
 {
  // TODO Auto-generated method stub

 }

 @Override
 public void addRule(PermissionProfile prof, UserGroup grp)
 {
  // TODO Auto-generated method stub

 }

 @Override
 public void removeRule(ACR rule)
 {
  // TODO Auto-generated method stub

 }

 @Override
 public boolean isObjectOk()
 {
  // TODO Auto-generated method stub
  return false;
 }

}
