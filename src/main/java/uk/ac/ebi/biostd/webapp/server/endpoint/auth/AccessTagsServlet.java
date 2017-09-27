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

package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.BuiltInUsers;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public class AccessTagsServlet extends HttpServlet {

    public static final String loginParameter = "login";
    public static final String passwordParameter = "password";
    public static final String hashParameter = "hash";

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String login = null;
        String pass = null;
        String hash = null;

        if ("text/plain".equalsIgnoreCase(req.getContentType())) {
            BufferedReader reader = req.getReader();

            String line = null;

            while ((line = reader.readLine()) != null) {
                int pos = line.indexOf(":");

                if (pos < 0) {
                    continue;
                }

                String pname = line.substring(0, pos).trim();
                String pval = line.substring(pos + 1).trim();

                if (loginParameter.equals(pname)) {
                    login = pval;
                } else if (passwordParameter.equals(pname)) {
                    pass = pval;
                } else if (hashParameter.equals(pname)) {
                    hash = pval;
                }
            }
        } else {
            login = req.getParameter(loginParameter);
            pass = req.getParameter(passwordParameter);
            hash = req.getParameter(hashParameter);
        }

        if (pass != null && pass.length() == 0) {
            pass = null;
        }

        if (hash != null && hash.length() == 0) {
            hash = null;
        }

        if (login == null || (pass == null && hash == null && !BuiltInUsers.Guest.getUserName().equals(login))) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Status: INVALID REQUEST");
            return;
        }

        User usr = null;

        if (BuiltInUsers.Guest.getUserName().equals(login)) {
            if (pass != null || hash != null) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().println("Status: FAILED");
                return;
            }

            usr = BackendConfig.getServiceManager().getSecurityManager()
                    .getUserByLogin(BuiltInUsers.Guest.getUserName());
        } else {
            boolean useHash = false;
            if (pass == null) {
                pass = hash;
                useHash = true;
            }

            try {
                usr = BackendConfig.getServiceManager().getSecurityManager().checkUserLogin(login, pass, useHash);
            } catch (SecurityException e1) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().println("Status: FAILED");
                return;
            }
        }

        EntityManager em = null;

        try {
            em = BackendConfig.getEntityManagerFactory().createEntityManager();

            StringBuilder allow = new StringBuilder();
            StringBuilder deny = new StringBuilder();

            String txtId = usr.getEmail();

            if (txtId == null) {
                txtId = usr.getLogin();
            }

            allow.append('~').append(txtId).append(';');
            allow.append('#').append(usr.getId()).append(';');

            Query q = em.createQuery("SELECT t FROM AccessTag t");

            for (AccessTag t : (List<AccessTag>) q.getResultList()) {
                Permit p = t.checkDelegatePermission(SystemAction.READ, usr);

                if (p == Permit.ALLOW) {
                    allow.append(t.getName()).append(';');
                } else if (p == Permit.DENY) {
                    deny.append(t.getName()).append(';');
                }
            }

            allow.setLength(allow.length() - 1);

            if (deny.length() > 0) {
                deny.setLength(deny.length() - 1);
            }

            resp.getWriter().println("Status: OK");
            resp.getWriter().println("Allow: " + allow.toString());
            resp.getWriter().println("Deny: " + deny.toString());
            resp.getWriter().println("Superuser: " + (usr.isSuperuser() ? "true" : "false"));
            resp.getWriter().println("Name: " + (usr.getFullName() != null ? usr.getFullName() : ""));
            resp.getWriter().println("Login: " + (usr.getLogin() != null ? usr.getLogin() : ""));
            resp.getWriter().println("EMail: " + (usr.getEmail() != null ? usr.getEmail() : ""));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Status: SERVER ERROR");
        } finally {
            if (em != null) {
                em.close();
            }
        }

    }


}
