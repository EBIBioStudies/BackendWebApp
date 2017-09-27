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

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import uk.ac.ebi.biostd.authz.Classifier;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.TagManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public class JPATagManager implements TagManager {

    @Override
    public void createTag(String tagName, String desc, String classifierName, String parentTag, User user)
            throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage tags");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Classifier.GetByNameQuery);

            clq.setParameter(Classifier.NameQueryParameter, classifierName);

            List<Classifier> clres = clq.getResultList();

            if (clres.size() != 1) {
                throw new ServiceException("Invalid classifier: '" + classifierName + "'");
            }

            Classifier clsf = clres.get(0);

            Query tgq = em.createNamedQuery(Tag.GetByNameQuery);

            tgq.setParameter(Tag.TagNameQueryParameter, tagName);
            tgq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);

            List<Tag> tgres = tgq.getResultList();

            if (tgres.size() > 0) {
                throw new ServiceException("Tag already exists: '" + classifierName + "." + tagName + "'");
            }

            Tag pTag = null;

            if (parentTag != null) {
                tgq.setParameter(Tag.TagNameQueryParameter, parentTag);
                tgq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);

                tgres = tgq.getResultList();

                if (tgres.size() != 1) {
                    throw new ServiceException("Invalid parent tag: '" + classifierName + "." + parentTag + "'");
                }

                pTag = tgres.get(0);
            }

            Tag nt = new Tag();

            nt.setName(tagName);
            nt.setDescription(desc);
            nt.setParentTag(pTag);
            nt.setClassifier(clsf);

            em.persist(nt);
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
    public void createClassifier(String classifierName, String description, User user)
            throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage classifiers");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Classifier.GetByNameQuery);

            clq.setParameter(Classifier.NameQueryParameter, classifierName);

            List<Classifier> clres = clq.getResultList();

            if (clres.size() > 0) {
                throw new ServiceException("Classifier already exists: '" + classifierName + "'");
            }

            Classifier ncl = new Classifier();

            ncl.setName(classifierName);
            ncl.setDescription(description);

            em.persist(ncl);
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
    public void deleteClassifier(String classifierName, User user) throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage classifiers");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Classifier.GetByNameQuery);

            clq.setParameter(Classifier.NameQueryParameter, classifierName);

            List<Classifier> clres = clq.getResultList();

            if (clres.size() == 0) {
                throw new ServiceException("Classifier doesn't exist: '" + classifierName + "'");
            }

            em.remove(clres.get(0));
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
    public void deleteTag(String tagName, String classifierName, boolean cascade, User user)
            throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage tags");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Tag.GetByNameQuery);

            clq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);
            clq.setParameter(Tag.TagNameQueryParameter, tagName);

            List<Tag> clres = clq.getResultList();

            if (clres.size() == 0) {
                throw new ServiceException("Tag doesn't exist: '" + classifierName + "." + tagName + "'");
            }

            Tag t = clres.get(0);

            if (!cascade) {
                if (t.getSubTags() != null) {
                    for (Tag st : t.getSubTags()) {
                        st.setParentTag(t.getParentTag());
                    }
                }

                t.getSubTags().clear();
            }

            em.remove(t);

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
    public Collection<Classifier> listClassifiers() throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = true;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Classifier.GetAllQuery);

            List<Classifier> res = clq.getResultList();

            return res;
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


    @Override
    public Collection<Tag> listTags() throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = true;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Tag.GetAllQuery);

            List<Tag> res = clq.getResultList();

            for (Tag t : res) {
                t.getClassifier().getName().length();
                if (t.getParentTag() != null) {
                    t.getParentTag().getName().length();
                }
            }

            return res;
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

    @Override
    public void renameClassifier(String classifierName, String newname, String description, User user)
            throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage classifiers");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Classifier.GetByNameQuery);

            clq.setParameter(Classifier.NameQueryParameter, classifierName);

            List<Classifier> clres = clq.getResultList();

            if (clres.size() == 0) {
                throw new ServiceException("Classifier doesn't exist: '" + classifierName + "'");
            }

            Classifier c = clres.get(0);

            if (newname != null && newname.length() > 0) {
                clq.setParameter(Classifier.NameQueryParameter, newname);

                clres = clq.getResultList();

                if (clres.size() != 0) {
                    throw new ServiceException("Classifier alredy exists: '" + newname + "'");
                }

                c.setName(newname);
            }

            if (description != null) {
                c.setDescription(description);
            }

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
    public void renameTag(String tagName, String classifierName, String newname, String description, User user)
            throws SecurityException, ServiceException {
        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags(user)) {
            throw new SecurityException("User has no perimission to manage tags");
        }

        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(Tag.GetByNameQuery);

            clq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);
            clq.setParameter(Tag.TagNameQueryParameter, tagName);

            List<Tag> clres = clq.getResultList();

            if (clres.size() == 0) {
                throw new ServiceException("Tag doesn't exist: '" + classifierName + "." + tagName + "'");
            }

            Tag t = clres.get(0);

            if (newname != null && newname.length() > 0) {
                clq.setParameter(Tag.TagNameQueryParameter, newname);

                clres = clq.getResultList();

                if (clres.size() != 0) {
                    throw new ServiceException("Tag alredy exists: '" + classifierName + "." + newname + "'");
                }

                t.setName(newname);
            }

            if (description != null) {
                t.setDescription(description);
            }

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
    public void subscribeUser(String tagName, String classifierName, User user) throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query tgq = em.createNamedQuery(Tag.GetByNameQuery);

            tgq.setParameter(Tag.TagNameQueryParameter, tagName);
            tgq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);

            List<Tag> tgres = tgq.getResultList();

            if (tgres.size() != 1) {
                throw new ServiceException("Tag doesn't exist: '" + classifierName + "." + tagName + "'");
            }

            Tag t = tgres.get(0);

            Query clq = em.createNamedQuery(TagSubscription.GetByTagIdAndUserQuery);

            clq.setParameter(TagSubscription.TagIdQueryParameter, t.getId());
            clq.setParameter(TagSubscription.UserIdQueryParameter, user.getId());

            List<TagSubscription> clres = clq.getResultList();

            if (clres.size() != 0) {
                throw new ServiceException("Subscription exists");
            }

            TagSubscription ts = new TagSubscription();

            ts.setTag(t);
            ts.setUser(user);

            em.persist(ts);
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
    public void unsubscribeUser(String tagName, String classifierName, User user) throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = false;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(TagSubscription.GetByTagAndUserQuery);

            clq.setParameter(TagSubscription.TagNameQueryParameter, tagName);
            clq.setParameter(TagSubscription.ClassifierNameQueryParameter, classifierName);
            clq.setParameter(TagSubscription.UserIdQueryParameter, user.getId());

            List<TagSubscription> clres = clq.getResultList();

            if (clres.size() == 0) {
                throw new ServiceException("Subscription doesn't exists");
            }

            TagSubscription ts = clres.get(0);

            em.remove(ts);
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
    public Collection<TagSubscription> listSubscriptions(User user) throws ServiceException {
        EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction trn = em.getTransaction();

        boolean trnOk = true;

        try {
            trn.begin();

            Query clq = em.createNamedQuery(TagSubscription.GetAllByUserQuery);

            clq.setParameter(TagSubscription.UserIdQueryParameter, user.getId());

            List<TagSubscription> res = clq.getResultList();

            for (TagSubscription ts : res) {
                ts.getTag().getClassifier().getName().length();
            }

            return res;
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

}
