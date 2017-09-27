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

import java.util.List;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.exception.UserMngException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public interface UserManager {

    Session login(String login, String password, boolean passHash) throws SecurityException;

    Session loginUsingSSOToken(User user, String ssoToken, String ssoSubject) throws SecurityException;

    void linkSSOSubjectToUser(User user, String ssoSubject) throws UserMngException;

    User getUserByLogin(String uName);

    User getUserByEmail(String email);

    User getUserBySSOSubject(String ssoSubject);

    int getUsersNumber();

    void addUser(User u, List<String[]> aux, boolean validateEmail, String actvURL) throws UserMngException;

    UserData getUserData(User user, String key);

    void storeUserData(UserData ud);

    boolean activateUser(ActivationInfo ainf) throws UserMngException;

    void passwordResetRequest(User usr, String resetURL) throws UserMngException;

    void resetPassword(ActivationInfo ainf, String pass) throws UserMngException;

    List<UserData> getAllUserData(User user);

    List<UserData> getUserDataByTopic(User user, String topic);

    UserGroup getGroup(String grName);

    void addGroup(UserGroup ug) throws UserMngException;

    void removeGroup(String grName) throws UserMngException;


}
