package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.AuthzObject;

public class AccessTagAA extends AbstractAA {

    public AccessTagAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected AuthzObject loadObject(String oID) {
        TypedQuery<AccessTag> q = getEM().createNamedQuery(AccessTag.GetByNameQuery, AccessTag.class);

        q.setParameter("name", oID);

        List<AccessTag> tags = q.getResultList();

        if (tags.size() == 1) {
            return tags.get(0);
        }

        return null;
    }

}
