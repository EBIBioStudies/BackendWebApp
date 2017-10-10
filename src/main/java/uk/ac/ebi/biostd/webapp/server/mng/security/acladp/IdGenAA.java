package uk.ac.ebi.biostd.webapp.server.mng.security.acladp;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import uk.ac.ebi.biostd.idgen.IdGen;

public class IdGenAA extends AbstractAA {

    public IdGenAA(EntityManager em, String oId) {
        super(em, oId);
    }

    @Override
    protected IdGen loadObject(String oID) {
        TypedQuery<IdGen> q = getEM().createNamedQuery(IdGen.GetByPfxSfxQuery, IdGen.class);

        String pfx = oID;
        String sfx = null;

        int pos = oID.indexOf(",");

        if (pos >= 0) {
            pfx = pos == 0 ? null : oID.substring(0, pos);
            sfx = pos == oID.length() - 1 ? null : oID.substring(pos + 1);
        }

        q.setParameter("prefix", pfx);
        q.setParameter("suffix", sfx);

        List<IdGen> tags = q.getResultList();

        if (tags.size() == 1) {
            return tags.get(0);
        }

        return null;

    }

}
