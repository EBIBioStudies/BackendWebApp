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

package uk.ac.ebi.biostd.webapp.server.mng.security;

import java.util.Collection;
import uk.ac.ebi.biostd.authz.AuthorizationTemplate;
import uk.ac.ebi.biostd.authz.AuthzObject;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public interface SecurityManager {

    void init();

    void applyTemplate(AuthzObject gen, AuthorizationTemplate authorizationTemplate);

    void removeExpiredUsers();

    void refreshUserCache();

    User checkUserLogin(String login, String pass, boolean passHash) throws SecurityException;

    User checkUserSSOSubject(String ssoSubject) throws SecurityException;

    void addUserSSOSubject(User user, String ssoSubject);

    boolean mayUserListAllSubmissions(User u);

    boolean mayUserReadSubmission(Submission sub, User user);

    boolean mayUserCreateSubmission(User usr);

    boolean mayUserUpdateSubmission(Submission oldSbm, User usr);

    boolean mayUserDeleteSubmission(Submission sbm, User usr);

    boolean mayUserAttachToSubmission(Submission s, User usr);

    boolean mayEveryoneReadSubmission(Submission submission);


    boolean mayUserCreateIdGenerator(User usr);

    User addUser(User u) throws ServiceException;

    boolean addUserToGroup(User usr, UserGroup grp) throws ServiceException;

    boolean removeUserFromGroup(User usr, UserGroup grp) throws ServiceException;

    User getUserById(long id);

    User getUserByLogin(String login);

    User getUserByEmail(String email);

    User getUserBySSOSubject(String ssoSubject);

    User getAnonymousUser();

    UserGroup addGroup(UserGroup ug) throws ServiceException;

    UserGroup getGroup(String name);

    Collection<UserGroup> getGroups();

    void removeGroup(long id) throws ServiceException;

    boolean mayUserManageTags(User user);


    boolean mayUserCreateGroup(User usr);

    boolean mayUserReadGroupFiles(User user, UserGroup g);

    boolean mayUserWriteGroupFiles(User user, UserGroup group);

    boolean mayUserChangeGroup(User usr, UserGroup grp);

    boolean mayUserControlExport(User usr);

    boolean mayUserLockExport(User usr);

    int getUsersNumber();

    void setPermission(PermissionClass pClass, String pID, boolean pAction, SubjectClass sClass, String sID,
            ObjectClass oClass, String oID, User user) throws SecurityException;

    void clearPermission(PermissionClass pClass, String pID, boolean pAction, SubjectClass sClass, String sID,
            ObjectClass oClass, String oID, User user) throws SecurityException;

}
