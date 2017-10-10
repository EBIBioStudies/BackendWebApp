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

package uk.ac.ebi.biostd.webapp.server.export.formatting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.out.TextStreamFormatter;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.config.ConfigurationManager;
import uk.ac.ebi.biostd.webapp.server.export.ExporterStat;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.util.MapParamPool;

public class FormattingOutputModule implements OutputModule {

    static final boolean DefaultShowNS = false;
    static final boolean DefaultShowAC = true;

    static final boolean DefaultPublicOnly = false;
    private static Logger log = null;
    private final String name;
    private final File outFile;
    private final File tmpDir;
    private TextStreamFormatter formatter;
    private File tmpFile;
    private PrintStream tmpStream;
    private Date startTime;

    public FormattingOutputModule(String name, Map<String, String> cfgMap) throws TaskConfigException {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        this.name = name;

        FmtModuleConfig cfg = new FmtModuleConfig();

        cfg.loadParameters(new MapParamPool(cfgMap), "");

        String fmtr = cfg.getFormat(null);

        if (fmtr == null) {
            throw new TaskConfigException("Output module '" + name + "': '" + FmtModuleConfig.FormatParameter
                    + "' parameter not defined. It should point to formatter class");
        }

        Class<?> fmtCls = null;

        try {
            fmtCls = Class.forName(fmtr);
        } catch (ClassNotFoundException e) {
            throw new TaskConfigException("Output module '" + name + "': Formatter class '" + fmtCls + "' not found");
        }

        if (!TextStreamFormatter.class.isAssignableFrom(fmtCls)) {
            throw new TaskConfigException(
                    "Output module '" + name + "': Class '" + fmtCls + "' doesn't implement Formatter interface");
        }

        Constructor<?> ctor = null;

        try {
            try {
                ctor = fmtCls.getConstructor(Map.class);
                formatter = (TextStreamFormatter) ctor.newInstance(cfg.getFormatterParams());
            } catch (NoSuchMethodException e) {
                try {
                    ctor = fmtCls.getConstructor();
                    formatter = (TextStreamFormatter) ctor.newInstance();
                } catch (NoSuchMethodException e1) {
                    throw new TaskConfigException(
                            "Output module '" + name + "': Can't find appropriate constructor of class '" + fmtCls
                                    + "'");
                }
            } catch (SecurityException e) {
                throw new TaskConfigException(
                        "Output module '" + name + "': Can't get constructor of class '" + fmtCls + "' " + e
                                .getMessage());
            }
        } catch (Exception ex) {
            throw new TaskConfigException(
                    "Output module '" + name + "': Can't create instance of class '" + fmtCls + "'");
        }

        String tmpDirName = cfg.getTmpDir(null);

        if (tmpDirName == null) {
            throw new TaskConfigException("Output module '" + name + "': Temp directory is not defined");
        }

        Path tmpPath = FileSystems.getDefault().getPath(tmpDirName);

        if (!tmpPath.isAbsolute()) {
            if (BackendConfig.getBaseDirectory() == null) {
                log.error("Output module '" + name
                        + "': Temporary directory path should be absolute or relative to path defined by '"
                        + ConfigurationManager.BaseDirParameter + "' parameter");
                throw new TaskConfigException("Output module '" + name
                        + "': Temporary directory path should be absolute or relative to path defined by '"
                        + ConfigurationManager.BaseDirParameter + "' parameter");
            }

            tmpPath = BackendConfig.getBaseDirectory().resolve(tmpPath);
        }

        tmpDir = tmpPath.toFile();

        if (!tmpDir.exists() && BackendConfig.isCreateFileStructure()) {
            tmpDir.mkdirs();
        }

        if (!tmpDir.canWrite()) {
            log.error("Output module '" + name + "': Temporary directory is not writable: " + tmpDir);
            throw new TaskConfigException(
                    "Output module '" + name + "': Temporary directory is not writable: " + tmpDir);
        }

        String outFileName = cfg.getOutputFile(null);

        if (outFileName == null) {
            throw new TaskConfigException("Output module '" + name + "': Output file is not defined");
        }

        Path outPath = FileSystems.getDefault().getPath(outFileName);

        if (!outPath.isAbsolute()) {
            if (BackendConfig.getBaseDirectory() == null) {
                log.error("Output module '" + name
                        + "': Output file path should be absolute or relative to path defined by '"
                        + ConfigurationManager.BaseDirParameter + "' parameter");
                throw new TaskConfigException("Output module '" + name
                        + "': TOutput file path should be absolute or relative to path defined by '"
                        + ConfigurationManager.BaseDirParameter + "' parameter");
            }

            outPath = BackendConfig.getBaseDirectory().resolve(outPath);
        }

        outFile = outPath.toFile();

        File outPDir = outFile.getParentFile();

        if (!outPDir.exists() && BackendConfig.isCreateFileStructure()) {
            outPDir.mkdirs();
        }

        if (!outPDir.isDirectory() || !outPDir.canWrite()) {
            log.error("Output module '" + name + "': Output file directory is not writable: " + outPDir);
            throw new TaskConfigException(
                    "Output module '" + name + "': Output file directory is not writable: " + outPDir);
        }

    }


    @Override
    public TextStreamFormatter getFormatter() {
        return formatter;
    }

    @Override
    public Appendable getOut() {
        return tmpStream;
    }


    @Override
    public void start() throws IOException {

        startTime = new java.util.Date();

        tmpFile = new File(tmpDir, "biostd" + name.hashCode() + "_" + System.currentTimeMillis() + ".tmp");

        tmpStream = new PrintStream(tmpFile, "UTF-8");

        log.debug("Starting export module for task '" + name + "'");

    }

    @Override
    public void finish(ExporterStat stat) throws IOException {

        formatter.footer(tmpStream);

        Date endTime = new java.util.Date();

//  String summary = stat.createReport(startTime, endTime, stat.getThreads());
//  
//  formatter.comment(summary,tmpStream);

        if (tmpStream != null) {
            tmpStream.close();
        }

        Map<String, List<String>> hdrs = new HashMap<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        hdrs.put("startTime", Collections.singletonList(simpleDateFormat.format(startTime)));
        hdrs.put("startTimeTS", Collections.singletonList(String.valueOf(startTime.getTime() / 1000)));
        hdrs.put("endTime", Collections.singletonList(simpleDateFormat.format(endTime)));
        hdrs.put("endTimeTS", Collections.singletonList(String.valueOf(endTime.getTime() / 1000)));
        hdrs.put("elapsedTime", Collections
                .singletonList(String.valueOf(StringUtils.millisToString(endTime.getTime() - startTime.getTime()))));
        hdrs.put("submissions", Collections.singletonList(String.valueOf(stat.getSubmissionCount())));
        hdrs.put("threads", Collections.singletonList(String.valueOf(stat.getThreads())));
        hdrs.put("ioErrors", Collections.singletonList(String.valueOf(stat.getRecoverAttempt())));

        File tmpOutFile = new File(outFile.getAbsolutePath() + ".tmp");

        try
                (
                        PrintWriter outStream = new PrintWriter(tmpOutFile, "utf-8");
                        Reader tmpRd = new InputStreamReader(new FileInputStream(tmpFile), "utf-8")
                ) {
            formatter.header(hdrs, outStream);

            char[] buf = new char[4096 * 1000];

            int read;

            while ((read = tmpRd.read(buf)) > 0) {
                outStream.write(buf, 0, read);
            }
        }

        if (!tmpFile.delete()) {
            log.warn("Task '" + name + "': Can't delete temporary file: " + tmpFile);
        }

        if (outFile.exists() && !outFile.delete()) {
            log.error("Task '" + name + "': Can't delete file: " + outFile);
        }

        if (!tmpOutFile.renameTo(outFile)) {
            log.error("Task '" + name + "': Moving aux file failed. {} -> {} ", tmpOutFile.getAbsolutePath(),
                    outFile.getAbsolutePath());
        }

        tmpStream = null;
    }

    @Override
    public void cancel() {
        if (tmpStream != null) {
            tmpStream.close();
        }

        if (tmpFile != null) {
            tmpFile.delete();
        }

        tmpStream = null;
    }


    private boolean checkDirs() {
        File outDir = outFile.getParentFile();

        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                log.error("Task '" + name + "': Can't create output directory: {}", outDir.getAbsolutePath());
                return false;
            }
        }

        if (!outDir.canWrite()) {
            log.error("Task '" + name + "': Output directory is not writable: {}", outDir.getAbsolutePath());
            return false;
        }

        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                log.error("Task '" + name + "': Can't create temp directory: {}", tmpDir.getAbsolutePath());
                return false;
            }
        }

        if (!tmpDir.canWrite()) {
            log.error("Task '" + name + "': Temp directory is not writable: {}", tmpDir.getAbsolutePath());
            return false;
        }

        return true;
    }


    @Override
    public String getName() {
        return name;
    }
}
