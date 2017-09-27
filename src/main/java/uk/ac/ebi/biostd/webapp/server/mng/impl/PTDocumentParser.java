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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.odftoolkit.simple.SpreadsheetDocument;
import uk.ac.ebi.biostd.db.TagResolver;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.in.json.JSONReader;
import uk.ac.ebi.biostd.in.pagetab.PageTabSyntaxParser;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.model.AbstractAttribute;
import uk.ac.ebi.biostd.model.Annotated;
import uk.ac.ebi.biostd.model.FileRef;
import uk.ac.ebi.biostd.model.Link;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.mg.spreadsheet.SpreadsheetReader;
import uk.ac.ebi.mg.spreadsheet.readers.CSVTSVSpreadsheetReader;
import uk.ac.ebi.mg.spreadsheet.readers.ODSpreadsheetReader;
import uk.ac.ebi.mg.spreadsheet.readers.XLSpreadsheetReader;

public class PTDocumentParser {

    private ParserConfig parserCfg;

    public PTDocumentParser(ParserConfig pc) {
        parserCfg = pc;
    }

    public PMDoc parseDocument(byte[] data, DataFormat type, String charset, TagResolver tagRslv, LogNode gln) {
        PMDoc doc = null;
        SpreadsheetReader reader = null;

        switch (type) {
            case xml:

                gln.log(Level.ERROR, "XML submission are not supported yet");

                return null;

            case json:

                String txt = convertToText(data, charset, gln);

                if (txt != null) {
                    doc = new JSONReader(tagRslv, parserCfg).parse(txt, gln);
                }

                break;

            case xlsx:
            case xls:

                try {
                    Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data));
                    reader = new XLSpreadsheetReader(wb);
                } catch (Exception e) {
                    gln.log(Level.ERROR, "Can't read Excel file: " + e.getMessage());
                }

                break;

            case ods:

                try {
                    SpreadsheetDocument odsdoc = SpreadsheetDocument.loadDocument(new ByteArrayInputStream(data));
                    reader = new ODSpreadsheetReader(odsdoc);
                } catch (Exception e) {
                    gln.log(Level.ERROR, "Can't read ODS file: " + e.getMessage());
                }

                break;

            case csv:
            case tsv:
            case csvtsv:

                txt = convertToText(data, charset, gln);

                if (txt != null) {
                    reader = new CSVTSVSpreadsheetReader(txt,
                            type == DataFormat.csv ? ',' : (type == DataFormat.tsv ? '\t' : '\0'));
                }

                break;

            default:

                gln.log(Level.ERROR, "Unsupported file type: " + type.name());
                SimpleLogNode.setLevels(gln);
                return null;

        }

        SimpleLogNode.setLevels(gln);

        if (gln.getLevel() == Level.ERROR) {
            return null;
        }

        if (reader != null) {
            PageTabSyntaxParser prs = new PageTabSyntaxParser(tagRslv, parserCfg);

            try {
                doc = prs.parse(reader, gln);
            } catch (ParserException e) {
                gln.log(Level.ERROR, "Parser exception: " + e.getMessage());
                SimpleLogNode.setLevels(gln);
                return null;
            }
        }

        for (SubmissionInfo si : doc.getSubmissions()) {
            Submission s = si.getSubmission();

            s.setId(0);

            cleanId(s.getRootSection());
        }

        return doc;

    }

    private void cleanId(Section sc) {
        sc.setId(0);

        cleanAnnotationId(sc);

        if (sc.getSections() != null) {
            for (Section ssc : sc.getSections()) {
                cleanId(ssc);
            }
        }

        if (sc.getFileRefs() != null) {
            for (FileRef fr : sc.getFileRefs()) {
                fr.setId(0);
                cleanAnnotationId(fr);
            }
        }

        if (sc.getLinks() != null) {
            for (Link ln : sc.getLinks()) {
                ln.setId(0);
                cleanAnnotationId(ln);
            }
        }
    }

    private void cleanAnnotationId(Annotated ent) {
        if (ent.getAttributes() != null) {
            for (AbstractAttribute aa : ent.getAttributes()) {
                aa.setId(0);
            }
        }
    }

    private String convertToText(byte[] data, String charset, LogNode ln) {
        Charset cs = null;

        if (charset == null) {
            cs = Charset.forName("utf-8");
            ln.log(Level.WARN, "Charset isn't specified. Assuming default 'utf-8'");
        } else {
            try {
                cs = Charset.forName(charset);
            } catch (Exception e) {
                ln.log(Level.ERROR, "System doen't support charser: '" + charset + "'");
                return null;
            }
        }

        return new String(data, cs);
    }
}
