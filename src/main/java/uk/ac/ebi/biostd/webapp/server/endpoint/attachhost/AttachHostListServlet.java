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

package uk.ac.ebi.biostd.webapp.server.endpoint.attachhost;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONHttpResponse;
import uk.ac.ebi.biostd.webapp.server.endpoint.Response;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.endpoint.TextHttpResponse;

public class AttachHostListServlet extends ServiceServlet {

    public static final String FormatParameter = "format";
    public static final String TypeParameter = "type";
    public static final String DefaultResponseFormat = "xml";
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AttachHostListServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        String format = req.getParameter(FormatParameter);

        if (format == null) {
            format = DefaultResponseFormat;
        }

        if (sess == null || sess.isAnonymouns()) {
            getResponse(format, resp).respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
            return;
        }

        String acc = req.getParameter(TypeParameter);

        if (acc == null || acc.length() < 1) {
            getResponse(format, resp)
                    .respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request. Type is missing");
            return;
        }

        List<Submission> subs = BackendConfig.getServiceManager().getSubmissionManager()
                .getHostSubmissionsByType(acc, sess.getUser());

        if ("json".equalsIgnoreCase(format)) {
            JSONRenderer.render(subs, out);
        } else {
            for (Submission s : subs) {
                out.printf("ID:%d AccNo:%s Title: %s\n", s.getId(), s.getAccNo(), s.getTitle());
            }
        }

    }

    private Response getResponse(String fmt, HttpServletResponse response) {
        Response resp = null;

        if ("json".equalsIgnoreCase(fmt)) {
            resp = new JSONHttpResponse(response);
        } else {
            resp = new TextHttpResponse(response);
        }

        return resp;

    }
}