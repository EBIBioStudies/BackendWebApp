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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.treelog.JSON4Log;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager.Operation;

public class SubmitFormServlet extends ServiceServlet {


    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {

        if (sess == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        Part pt = req.getPart("Filedata");

        if (pt == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'Filedata' section is not defined");
            return;
        }

        InputStream is = pt.getInputStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[1000];

        int n;

        int count = 0;

        while ((n = is.read(buf)) != -1) {
            count += n;

            if (count > BackendConfig.maxPageTabSize) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "File size limit exeeded: " + BackendConfig.maxPageTabSize);
                return;
            }

            baos.write(buf, 0, n);
        }

        byte[] bindata = baos.toByteArray();

        LogNode ln = BackendConfig.getServiceManager().getSubmissionManager()
                .createSubmission(bindata, null, null, Operation.CREATE, sess.getUser(), true, false).getLog();

        JSON4Log.convert(ln, resp.getWriter());

    }

}
