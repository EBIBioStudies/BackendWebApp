package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.ACR;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AuthzObject;
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
import uk.ac.ebi.biostd.webapp.server.mng.security.ACLObjectAdapter;

public abstract class AbstractAA implements ACLObjectAdapter {

    private static Logger log;

    protected AuthzObject obj;
    private EntityManager em;

    public AbstractAA(EntityManager em, String oId) {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        this.em = em;

        obj = loadObject(oId);
    }

    protected abstract AuthzObject loadObject(String oID);

    protected EntityManager getEM() {
        return em;
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
        if (obj.getPermissionForUserACRs() == null) {
            return null;
        }

        for (PermUserACR acr : obj.getPermissionForUserACRs()) {
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
        if (obj.getPermissionForGroupACRs() == null) {
            return null;
        }

        for (PermGroupACR acr : obj.getPermissionForGroupACRs()) {
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
        if (obj.getProfileForUserACRs() == null) {
            return null;
        }

        for (ProfileUserACR acr : obj.getProfileForUserACRs()) {
            if (usr.getId() == acr.getSubject().getId() && acr.getPermissionUnit().getId() == prof.getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public ACR findACR(PermissionProfile prof, UserGroup grp) {
        if (obj.getProfileForGroupACRs() == null) {
            return null;
        }

        for (ProfileGroupACR acr : obj.getProfileForGroupACRs()) {
            if (grp.getId() == acr.getSubject().getId() && acr.getPermissionUnit().getId() == prof.getId()) {
                return acr;
            }
        }

        return null;
    }

    @Override
    public void addRule(SystemAction act, boolean pAction, User usr)
            throws uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException {
        obj.addPermissionForUserACR(usr, act, pAction);
    }


    @Override
    public void addRule(SystemAction act, boolean pAction, UserGroup grp) {
        obj.addPermissionForGroupACR(grp, act, pAction);
    }

    @Override
    public void addRule(PermissionProfile prof, User usr) {
        obj.addProfileForUserACR(usr, prof);
    }

    @Override
    public void addRule(PermissionProfile prof, UserGroup grp) {
        obj.addProfileForGroupACR(grp, prof);
    }

    @Override
    public void removeRule(ACR rule) {
        if (rule.getPermissionUnit() instanceof Permission) {
            if (rule.getSubject() instanceof User) {
                obj.getPermissionForUserACRs().remove(rule);
            } else {
                obj.getPermissionForGroupACRs().remove(rule);
            }
        } else {
            if (rule.getSubject() instanceof User) {
                obj.getProfileForUserACRs().remove(rule);
            } else {
                obj.getProfileForGroupACRs().remove(rule);
            }
        }
    }

    @Override
    public boolean isObjectOk() {
        return obj != null;
    }

}
