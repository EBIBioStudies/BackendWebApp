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

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;

public interface SessionManager {

    User getEffectiveUser();

    Session createSession(User user);

    Session createAnonymousSession();

    Session getSession(String sKey);

    Session getSession();

    Session getSessionByUserId(long id);

    boolean closeSession(String sKey);

    Session checkin(String sessId);

    Session checkout();

    boolean hasActiveSessions();

    void addSessionListener(SessionListener sl);

    void removeSessionListener(SessionListener sl);

    void shutdown();

}
