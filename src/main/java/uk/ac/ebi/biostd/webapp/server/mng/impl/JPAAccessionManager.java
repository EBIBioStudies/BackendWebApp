/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AuthorizationTemplate;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.idgen.Counter;
import uk.ac.ebi.biostd.idgen.IdGen;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.AccessionManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public class JPAAccessionManager implements AccessionManager {

    public static boolean checkPermissions = false;
    private static Logger log;
    private Map<PfxSfx, IdGen> cache = new HashMap<>();

    public JPAAccessionManager() {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

    }

    @Override
    public synchronized String getNextAccNo(String prefix, String suffix, User usr)
            throws SecurityException, ServiceException {

        StringBuilder sb = new StringBuilder();

        if (prefix != null) {
            sb.append(prefix);
        }

        sb.append(incrementIdGen(prefix, suffix, 1, usr));

        if (suffix != null) {
            sb.append(suffix);
        }

        return sb.toString();

    }

    @Override
    public synchronized long incrementIdGen(String prefix, String suffix, int num, User usr)
            throws SecurityException, ServiceException {
        EntityManager em = null;

        long origMaxCount = -1;
        Counter cnt = null;

        EntityTransaction trn = null;

        try {
            em = BackendConfig.getEntityManagerFactory().createEntityManager();

            PfxSfx cacheKey = new PfxSfx(prefix, suffix);

//   IdGen gen = cache.get( cacheKey );
            IdGen gen = null;

            trn = em.getTransaction();
            trn.begin();

            if (gen == null) {
                Query q = em.createNamedQuery("IdGen.getByPfxSfx");

                q.setParameter("prefix", prefix);
                q.setParameter("suffix", suffix);

                @SuppressWarnings("unchecked")
                List<IdGen> genList = q.getResultList();

                if (genList.size() == 1) {
                    gen = genList.get(0);
                } else if (genList.size() > 1) {
                    log.error("Query returned multiple (" + genList.size() + ") IdGen objects");
                }

            }

            if (gen == null) {
                if (checkPermissions && !BackendConfig.getServiceManager().getSecurityManager()
                        .mayUserCreateIdGenerator(usr)) {
                    throw new SecurityException("User has no right to create ID generator " + prefix + "000" + suffix);
                }

                gen = new IdGen();

                gen.setPrefix(prefix);
                gen.setSuffix(suffix);

                TypedQuery<AuthorizationTemplate> gtq = em
                        .createNamedQuery("AuthorizationTemplate.getByClassName", AuthorizationTemplate.class);

                gtq.setParameter("className", IdGen.class.getName());

                List<AuthorizationTemplate> tpls = gtq.getResultList();

                if (tpls.size() == 1) {
                    BackendConfig.getServiceManager().getSecurityManager().applyTemplate(gen, tpls.get(0));
                }

                em.persist(gen);
            }

            cache.put(cacheKey, gen);

            cnt = gen.getCounter();

            if (cnt == null) {
                cnt = new Counter();
                cnt.setMaxCount(0);
                cnt.setName("" + prefix + "000" + suffix);

                TypedQuery<AuthorizationTemplate> ctq = em
                        .createNamedQuery("AuthorizationTemplate.getByClassName", AuthorizationTemplate.class);
                ctq.setParameter("className", Counter.class.getName());

                List<AuthorizationTemplate> tpls = ctq.getResultList();

                if (tpls.size() == 1) {
                    BackendConfig.getServiceManager().getSecurityManager().applyTemplate(cnt, tpls.get(0));
                }

                gen.setCounter(cnt);

                em.persist(cnt);
            }

            origMaxCount = cnt.getMaxCount();

            if (checkPermissions && gen.checkPermission(SystemAction.INCREMENT, usr) != Permit.ALLOW) {
                throw new SecurityException("User has no right to change ID generator " + prefix + "000" + suffix);
            }

            long firstNum = cnt.incrementByNum(num);

//   em.merge(cnt);
//   em.merge(gen);

            trn.commit();

            em.detach(gen);
            em.detach(cnt);
            em.close();

            return firstNum;
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            if (em != null && em.isOpen()) {
                if (trn.isActive()) {
                    trn.rollback();
                }

                em.close();
            }

            if (cnt != null && origMaxCount >= 0) {
                cnt.setMaxCount(origMaxCount);
            }

            throw new ServiceException("Exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
        }

    }

    static class PfxSfx {

        String pfx;
        String sfx;

        public PfxSfx(String p, String s) {
            pfx = p;
            sfx = s;
        }

        @Override
        public boolean equals(Object obj) {
            PfxSfx otherObj = (PfxSfx) obj;

            if (pfx == null) {
                if (otherObj.pfx != null) {
                    return false;
                }
            } else if (!pfx.equals(otherObj.pfx)) {
                return false;
            }

            if (sfx == null) {
                if (otherObj.sfx != null) {
                    return false;
                }
            } else if (!sfx.equals(otherObj.sfx)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int hash = 0;

            if (sfx != null) {
                hash = sfx.hashCode();
            }

            if (sfx != null) {
                hash = hash ^ sfx.hashCode();
            }

            return hash;
        }
    }

}
