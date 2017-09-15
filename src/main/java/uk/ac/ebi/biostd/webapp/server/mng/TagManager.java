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

package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;
import uk.ac.ebi.biostd.authz.Classifier;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public interface TagManager {

    void createTag(String tagName, String desc, String classifierName, String parentTag, User user)
            throws SecurityException, ServiceException;

    void createClassifier(String classifierName, String description, User user)
            throws SecurityException, ServiceException;

    void deleteClassifier(String classifierName, User user) throws SecurityException, ServiceException;

    void deleteTag(String tagName, String classifierName, boolean cascade, User user)
            throws SecurityException, ServiceException;

    Collection<Tag> listTags() throws ServiceException;

    Collection<Classifier> listClassifiers() throws ServiceException;

    void renameClassifier(String classifierName, String newname, String description, User user)
            throws SecurityException, ServiceException;

    void renameTag(String tagName, String classifierName, String newname, String description, User user)
            throws SecurityException, ServiceException;

    void subscribeUser(String tagName, String classifierName, User user) throws ServiceException;

    void unsubscribeUser(String tagName, String classifierName, User user) throws ServiceException;

    Collection<TagSubscription> listSubscriptions(User user) throws ServiceException;

}
