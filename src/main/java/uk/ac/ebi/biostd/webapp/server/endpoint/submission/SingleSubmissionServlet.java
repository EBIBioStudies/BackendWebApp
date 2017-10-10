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

package uk.ac.ebi.biostd.webapp.server.endpoint.submission;

import com.pri.util.HttpAccept;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.out.cell.CellFormatter;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.out.pageml.PageMLFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONHttpResponse;
import uk.ac.ebi.biostd.webapp.server.endpoint.Response;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.endpoint.TextHttpResponse;
import uk.ac.ebi.mg.spreadsheet.cell.XSVCellStream;

/**
 * Servlet implementation class SingleSubmissionServlet
 */

public class SingleSubmissionServlet extends ServiceServlet {

    public static final String FormatParameter = "format";
    public static final String CutTechinicalInfoParameter = "notech";
    public static final String AccNoParameter = "accno";
    public static final String DefaultResponseFormat = "xml";
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SingleSubmissionServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        String format = req.getParameter(FormatParameter);

        if (format == null) {
            String accpt = req.getHeader("Accept");

            if (accpt != null) {
                if ("application/json".equalsIgnoreCase(accpt)) {
                    format = "json";
                } else if ("text/xml".equalsIgnoreCase(accpt) || "application/xml".equalsIgnoreCase(accpt)) {
                    format = "xml";
                } else {
                    HttpAccept accp = new HttpAccept(accpt);

                    int mtch = accp
                            .bestMatch(Arrays.asList("text/xml", "application/xml", "application/json"));

                    if (mtch == 0 || mtch == 1) {
                        format = "xml";
                    } else if (mtch == 2) {
                        format = "json";
                    } else {
                        format = DefaultResponseFormat;
                    }
                }
            }
        }

        if (sess == null || sess.isAnonymouns()) {
            getResponse(format, resp).respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
            return;
        }

        String acc = req.getPathInfo();

        if (acc == null) {
            acc = req.getParameter(AccNoParameter);
        }

        if (acc == null || acc.length() < 1) {
            getResponse(format, resp)
                    .respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request. Invalid submission accno");
            return;
        }

        if (acc.charAt(0) == '/') {
            acc = acc.substring(1);
        }

        Submission sub = BackendConfig.getServiceManager().getSubmissionManager().getSubmissionsByAccession(acc);

        if (sub == null) {
            getResponse(format, resp)
                    .respond(HttpServletResponse.SC_NOT_FOUND, "FAIL", "Submission with accno '" + acc + "' not found");
            return;
        }

        if (!BackendConfig.getServiceManager().getSecurityManager().mayUserReadSubmission(sub, sess.getUser())) {
            getResponse(format, resp)
                    .respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not allowed reading this submission");
            return;
        }

        String ctVal = req.getParameter(CutTechinicalInfoParameter);
        boolean cutTech = !("no".equalsIgnoreCase(ctVal) || "false".equalsIgnoreCase(ctVal) || "0".equals(ctVal));

        if (format.equalsIgnoreCase("json")) {
            resp.setContentType("application/json; charset=utf-8");

            new JSONFormatter(out, cutTech).format(sub, out);
        } else if (format.equalsIgnoreCase("csv")) {
            resp.setContentType("text/plain; charset=utf-8");

            CellFormatter jfmt = new CellFormatter(XSVCellStream.getCSVCellStream(out));

            PMDoc doc = new PMDoc();
            doc.addSubmission(new SubmissionInfo(sub));

            jfmt.format(doc);
        } else if (format.equalsIgnoreCase("tsv")) {
            resp.setContentType("text/plain; charset=utf-8");

            CellFormatter jfmt = new CellFormatter(XSVCellStream.getTSVCellStream(out));

            PMDoc doc = new PMDoc();
            doc.addSubmission(new SubmissionInfo(sub));

            jfmt.format(doc);
        } else {
            resp.setContentType("text/xml; charset=utf-8");
            new PageMLFormatter(out, cutTech).format(sub, out);
        }


    }

    private Response getResponse(String fmt, HttpServletResponse response) {
        Response resp = null;

        if ("json".equalsIgnoreCase(fmt)) {
            resp = new JSONHttpResponse(response);
        } else {
            resp = new TextHttpResponse(response);
        }

        return resp;

    }
}
