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

package uk.ac.ebi.biostd.webapp.server.export;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.model.Submission;

public class IDBagQueryManager implements QueryManager {

    private static final int RETRIEVE_ATTEMPTS = 3;

    private static final boolean useTransaction = false;
    private static Logger log;

    private EntityManagerFactory factory;
    private EntityManager em;

    private Query query;

    private int recovers = 0;

    private SbmIDBagManager sgidsm;


    public IDBagQueryManager(EntityManagerFactory emf, SbmIDBagManager slMgr) {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        factory = emf;

        sgidsm = slMgr;
    }


    private void createEM() {
        if (em != null) {
            return;
        }

        em = factory.createEntityManager();

        query = em.createQuery("SELECT a FROM " + Submission.class.getCanonicalName()
                + " a WHERE a.id >=:id and a.id <= :endId AND a.version > 0").setHint("org.hibernate.cacheable", false);
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<Submission> getSubmissions(PrintWriter out) {
        Range r = sgidsm.getSubmissionRange(out);

        if (r == null) {
            log.debug("({}) No more submission ranges", Thread.currentThread().getName());
            return Collections.emptyList();
        } else {
            log.debug("({}) Processing submission range {}", Thread.currentThread().getName(), r);
        }

        int tries = 0;

        out.printf("Got range: %d-%d\n", r.getMin(), r.getMax());

        while (true) {
            try {
                createEM();

                if (useTransaction) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().commit();
                    }

                    em.getTransaction().begin();
                }

                query.setParameter("id", r.getMin());
                query.setParameter("endId", r.getMax());

                List<Submission> res = query.getResultList();

                log.debug("({}) Retrieved submissions: {}", Thread.currentThread().getName(), res.size());

                if (r.getIds() != null && res.size() != r.getIds().length) {
                    for (long id : r.getIds()) {
                        boolean found = false;

                        for (Submission s : res) {
                            if (s.getId() == id) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            log.warn("While export: missing a submission with id=" + id + " Range: " + r);
                        }
                    }
                }

                return res;
            } catch (Exception e) {
                if (tries >= RETRIEVE_ATTEMPTS) {
                    throw e;
                }

                tries++;
                recovers++;

                close();
            }
        }

    }


    @Override
    public void release() {
        if (em == null) {
            return;
        }

        if (useTransaction) {
            EntityTransaction trn = em.getTransaction();

            if (trn.isActive() && !trn.getRollbackOnly()) {
                try {
                    trn.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        em.clear();
    }


    @Override
    public void close() {
        if (em == null) {
            return;
        }

        if (useTransaction) {
            EntityTransaction trn = em.getTransaction();

            if (trn.isActive() && !trn.getRollbackOnly()) {
                try {
                    trn.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        em.close();
        em = null;

    }

    @Override
    public int getChunkSize() {
        return sgidsm.getChunkSize();
    }


    @Override
    public int getRecovers() {
        return recovers;
    }

}
