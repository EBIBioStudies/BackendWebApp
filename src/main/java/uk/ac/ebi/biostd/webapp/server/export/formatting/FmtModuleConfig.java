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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.util.ParamPool;

public class FmtModuleConfig {


    public static final String PublicOnlyParameter = "publicOnly";

    public static final String OutputFileParameter = "outfile";
    public static final String FormatParameter = "format";
    public static final String TmpDirParameter = "tmpdir";
    public static final String ChunkParameter = "chunkSize";
    public static final String FSProviderParameter = "fsProvider";

    private String format;
    private Boolean publicOnly;
    private String outputFile;
    private String tmpDir;
    private String fsProvider;
    private Map<String, String> formatterParams = new HashMap<>();
    private boolean chunkOutput;
    private long chunkSize;
    private boolean chunkSizeInUnits;

    public void loadParameters(ParamPool params, String pfx) throws TaskConfigException {
        if (pfx == null) {
            pfx = "";
        }

        format = params.getParameter(pfx + FormatParameter);

        outputFile = params.getParameter(pfx + OutputFileParameter);

        tmpDir = params.getParameter(pfx + TmpDirParameter);

        fsProvider = params.getParameter(pfx + FSProviderParameter);

        String pv = params.getParameter(pfx + PublicOnlyParameter);

        if (pv != null) {
            publicOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
        }

        pv = params.getParameter(pfx + ChunkParameter);

        if (pv != null) {
            pv = pv.trim();

            if (pv.length() == 0) {
                throw new TaskConfigException(
                        "FormattingOutputModule: invalid value of parameter: " + pfx + ChunkParameter);
            }

            char ch = pv.charAt(pv.length() - 1);

            int mult = 1;

            chunkSizeInUnits = false;

            if (Character.toUpperCase(ch) == 'K') {
                mult = 1024;
            } else if (Character.toUpperCase(ch) == 'M') {
                mult = 1024 * 1024;
            } else if (Character.toUpperCase(ch) == 'G') {
                mult = 1024 * 1024 * 1024;
            } else if (Character.toUpperCase(ch) == 'U' || Character.isDigit(ch)) {
                chunkSizeInUnits = true;
            } else {
                throw new TaskConfigException(
                        "FormattingOutputModule: invalid value of parameter: " + pfx + ChunkParameter + "=" + pv);
            }

            try {
                chunkSize = Math.round(Double.parseDouble(Character.isDigit(ch) ? pv : pv.substring(0, pv.length() - 1))
                        * mult);
            } catch (NumberFormatException e) {
                throw new TaskConfigException(
                        "FormattingOutputModule: invalid value of parameter: " + pfx + ChunkParameter + "=" + pv);
            }

            chunkOutput = true;

        } else {
            chunkOutput = false;
        }

        Enumeration<String> pnames = params.getNames();

        while (pnames.hasMoreElements()) {
            String nm = pnames.nextElement();

            if (nm.startsWith(FormatParameter + ".")) {
                formatterParams.put(nm.substring(FormatParameter.length() + 1), params.getParameter(nm));
            }
        }

    }


    public String getFsProvider() {
        return fsProvider;
    }


    public Boolean getPublicOnly(boolean def) {
        return publicOnly != null ? publicOnly : def;
    }


    public String getFormat(String def) {
        return format != null ? format : def;
    }


    public String getOutputFile(String def) {
        return outputFile != null ? outputFile : def;
    }


    public String getTmpDir(String def) {
        return tmpDir != null ? tmpDir : def;
    }

    public Map<String, String> getFormatterParams() {
        return formatterParams;
    }


    public boolean isChunkOutput() {
        return chunkOutput;
    }


    public long getChunkSize() {
        return chunkSize;
    }


    public boolean isChunkSizeInUnits() {
        return chunkSizeInUnits;
    }

}
