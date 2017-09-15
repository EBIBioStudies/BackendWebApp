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

package uk.ac.ebi.biostd.webapp.server.endpoint.subscription;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.TagSubscription;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.HttpReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONHttpResponse;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.Response;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.endpoint.TextHttpResponse;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public class SubscribeServlet extends ServiceServlet {

    public static final String FormatParameter = "format";
    private static final long serialVersionUID = 1L;
    private static final String TagParameter = "tag";
    private static Logger log;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response, Session sess)
            throws ServletException, IOException {
        if (sess == null || sess.isAnonymouns()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain");
            response.getWriter().print("FAIL User not logged in");
            return;
        }

        String pi = request.getPathInfo();

        Operation act = null;

        if (pi != null && pi.length() > 1) {
            pi = pi.substring(1);

            for (Operation op : Operation.values()) {
                if (op.name().equalsIgnoreCase(pi)) {
                    act = op;
                    break;
                }
            }

        }

        if (act == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().print("FAIL Invalid path: " + pi);
            return;
        }

        boolean jsonReq = false;

        String cType = request.getContentType();

        if (cType != null) {
            int pos = cType.indexOf(';');

            if (pos > 0) {
                cType = cType.substring(0, pos).trim();
            }

            jsonReq = cType.equalsIgnoreCase("application/json");
        }

        boolean jsonresp = jsonReq;

        Response resp = null;

        String prm = request.getParameter(FormatParameter);

        if ("json".equals(prm)) {
            resp = new JSONHttpResponse(response);
            jsonresp = true;
        } else if ("text".equals(prm)) {
            resp = new TextHttpResponse(response);
            jsonresp = false;
        } else if (jsonReq) {
            resp = new JSONHttpResponse(response);
        } else {
            resp = new TextHttpResponse(response);
        }

        ParameterPool params = null;

        if (jsonReq) {
            Charset cs = Charset.defaultCharset();

            String enc = request.getCharacterEncoding();

            if (enc != null) {
                try {
                    cs = Charset.forName(enc);
                } catch (Exception e) {
                }
            }

            String reqBody = null;

            reqBody = StringUtils.readFully(request.getInputStream(), cs);

            if (reqBody == null || reqBody.length() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain");
                response.getWriter().print("FAIL Empty JSON request body");
                return;
            }

            try {
                params = new JSONReqParameterPool(reqBody, request.getRemoteAddr());
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("FAIL Invalid JSON request body");
                return;
            }
        } else {
            params = new HttpReqParameterPool(request);
        }

        if (act == Operation.SUBSCRIBE) {
            subscribe(params, resp, sess);
        } else if (act == Operation.UNSUBSCRIBE) {
            unsubscribe(params, resp, sess);
        } else if (act == Operation.LIST) {
            listSubscriptions(response, jsonresp, sess);
        }
    }

    private void listSubscriptions(HttpServletResponse response, boolean json, Session sess) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            Collection<TagSubscription> subss = BackendConfig.getServiceManager().getTagManager()
                    .listSubscriptions(sess.getUser());

            if (json) {
                response.setContentType("application/json; charset=UTF-8");
                out.print("[\n");
            } else {
                response.setContentType("text/plain; charset=UTF-8");
            }

            boolean first = true;

            for (TagSubscription t : subss) {
                if (json) {
                    if (!first) {
                        out.print(",\n");
                    } else {
                        first = false;
                    }

                    out.print("{\n\"tagId\": " + t.getTag().getId() + ",\n");
                    out.print("\"tag\": \"");
                    StringUtils.appendAsJSONStr(out, t.getTag().getName());
                    out.print("\",\n");
                    out.print("\"classifierId\": " + t.getTag().getClassifier().getId() + ",\n");
                    out.print("\"classifier\": \"");
                    StringUtils.appendAsJSONStr(out, t.getTag().getClassifier().getName());
                    out.print("\"");

                    out.print("\n}");
                } else {
                    out.print(t.getTag().getId());
                    out.print(",");
                    out.print(t.getTag().getName());
                    out.print(",");
                    out.print(t.getTag().getClassifier().getId());
                    out.print(",");
                    out.print(t.getTag().getClassifier().getName());

                    out.print("\n");

                }
            }

            if (json) {
                out.print("\n]\n");
            }

        } catch (ServiceException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void subscribe(ParameterPool params, Response resp, Session sess) throws IOException {
        subscribe(params, resp, sess, true);
    }

    private void unsubscribe(ParameterPool params, Response resp, Session sess) throws IOException {
        subscribe(params, resp, sess, false);
    }


    private void subscribe(ParameterPool params, Response resp, Session sess, boolean subsc) throws IOException {
        String tag = params.getParameter(TagParameter);

        if (tag == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" + TagParameter + "' parameter");
            return;
        }

        int pos = tag.indexOf(":");

        if (pos <= 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" + TagParameter + "' parameter value");
            return;
        }

        String classifierName = tag.substring(0, pos);
        String tagName = tag.substring(pos + 1);

        if (classifierName.length() == 0 || tagName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" + TagParameter + "' parameter value");
            return;
        }

        try {
            if (subsc) {
                BackendConfig.getServiceManager().getTagManager()
                        .subscribeUser(tagName, classifierName, sess.getUser());
            } else {
                BackendConfig.getServiceManager().getTagManager()
                        .unsubscribeUser(tagName, classifierName, sess.getUser());
            }

            resp.respond(HttpServletResponse.SC_OK, "OK", "Tag subscribed successfully");
        } catch (ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }
}