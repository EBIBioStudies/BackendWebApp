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
 * @author Andrew Tikhonov <andrew.tikhonov@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import uk.ac.ebi.biostd.authz.AttributeSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SubscriptionManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

/**
 * Created by andrew on 04/05/2017.
 */
public class JPASubscriptionManager implements SubscriptionManager {


    @Override
    public void addAttributeSubscription(String attributeName, String textPattern, User user) throws ServiceException {

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query exactSubscriptionQuery = em.createNamedQuery(AttributeSubscription.GetExactSubscriptonQuery);

            exactSubscriptionQuery.setParameter(AttributeSubscription.AttributeQueryParameter, attributeName);
            exactSubscriptionQuery.setParameter(AttributeSubscription.PatternQueryParameter, textPattern);
            exactSubscriptionQuery.setParameter(AttributeSubscription.UserIdQueryParameter, user.getId());

            List<AttributeSubscription> queryResult = exactSubscriptionQuery.getResultList();

            if (queryResult.size() != 0) {
                throw new ServiceException("Subscription exists");
            }

            AttributeSubscription subscription = new AttributeSubscription();

            subscription.setUser(user);
            subscription.setAttribute(attributeName);
            subscription.setPattern(textPattern);

            em.persist(subscription);
            trnOk = true;

        } finally {
            if (trn.isActive()) {
                try {
                    if (trnOk) {
                        trn.commit();
                    } else {
                        trn.rollback();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            em.close();
        }
    }

    @Override
    public void deleteAttributeSubscription(long subscriptionId) throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query query = em.createNamedQuery(AttributeSubscription.GetBySubscriptionIdQuery);

            query.setParameter(AttributeSubscription.SubscriptionIdQueryParameter, subscriptionId);

            List<AttributeSubscription> queryResult = query.getResultList();

            if (queryResult.size() == 0) {
                throw new ServiceException("Subscription doesn't exists");
            }

            AttributeSubscription subscription = queryResult.get(0);

            em.remove(subscription);
            trnOk = true;

        } finally {
            if (trn.isActive()) {
                try {
                    if (trnOk) {
                        trn.commit();
                    } else {
                        trn.rollback();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            em.close();
        }
    }

    @Override
    public Collection<AttributeSubscription> listAttributeSubscriptions(User user) throws ServiceException {

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = true;

        try {
            trn.begin();

            Query query = em.createNamedQuery(AttributeSubscription.GetAllByUserIdQuery);

            query.setParameter(AttributeSubscription.UserIdQueryParameter, user.getId());

            List<AttributeSubscription> queryResult = query.getResultList();

            /*
            // kick lazy loading
            for( AttributeSubscription ts : queryResult ) {
                ts.getAttribute().length();
            } */

            return queryResult;
        } catch (Throwable t) {
            trnOk = false;

            throw new ServiceException(t.getMessage(), t);
        } finally {
            if (trn.isActive()) {
                try {
                    if (trnOk) {
                        trn.commit();
                    } else {
                        trn.rollback();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            em.close();
        }
    }

    public void triggerAttributeEventProcessors() throws ServiceException {
        new Thread(AttributeSubscriptionProcessor::processEvents).start();
    }

    public void triggerTagEventProcessors() throws ServiceException {
        new Thread(() -> TagSubscriptionProcessor.processEvents()).start();
    }


}
