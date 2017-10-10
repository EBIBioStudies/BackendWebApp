package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import javax.persistence.EntityManager;
import uk.ac.ebi.biostd.idgen.Domain;

public class DomainAA extends AbstractAA {

    public DomainAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected Domain loadObject(String oID) {
        long id;
        try {
            id = Long.parseLong(oID);
        } catch (Exception e) {
            return null;
        }

        return getEM().find(Domain.class, id);
    }
}
