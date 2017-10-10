package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import uk.ac.ebi.biostd.idgen.Counter;

public class CounterAA extends AbstractAA {

    public CounterAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected Counter loadObject(String oID) {
        TypedQuery<Counter> q = getEM().createNamedQuery(Counter.GetByNameQuery, Counter.class);

        q.setParameter("name", oID);

        List<Counter> tags = q.getResultList();

        if (tags.size() == 1) {
            return tags.get(0);
        }

        return null;
    }
}
