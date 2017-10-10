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

package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.in.pageml.PageMLElements;
import uk.ac.ebi.biostd.out.FormatterType;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class UpdateQueueProcessor implements Runnable {

    private static Logger log = null;


    private BlockingQueue<String> queue;
    private Thread myThread;

    private boolean shutdown = false;

    public UpdateQueueProcessor() {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        this.queue = new ArrayBlockingQueue<>(100);

        myThread = new Thread(this, "UpdateQueueProcessor");
    }

    public void start() {
        myThread.start();
    }

    public void shutdown() {
        shutdown = true;

        myThread.interrupt();
    }

    public void put(String msg) throws InterruptedException {
        queue.put(msg);
    }


    @Override
    public void run() {
        File f = null;
        Writer out = null;
        int outCap = BackendConfig.getMaxUpdatesPerFile();

        while (true) {
            String msg = null;

            while (true) {
                try {
                    if (out != null || shutdown) {
                        msg = queue.poll(BackendConfig.getUpdateWaitPeriod(), TimeUnit.SECONDS);
                    } else {
                        msg = queue.take();
                    }

                    break;
                } catch (InterruptedException ie) {
                    if (shutdown) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            if (msg == null) {
                if (out != null) {
                    closeFile(out, f);

                    out = null;
                    f = null;
                }

                if (shutdown) {
                    return;
                }
            } else {
                if (out != null && outCap <= 0) {
                    closeFile(out, f);

                    out = null;
                    f = null;
                }

                if (out == null) {
                    String fileExt = BackendConfig.getFrontendUpdateFormat().toLowerCase();

                    String fName =
                            String.valueOf(System.currentTimeMillis() / 1000) + "-" + BackendConfig.getSeqNumber() +
                                    "." + fileExt; // ".xml"

                    f = BackendConfig.getSubmissionUpdatePath().resolve(fName).toFile();

                    out = openFile(f);
                    outCap = BackendConfig.getMaxUpdatesPerFile();
                }

                if (out != null) {
                    outCap--;

                    try {
                        out.append(msg);
                    } catch (IOException e) {
                        log.error("File '" + f.getAbsolutePath() + "' IO error : " + e.getMessage());
                    }
                }
            }

        }
    }


    private Writer openFile(File f) {
        Writer out = null;

        try {
            out = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8); // Charsets.UTF_8

            if (FormatterType.XML.name().equalsIgnoreCase(BackendConfig.getFrontendUpdateFormat())) {
                out.append("<").append(PageMLElements.ROOT.getElementName()).append(">\n");
                out.append("<").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");
            } else if (FormatterType.JSON.name().equalsIgnoreCase(BackendConfig.getFrontendUpdateFormat())) {
                out.append("{\"").append(JSONFormatter.submissionsProperty).append("\":[\n");
            }

            return out;
        } catch (IOException e) {
            log.error("File '" + f.getAbsolutePath() + "' IO error : " + e.getMessage());

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e2) {
                }
            }

            f.delete();
            out = null;
            f = null;
        }

        return null;
    }

    private void closeFile(Writer out, File file) {
        try {
            if (FormatterType.XML.name().equalsIgnoreCase(BackendConfig.getFrontendUpdateFormat())) {
                out.append("</").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");
                out.append("</").append(PageMLElements.ROOT.getElementName()).append(">\n");
            } else if (FormatterType.JSON.name().equalsIgnoreCase(BackendConfig.getFrontendUpdateFormat())) {
                out.append("]}\n");
            }

            out.close();

            callUpdateListener(file.getName());
        } catch (IOException e) {
            log.error("File '" + file.getAbsolutePath() + "' IO error: " + e.getMessage());

            try {
                out.close();
            } catch (Exception e2) {
                log.error("File '" + file.getAbsolutePath() + "' close error : " + e.getMessage());
            }

            file.delete();
        }
    }

    private void callUpdateListener(String fName) {
        if (BackendConfig.getUpdateListenerURLPrefix() != null) {
            try {
                URL lUrl = new URL(BackendConfig.getUpdateListenerURLPrefix() + fName + BackendConfig
                        .getUpdateListenerURLPostfix());

                HttpURLConnection conn = (HttpURLConnection) lUrl.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(20000);

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    log.error(
                            "Update listener '" + lUrl + "' returned invalid responce code: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                log.error("Can't connect update listener: " + e.getMessage());
            }
        }
    }


    @SuppressWarnings("unused")
    private void sinkQueue(File f) throws IOException {
        int outCap = BackendConfig.getMaxUpdatesPerFile();

        try (Writer out = new FileWriter(f)) {
            out.append("<").append(PageMLElements.ROOT.getElementName()).append(">\n");
            out.append("<").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");

            String msg = null;

            while (outCap > 0 && (msg = queue.poll()) != null) {
                outCap--;

                out.append(msg);
            }

            out.append("</").append(PageMLElements.SUBMISSIONS.getElementName()).append(">\n");
            out.append("</").append(PageMLElements.ROOT.getElementName()).append(">\n");
        }

    }

}
