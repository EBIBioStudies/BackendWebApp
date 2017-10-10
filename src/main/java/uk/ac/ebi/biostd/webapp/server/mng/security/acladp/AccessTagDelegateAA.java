package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.Collection;
import javax.persistence.EntityManager;
import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.OwnedObject;
import uk.ac.ebi.biostd.authz.PermGroupACR;
import uk.ac.ebi.biostd.authz.PermUserACR;
import uk.ac.ebi.biostd.authz.Permission;
import uk.ac.ebi.biostd.authz.PermissionProfile;
import uk.ac.ebi.biostd.authz.ProfileGroupACR;
import uk.ac.ebi.biostd.authz.ProfileUserACR;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;

public class AccessTagDelegateAA extends AccessTagAA {

    public AccessTagDelegateAA(EntityManager em, String oId) {
        super(em, oId);
    }


    @Override
    public boolean checkChangeAccessPermission(User user) {
        if ((obj instanceof OwnedObject) && ((OwnedObject) obj).getOwner().equals(user)) {
            return true;
        }

        return obj.checkPermission(SystemAction.CHANGEACCESS, user) == Permit.ALLOW;
    }

    @Override
    public ACR findACR(SystemAction act, boolean pAction, User usr) {
        Collection<? extends PermUserACR> acl = ((AccessTag) obj).getDelegatePermissionForUserACRs();

        if (acl == null) {
            return null;
        }

        for (PermUserACR acr : acl) {
            Permission prm = acr.getPermissionUnit();

            if (prm.isAllow() != pAction || prm.getAction() != act) {
                continue;
            }

            if (usr.getId() == acr.getSubject().getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public ACR findACR(SystemAction act, boolean pAction, UserGroup grp) {
        Collection<? extends PermGroupACR> acl = ((AccessTag) obj).getDelegatePermissionForGroupACRs();

        if (acl == null) {
            return null;
        }

        for (PermGroupACR acr : acl) {
            Permission prm = acr.getPermissionUnit();

            if (prm.isAllow() != pAction || prm.getAction() != act) {
                continue;
            }

            if (grp.getId() == acr.getSubject().getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public ACR findACR(PermissionProfile prof, User usr) {
        Collection<? extends ProfileUserACR> acl = ((AccessTag) obj).getDelegateProfileForUserACRs();

        if (acl == null) {
            return null;
        }

        for (ProfileUserACR acr : acl) {
            if (usr.getId() == acr.getSubject().getId() && acr.getPermissionUnit().getId() == prof.getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public ACR findACR(PermissionProfile prof, UserGroup grp) {
        Collection<? extends ProfileGroupACR> acl = ((AccessTag) obj).getDelegateProfileForGroupACRs();

        if (acl == null) {
            return null;
        }

        for (ProfileGroupACR acr : acl) {
            if (grp.getId() == acr.getSubject().getId() && acr.getPermissionUnit().getId() == prof.getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public void addRule(SystemAction act, boolean pAction, User usr)
            throws uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException {
        ((AccessTag) obj).addDelegatePermissionForUserACR(usr, act, pAction);
    }


    @Override
    public void addRule(SystemAction act, boolean pAction, UserGroup grp) {
        ((AccessTag) obj).addDelegatePermissionForGroupACR(grp, act, pAction);
    }

    @Override
    public void addRule(PermissionProfile prof, User usr) {
        ((AccessTag) obj).addDelegateProfileForUserACR(usr, prof);
    }

    @Override
    public void addRule(PermissionProfile prof, UserGroup grp) {
        ((AccessTag) obj).addDelegateProfileForGroupACR(grp, prof);
    }

    @Override
    public void removeRule(ACR rule) {
        if (rule.getPermissionUnit() instanceof Permission) {
            if (rule.getSubject() instanceof User) {
                ((AccessTag) obj).getDelegatePermissionForUserACRs().remove(rule);
            } else {
                ((AccessTag) obj).getDelegatePermissionForGroupACRs().remove(rule);
            }
        } else {
            if (rule.getSubject() instanceof User) {
                ((AccessTag) obj).getDelegateProfileForUserACRs().remove(rule);
            } else {
                ((AccessTag) obj).getDelegateProfileForGroupACRs().remove(rule);
            }
        }
    }
}
