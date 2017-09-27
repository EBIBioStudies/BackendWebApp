/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute <p> Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at <p> http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * @author Andrew Tikhonov andrew.tikhonov@gmail.com, Mikhail Gostev <gostev@gmail.com>
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
import uk.ac.ebi.biostd.authz.AttributeSubscription;
import uk.ac.ebi.biostd.authz.Session;
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

/**
 * Created by andrew on 26/04/2017.
 */

public class SubscribeAttributeServlet extends ServiceServlet {

    public static final String FormatParameter = "format";
    private static final long serialVersionUID = 1L;
    private static final String AttributeParameter = "attribute";
    private static final String PatternParameter = "pattern";
    private static final String subscriptionIdParameter = "subscriptionId";
    private static Logger log;

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response, Session sess) throws ServletException, IOException {

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
            subscribeToAttribute(params, resp, sess);
        } else if (act == Operation.UNSUBSCRIBE) {
            unsubscribeFromAttribute(params, resp, sess);
        } else if (act == Operation.LIST) {
            listUserSubscriptions(response, jsonresp, sess);
        } else if (act == Operation.TRIGGERATTREVENTS) {
            triggerAttributeEvents(params, resp, sess);
        } else if (act == Operation.TRIGGERTAGEVENTS) {
            triggerTagEvents(params, resp, sess);
        }

    }

    private void listUserSubscriptions(HttpServletResponse response, boolean json, Session session) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            Collection<AttributeSubscription> subscriptionList = BackendConfig.getServiceManager()
                    .getSubscriptionManager().
                            listAttributeSubscriptions(session.getUser());

            if (json) {
                response.setContentType("application/json; charset=UTF-8");
                out.print("[\n");
            } else {
                response.setContentType("text/plain; charset=UTF-8");
            }

            boolean first = true;

            for (AttributeSubscription s : subscriptionList) {
                if (json) {
                    if (!first) {
                        out.print(",\n");
                    } else {
                        first = false;
                    }

                    out.print("{\n\"subscriptionId\": " + s.getId() + ",\n");
                    out.print("\"attribute\": \"");
                    StringUtils.appendAsJSONStr(out, s.getAttribute());
                    out.print("\",\n");
                    out.print("\"pattern\": \"");
                    StringUtils.appendAsJSONStr(out, s.getPattern());
                    out.print("\"");

                    out.print("\n}");
                } else {
                    out.print(s.getId());
                    out.print(",");
                    out.print(s.getAttribute());
                    out.print(",");
                    out.print(s.getPattern());

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

    private void subscribeToAttribute(ParameterPool params,
            Response response, Session session) throws IOException {
        String attribute = params.getParameter(AttributeParameter);

        if (attribute == null) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" +
                    AttributeParameter + "' parameter");
            return;
        }

        if (attribute.length() == 0) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" +
                    AttributeParameter + "' parameter value");
            return;
        }

        String pattern = params.getParameter(PatternParameter);

        if (pattern == null) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" +
                    PatternParameter + "' parameter");
            return;
        }

        if (pattern.length() == 0) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" +
                    PatternParameter + "' parameter value");
            return;
        }

        try {
            BackendConfig.getServiceManager().getSubscriptionManager().addAttributeSubscription(attribute,
                    pattern, session.getUser());

            response.respond(HttpServletResponse.SC_OK, "OK", "Tag subscribed successfully");
        } catch (ServiceException e) {
            response.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

    private void unsubscribeFromAttribute(ParameterPool params,
            Response response, Session session) throws IOException {
        String subscriptionId = params.getParameter(subscriptionIdParameter);

        if (subscriptionId == null) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" +
                    subscriptionIdParameter + "' parameter");
            return;
        }

        if (subscriptionId.length() == 0) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" +
                    subscriptionIdParameter + "' parameter value");
            return;
        }

        long id;
        try {
            id = Long.parseLong(subscriptionId);
        } catch (Exception ex) {
            response.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" +
                    subscriptionIdParameter + "' parameter value");
            return;
        }

        try {
            BackendConfig.getServiceManager().getSubscriptionManager().deleteAttributeSubscription(id);
            response.respond(HttpServletResponse.SC_OK, "OK", "Subscription deleted successfully");
        } catch (ServiceException e) {
            response.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

    private void triggerAttributeEvents(ParameterPool params, Response response,
            Session session) throws IOException {

        try {
            BackendConfig.getServiceManager().getSubscriptionManager().triggerAttributeEventProcessors();
            response.respond(HttpServletResponse.SC_OK, "OK", "Attribute Events Processor Triggered");
        } catch (ServiceException e) {
            response.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

    private void triggerTagEvents(ParameterPool params, Response response,
            Session session) throws IOException {

        try {
            BackendConfig.getServiceManager().getSubscriptionManager().triggerTagEventProcessors();
            response.respond(HttpServletResponse.SC_OK, "OK", "Attribute Events Processor Triggered");
        } catch (ServiceException e) {
            response.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

}