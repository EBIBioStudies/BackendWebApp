package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import uk.ac.ebi.biostd.authz.AuthorizationTemplate;

public class AuthzTemplateAA extends AbstractAA {

    public AuthzTemplateAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected AuthorizationTemplate loadObject(String oID) {
        TypedQuery<AuthorizationTemplate> q = getEM()
                .createNamedQuery(AuthorizationTemplate.GetByClassNameQuery, AuthorizationTemplate.class);

        q.setParameter("className", oID);

        List<AuthorizationTemplate> tags = q.getResultList();

        if (tags.size() == 1) {
            return tags.get(0);
        }

        return null;
    }
}
