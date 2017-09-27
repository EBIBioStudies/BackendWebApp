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
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ReqResp;
import uk.ac.ebi.biostd.webapp.server.export.ExportTask;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;

public class ECTasks {

    static final String ThreadsParameter = "threads";
    static final String LockerParameter = "locker";


    static final int MaxThreads = 32;

    private static final Logger log = LoggerFactory.getLogger(ECTasks.class);

    static void reportTaskState(ReqResp rqrs, User usr) throws IOException {
        if (!usr.isSuperuser() && !BackendConfig.getServiceManager().getSecurityManager().mayUserControlExport(usr)) {
            rqrs.getResponse()
                    .respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User has no permission to view task state");
            return;
        }

        if (BackendConfig.getExportTask() == null) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is not configured");
            return;
        }

        ExportTask task = BackendConfig.getExportTask().getTask();

        rqrs.getResponse()
                .respond(HttpServletResponse.SC_OK, "OK", "Export task is " + (task.isBusy() ? "" : "not ") + "busy");
    }

    static void forceInterrupt(ReqResp rqrs, User usr) throws IOException {
        if (!usr.isSuperuser() && !BackendConfig.getServiceManager().getSecurityManager().mayUserControlExport(usr)) {
            rqrs.getResponse()
                    .respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User has no permission to interrupt task");
            return;
        }

        if (BackendConfig.getExportTask() == null) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is not configured");
            return;
        }

        ExportTask task = BackendConfig.getExportTask().getTask();

        if (!task.interrupt()) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is not busy");
            return;
        }

        rqrs.getResponse().respond(HttpServletResponse.SC_OK, "OK", "Export task interrupted");
    }


    static void forceExport(ReqResp rqrs, User usr) throws IOException {
        if (!usr.isSuperuser() && !BackendConfig.getServiceManager().getSecurityManager().mayUserControlExport(usr)) {
            rqrs.getResponse()
                    .respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User has no permission to interrupt task");
            return;
        }

        if (BackendConfig.getExportTask() == null) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is not configured");
            return;
        }

        ExportTask task = BackendConfig.getExportTask().getTask();

        if (task.isBusy()) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is busy");
            return;
        }

        int threads = task.getTaskConfig().getThreads(-1);

        String thrStr = rqrs.getParameterPool().getParameter(ThreadsParameter);

        if (thrStr != null) {
            try {
                threads = Integer.parseInt(thrStr);
            } catch (Exception e) {
                rqrs.getResponse().respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                        "Invalid parameter value " + ThreadsParameter + "=" + thrStr);
                return;
            }

            if (threads > MaxThreads) {
                rqrs.getResponse()
                        .respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Max threads exceeded: " + MaxThreads);
                return;
            }
        }

        final int tnum = threads;
        new Thread(new Runnable() {

            @Override
            public void run() {
                log.info("Starting export task by administrator's requst");

                try {
                    task.export(task.getTaskConfig().getLimit(-1), tnum);
                } catch (Throwable e) {
                    log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
                    e.printStackTrace();
                }

                log.info("Finishing manual task");

            }
        }, "Manual export task").start();

        rqrs.getResponse().respond(HttpServletResponse.SC_OK, "OK", "Export task started");
    }


    private static void lockExport(ReqResp rqrs, User usr, boolean lck) throws IOException {
        if (!usr.isSuperuser() && !BackendConfig.getServiceManager().getSecurityManager().mayUserLockExport(usr)) {
            rqrs.getResponse()
                    .respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User has no permission to lock export task");
            return;
        }

        String locker = rqrs.getParameterPool().getParameter(LockerParameter);
        if (locker == null || (locker = locker.trim()).length() == 0) {
            rqrs.getResponse()
                    .respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", LockerParameter + " parameter is not defined");
            return;
        }

        if (BackendConfig.getExportTask() == null) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "FAIL", "Export task is not configured");
            return;
        }

        TaskInfo taskInfo = BackendConfig.getExportTask();

        boolean res;

        String opName;

        if (lck) {
            opName = "lock";
            res = taskInfo.lock(locker);
        } else {
            opName = "unlock";
            res = taskInfo.unlock(locker);
        }

        if (res) {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "OK", "Export task successully " + opName + "ed");
        } else {
            rqrs.getResponse().respond(HttpServletResponse.SC_OK, "OK", "Export task was " + opName + "ed");
        }

    }

    public static void lockExport(ReqResp rqrs, User usr) throws IOException {
        lockExport(rqrs, usr, true);
    }

    public static void unlockExport(ReqResp rqrs, User usr) throws IOException {
        lockExport(rqrs, usr, false);
    }

}
