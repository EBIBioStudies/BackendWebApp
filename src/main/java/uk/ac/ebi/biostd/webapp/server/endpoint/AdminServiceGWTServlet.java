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

package uk.ac.ebi.biostd.webapp.server.endpoint;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.BioStdService;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public class AdminServiceGWTServlet extends RemoteServiceServlet implements BioStdService {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String sessID = null;

        sessID = req.getHeader(BackendConfig.getSessionTokenHeader());

        if (sessID == null) {
            sessID = req.getParameter(BackendConfig.getSessionCookieName());

            if (sessID == null) {
                Cookie[] cuks = req.getCookies();

                if (cuks != null && cuks.length != 0) {
                    for (int i = cuks.length - 1; i >= 0; i--) {
                        if (cuks[i].getName().equals(BackendConfig.getSessionCookieName())) {
                            sessID = cuks[i].getValue();
                            break;
                        }
                    }
                }
            }
        }

        Session sess = null;

        if (sessID != null) {
            sess = BackendConfig.getServiceManager().getSessionManager().checkin(sessID);
        }

        try {
            super.service(req, resp);
        } finally {
            if (sess != null) {
                BackendConfig.getServiceManager().getSessionManager().checkout();
            }
        }
    }

    @Override
    public User getCurrentUser() {
        return BackendConfig.getServiceManager().getSessionManager().getEffectiveUser();
    }

    @Override
    public User login(String login, String pass) {

        Session sess = null;
        try {
            sess = BackendConfig.getServiceManager().getUserManager().login(login, pass, false);
        } catch (SecurityException e) {
            return null;
        }

        String skey = sess.getSessionKey();

        Cookie cke = new Cookie(BackendConfig.getSessionCookieName(), skey);
        cke.setPath(getServletContext().getContextPath());

        getThreadLocalResponse().addCookie(cke);

        return sess.getUser();
    }

}
