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

package uk.ac.ebi.biostd.webapp.server.endpoint.tags;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import uk.ac.ebi.biostd.authz.Classifier;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.Tag;
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
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;

public class TagsServlet extends ServiceServlet {

    public static final String FormatParameter = "format";
    private static final long serialVersionUID = 1L;
    private static final String TagParameter = "tag";
    private static final String NewNameParameter = "newname";
    private static final String ClassifierParameter = "classifier";
    private static final String DescParameter = "description";
    private static final String ParentParameter = "parent";
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

//  process(act, params, request, response, resp);

        switch (act) {
            case CREATETAG:
                createTag(params, request, resp, sess);
                break;

            case CREATECLASSIFIER:
                createClassifier(params, request, resp, sess);
                break;

            case DELETETAG:
                deleteTag(params, request, resp, sess);
                break;

            case DELETETAGTREE:
                deleteTagTree(params, request, resp, sess);
                break;

            case DELETECLASSIFIER:
                deleteClassifier(params, request, resp, sess);
                break;

            case RENAMECLASSIFIER:
                renameClassifier(params, request, resp, sess);
                break;

            case RENAMETAG:
                renameTag(params, request, resp, sess);
                break;

            case LISTTAGS:
                listTags(request, response, jsonresp, sess);
                break;

            case LISTCLASSIFIERS:
                listClassifiers(request, response, jsonresp, sess);
                break;

            default:
                break;
        }


    }

    private void listClassifiers(HttpServletRequest request, HttpServletResponse response, boolean json, Session sess)
            throws IOException {
        PrintWriter out = response.getWriter();

        try {
            Collection<Classifier> clsfss = BackendConfig.getServiceManager().getTagManager().listClassifiers();

            if (json) {
                response.setContentType("application/json; charset=UTF-8");
                out.print("[\n");
            } else {
                response.setContentType("text/plain; charset=UTF-8");
            }

            boolean first = true;

            for (Classifier cl : clsfss) {
                if (json) {
                    if (!first) {
                        out.print(",\n");
                    } else {
                        first = false;
                    }

                    out.print("{\n\"id\": " + cl.getId() + ",\n");
                    out.print("\"name\": \"");
                    StringUtils.appendAsJSONStr(out, cl.getName());
                    out.print("\",\n");
                    out.print("\"description\": \"");
                    StringUtils.appendAsJSONStr(out, cl.getDescription());
                    out.print("\"\n}");
                } else {
                    out.print(cl.getId());
                    out.print(",");
                    out.print(cl.getName());
                    out.print(",");
                    out.print(cl.getDescription());
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

    private void listTags(HttpServletRequest request, HttpServletResponse response, boolean json, Session sess)
            throws IOException {
        PrintWriter out = response.getWriter();

        try {
            Collection<Tag> tags = BackendConfig.getServiceManager().getTagManager().listTags();

            if (json) {
                response.setContentType("application/json; charset=UTF-8");
                out.print("[\n");
            } else {
                response.setContentType("text/plain; charset=UTF-8");
            }

            boolean first = true;

            for (Tag t : tags) {
                if (json) {
                    if (!first) {
                        out.print(",\n");
                    } else {
                        first = false;
                    }

                    out.print("{\n\"id\": " + t.getId() + ",\n");
                    out.print("\"name\": \"");
                    StringUtils.appendAsJSONStr(out, t.getName());
                    out.print("\",\n");
                    out.print("\"description\": \"");
                    StringUtils.appendAsJSONStr(out, t.getDescription());
                    out.print("\",\n");
                    out.print("\"classifierId\": " + t.getClassifier().getId() + ",\n");
                    out.print("\"classifier\": \"");
                    StringUtils.appendAsJSONStr(out, t.getClassifier().getName());
                    out.print("\"");

                    if (t.getParentTag() != null) {
                        out.print(",\n\"parentId\": " + t.getParentTag().getId() + ",\n");
                        out.print("\"parent\": \"");
                        StringUtils.appendAsJSONStr(out, t.getParentTag().getName());
                        out.print("\"");
                    }

                    out.print("\n}");
                } else {
                    out.print(t.getId());
                    out.print(",");
                    out.print(t.getName());
                    out.print(",");
                    out.print(t.getDescription());
                    out.print(",");
                    out.print(t.getClassifier().getId());
                    out.print(",");
                    out.print(t.getClassifier().getName());

                    if (t.getParentTag() != null) {
                        out.print(",");
                        out.print(t.getParentTag().getId());
                        out.print(",");
                        out.print(t.getParentTag().getName());
                    }

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

    private void renameTag(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        String classifierName = null;
        String tagName = null;
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

        classifierName = tag.substring(0, pos);
        tagName = tag.substring(pos + 1);

        if (classifierName.length() == 0 || tagName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" + TagParameter + "' parameter value");
            return;
        }

        String newname = params.getParameter(NewNameParameter);
        String description = params.getParameter(DescParameter);

        if ((newname == null || newname.length() == 0) && description == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "At least one of '" + NewNameParameter + "' or '" + DescParameter + "' parameters must be set");
            return;
        }

        try {
            BackendConfig.getServiceManager().getTagManager()
                    .renameTag(tagName, classifierName, newname, description, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Tag renamed successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }

    }

    private void renameClassifier(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        String classifierName = params.getParameter(ClassifierParameter);
        String newname = params.getParameter(NewNameParameter);
        String description = params.getParameter(DescParameter);

        if (classifierName == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" + ClassifierParameter + "' parameter");
            return;
        }

        if (classifierName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "Invalid '" + ClassifierParameter + "' parameter value");
            return;
        }

        if ((newname == null || newname.length() == 0) && description == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "At least one of '" + NewNameParameter + "' or '" + DescParameter + "' parameters must be set");
            return;
        }

        try {
            BackendConfig.getServiceManager().getTagManager()
                    .renameClassifier(classifierName, newname, description, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Classifier renamed successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }

    }

    private void deleteClassifier(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        String classifierName = params.getParameter(ClassifierParameter);

        if (classifierName == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" + ClassifierParameter + "' parameter");
            return;
        }

        if (classifierName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "Invalid '" + ClassifierParameter + "' parameter value");
            return;
        }

        try {
            BackendConfig.getServiceManager().getTagManager().deleteClassifier(classifierName, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Classifier deleted successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }

    }


    private void deleteTag(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        deleteTag(params, request, resp, sess, false);
    }

    private void deleteTagTree(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        deleteTag(params, request, resp, sess, true);
    }

    private void deleteTag(ParameterPool params, HttpServletRequest request, Response resp, Session sess,
            boolean cascade) throws IOException {
        String classifierName = null;
        String tagName = null;
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

        classifierName = tag.substring(0, pos);
        tagName = tag.substring(pos + 1);

        if (classifierName.length() == 0 || tagName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" + TagParameter + "' parameter value");
            return;
        }

        try {
            BackendConfig.getServiceManager().getTagManager()
                    .deleteTag(tagName, classifierName, cascade, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Tag deleted successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

    private void createClassifier(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        String classifierName = params.getParameter(ClassifierParameter);
        String description = params.getParameter(DescParameter);

        if (classifierName == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Missing '" + ClassifierParameter + "' parameter");
            return;
        }

        if (classifierName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "Invalid '" + ClassifierParameter + "' parameter value");
            return;
        }

        try {
            BackendConfig.getServiceManager().getTagManager()
                    .createClassifier(classifierName, description, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Classifier created successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }
    }

    private void createTag(ParameterPool params, HttpServletRequest request, Response resp, Session sess)
            throws IOException {
        String tagName = null;
        String classifierName = null;
        String parentTag = null;
        String description = null;

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

        classifierName = tag.substring(0, pos);
        tagName = tag.substring(pos + 1);

        if (classifierName.length() == 0 || tagName.length() == 0) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '" + TagParameter + "' parameter value");
            return;
        }

        parentTag = params.getParameter(ParentParameter);
        description = params.getParameter(DescParameter);

        try {
            BackendConfig.getServiceManager().getTagManager()
                    .createTag(tagName, description, classifierName, parentTag, sess.getUser());

            resp.respond(HttpServletResponse.SC_OK, "OK", "Tag created successfully");
        } catch (SecurityException | ServiceException e) {
            resp.respond(HttpServletResponse.SC_OK, "FAIL", e.getMessage());
        }

    }


}
