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

package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.db.AdHocTagResolver;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.out.pageml.PageMLFormatter;
import uk.ac.ebi.biostd.treelog.JSON4Log;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.server.mng.impl.PTDocumentParser;

public class Converter {

    public static void convert(byte[] data, DataFormat fmt, String outFmt, HttpServletResponse response)
            throws IOException {
        ParserConfig pc = new ParserConfig();

        pc.setMultipleSubmissions(true);
        pc.setPreserveId(false);

        SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Converting " + fmt.name() + " document", null);

        PMDoc doc = new PTDocumentParser(pc).parseDocument(data, fmt, "UTF-8", new AdHocTagResolver(), gln);

        Writer out = response.getWriter();

        if ("xml".equalsIgnoreCase(outFmt)) {
            if (doc == null) {
                out.append("FAIL Invalid document");
            } else {
                response.setContentType("text/xml");
                new PageMLFormatter(out, false).format(doc);
            }
        } else {
            response.setContentType("application/json");

            SimpleLogNode.setLevels(gln);

            out.append("{\n\"status\": ");

            if (gln.getLevel().getPriority() < Level.ERROR.getPriority()) {
                out.append("\"OK\"");
            } else {
                out.append("\"FAIL\"");
            }

            out.append(",\n\"log\": ");
            JSON4Log.convert(gln, out);

            out.append(",\n\"document\": ");

            new JSONFormatter(response.getWriter(), true).format(doc);

            out.append("\n}");

        }
    }

}
