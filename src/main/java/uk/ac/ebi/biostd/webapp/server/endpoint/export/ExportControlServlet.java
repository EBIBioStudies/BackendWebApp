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

package uk.ac.ebi.biostd.webapp.server.endpoint.export;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.endpoint.ReqResp;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class ExportControlServlet extends ServiceServlet {

    static final long serialVersionUID = 1L;

    static final String CommandForceTask = "force";
    static final String CommandInterruptTask = "interrupt";
    static final String CommandLockExport = "lock";
    static final String CommandUnlockExport = "unlock";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {
        ReqResp rqrs = new ReqResp(req, resp);

        if (sess == null || sess.isAnonymouns()) {
            rqrs.getResponse().respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
            return;
        }

        String cmd = req.getPathInfo();

        if (cmd != null && cmd.charAt(0) == '/') {
            cmd = cmd.substring(1);
        }

        if (CommandForceTask.equals(cmd)) {
            ECTasks.forceExport(rqrs, sess.getUser());
        } else if (CommandInterruptTask.equals(cmd)) {
            ECTasks.forceInterrupt(rqrs, sess.getUser());
        } else if (CommandLockExport.equals(cmd)) {
            ECTasks.lockExport(rqrs, sess.getUser());
        } else if (CommandUnlockExport.equals(cmd)) {
            ECTasks.unlockExport(rqrs, sess.getUser());
        } else if (cmd == null || cmd.length() == 0) {
            ECTasks.reportTaskState(rqrs, sess.getUser());
        } else {
            rqrs.getResponse().respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid operation");
        }


    }


}
