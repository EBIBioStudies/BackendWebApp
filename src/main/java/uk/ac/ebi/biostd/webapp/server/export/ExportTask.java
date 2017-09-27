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

package uk.ac.ebi.biostd.webapp.server.export;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class ExportTask {

    public static final boolean DefaultShowNS = false;
    public static final int DefaultSliceSize = 10;
    public static final int DefaultThreadSoftTTL = 4000;
    public static final int DefaultThreadTTL = 5000;
    private static Logger log = null;
    private final String name;
    private final EntityManagerFactory emf;
    private final Collection<OutputModule> modules;
    private final TaskConfig taskConfig;
    private final Lock busy = new ReentrantLock();
    ExporterMTControl exportControl;


    public ExportTask(String nm, EntityManagerFactory emf, Collection<OutputModule> modules, TaskConfig cnf)
            throws TaskInitError {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        name = nm;

        this.emf = emf;

        this.modules = modules;

        taskConfig = cnf;
    }


    public boolean isBusy() {
        if (!busy.tryLock()) {
            return true;
        }

        busy.unlock();

        return false;
    }

    public boolean export(long limit, int threads) throws Throwable {
        if (!busy.tryLock()) {
            log.info("Export in progress. Skiping");
            return false;
        }

        if (threads <= 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }

        try {

            log.debug("Start exporting data for task '" + name + "'");

            synchronized (this) {
                exportControl = new ExporterMTControl(emf, modules, threads,
                        taskConfig.getSliceSize(DefaultSliceSize),
                        taskConfig.getThreadTTL(DefaultThreadTTL));
            }

            Date startTime = new Date();

            try {
                ExporterStat stat = exportControl.export(-1, limit, startTime);

                Date endTime = new Date();

                if (BackendConfig.getServiceManager().getEmailService() != null) {
                    if (!BackendConfig.getServiceManager().getEmailService().sendAnnouncement(
                            "BioStudy export task '" + name + "' success " + StringUtils
                                    .millisToString(endTime.getTime() - startTime.getTime()) + (
                                    stat.getRecoverAttempt() > 0 ? " I/O errors recovered: " + stat.getRecoverAttempt()
                                            : ""),
                            "Task '" + name + "' has finished successfully\n\n" + stat
                                    .createReport(startTime, endTime, threads))) {
                        log.error("Can't send an info announcement by email");
                    }
                }

            } catch (Throwable t) {
                log.error("Task '" + name + "': XML generation terminated with error: " + t.getMessage());

                if (BackendConfig.getServiceManager().getEmailService() != null) {
                    if (!BackendConfig.getServiceManager().getEmailService()
                            .sendErrorAnnouncement("BioStudy export '" + name + "' error",
                                    "Task '" + name + "': XML generation terminated with error", t)) {
                        log.error("Can't send an error announcement by email");
                    }
                }
            }

        } finally {
            busy.unlock();

            synchronized (this) {
                if (exportControl != null) {
                    exportControl.interrupt();
                }

                exportControl = null;
            }

        }

        return true;
    }


    public boolean interrupt() {

        if (busy.tryLock()) {
            busy.unlock();
            return false;
        }

        synchronized (this) {
            if (exportControl != null) {
                exportControl.interrupt();
            }
        }

        busy.lock(); // waiting until task is idle
        busy.unlock();

        return true;

    }


    public String getName() {
        return name;
    }


    public TaskConfig getTaskConfig() {
        return taskConfig;
    }

}
