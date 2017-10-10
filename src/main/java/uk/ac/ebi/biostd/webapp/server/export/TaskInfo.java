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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class TaskInfo extends TimerTask {

    private final Logger log = LoggerFactory.getLogger(TaskInfo.class);

    private Timer timer;

    private ExportTask task;
    private long timeZero = -1;
    private int enqueueTimeMoD = -1;

    private int periodMin = -1;

    private Map<String, Long> lockMap = new HashMap<>();

    private boolean enqueued;

    public boolean isEnqueued() {
        return enqueued;
    }

    public void setEnqueued(boolean enqueued) {
        this.enqueued = enqueued;
    }

    public long getEnqueueTime() {
        return enqueueTimeMoD;
    }

    public void setEnqueueTime(int enqueueTime) {
        this.enqueueTimeMoD = enqueueTime;
    }

    public int getPeriod() {
        return periodMin;
    }

    public void setPeriod(int period) {
        this.periodMin = period;
    }

    public ExportTask getTask() {
        return task;
    }

    public void setTask(ExportTask task) {
        this.task = task;
    }

    public long getTimeZero() {
        return timeZero;
    }

    public void setTimeZero(long tz) {
        this.timeZero = tz;
    }


    @Override
    public void run() {

        boolean delay = false;

        synchronized (lockMap) {
            if (lockMap.size() > 0) {
                long ctime = System.currentTimeMillis();

                Iterator<Map.Entry<String, Long>> iter = lockMap.entrySet().iterator();

                while (iter.hasNext()) {
                    long ltime = iter.next().getValue();

                    if ((ctime - ltime) > BackendConfig.getExportLockTimeoutMsec()) {
                        iter.remove();
                    } else {
                        delay = true;
                    }
                }
            }
        }

        if (delay) {
            log.info("Delaying export task: " + task.getName());

            getTimer().schedule(this, BackendConfig.getExportLockDelayMsec());

            return;
        }

        log.info("Starting scheduled task: " + task.getName());

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    task.export(task.getTaskConfig().getLimit(-1), task.getTaskConfig().getThreads(-1));
                } catch (Throwable e) {
                    log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
                    e.printStackTrace();
                }

                log.info("Finishing scheduled task: " + task.getName());

            }
        }, "Task '" + task.getName() + "' export").start();
    }

    public Timer getTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        return timer;

    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public boolean lock(String locker) {
        synchronized (lockMap) {
            boolean res = lockMap.put(locker, System.currentTimeMillis()) != null;

            return res;
        }
    }

    public boolean unlock(String locker) {
        synchronized (lockMap) {
            boolean res = lockMap.remove(locker) != null;

            return res;
        }
    }

}
