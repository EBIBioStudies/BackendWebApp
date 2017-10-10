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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import uk.ac.ebi.biostd.util.StringUtils;

public class ExporterStat {

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Date now;
    private int idCount = 0;
    private int submissionCount = 0;
    private int threads;
    private int errorRecoverCount = 0;

    private Map<String, Integer> outStat = new HashMap<>();

    public ExporterStat(Date now) {
        this.now = now;
    }

    public void reset() {
    }

    public Date getNowDate() {
        return now;
    }

    public synchronized void incRecoverAttempt() {
        errorRecoverCount++;
    }


    public synchronized void addRecoverAttempt(int recovers) {
        errorRecoverCount += recovers;
    }

    public synchronized int getRecoverAttempt() {
        return errorRecoverCount;
    }


    public int getSubmissionCount() {
        return submissionCount;
    }


    public synchronized void incSubmissionCount() {
        submissionCount++;
    }

    public Map<String, Integer> getOutStat() {
        return outStat;
    }

    public void addOutStat(String outName, int count) {
        outStat.put(outName, count);
    }

    public String createReport(Date startTime, Date endTime, int threads) {
        long startTs = startTime.getTime();
        long endTs = endTime.getTime();

        long rate = getSubmissionCount() != 0 ? (endTs - startTs) / getSubmissionCount() : 0;

        StringBuffer summaryBuf = new StringBuffer();

        summaryBuf.append("\n<!-- Exported: ").append(getSubmissionCount()).append(" submissions in ").append(threads)
                .append(" threads. Rate: ").append(rate).append("ms per submission -->");
        summaryBuf.append("\n<!-- ID selected: ").append(getIdCount()).append(" -->");

        for (Map.Entry<String, Integer> me : outStat.entrySet()) {
            summaryBuf.append("\n<!-- Output '").append(me.getKey()).append("' : ").append(me.getValue())
                    .append(" -->");
        }

        summaryBuf.append("\n<!-- Start time: ").append(simpleDateFormat.format(startTime)).append(" -->");
        summaryBuf.append("\n<!-- End time: ").append(simpleDateFormat.format(endTime))
                .append(". Time spent: " + StringUtils.millisToString(endTs - startTs)).append(" -->");
        summaryBuf.append("\n<!-- I/O error recovered: ").append(getRecoverAttempt()).append(" -->");
        summaryBuf.append("\n<!-- Thank you. Good bye. -->\n");

        return summaryBuf.toString();

    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getIdCount() {
        return idCount;
    }

    public void setIdCount(int idCount) {
        this.idCount = idCount;
    }

}
