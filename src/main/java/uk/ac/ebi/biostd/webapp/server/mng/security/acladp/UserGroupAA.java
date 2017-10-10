package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import uk.ac.ebi.biostd.authz.UserGroup;

public class UserGroupAA extends AbstractAA {

    public UserGroupAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected UserGroup loadObject(String oID) {
        TypedQuery<UserGroup> q = getEM().createQuery("select g from UserGroup g where g.name=:name", UserGroup.class);

        q.setParameter("name", oID);

        List<UserGroup> tags = q.getResultList();

        if (tags.size() == 1) {
            return tags.get(0);
        }

        return null;
    }

}
