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

package uk.ac.ebi.biostd.webapp.server.endpoint.userdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class UserDataServlet extends ServiceServlet {

    public static final String keyParameter = "key";
    public static final String dataParameter = "value";
    public static final String topicParameter = "topic";
    public static final String ctypeParameter = "contentType";
    public static final String opParameter = "op";
    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {
        if (sess == null || sess.isAnonymouns()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("text/plain");
            resp.getWriter().print("FAIL User not logged in");
            return;
        }

        String opstr = req.getParameter(opParameter);

        Op op = null;

        if (opstr == null) {
            String pi = req.getPathInfo();

            if (pi != null && pi.length() > 1) {
                opstr = pi.substring(1);
            }
        }

        if (opstr != null) {
            for (Op o : Op.values()) {
                if (opstr.equalsIgnoreCase(o.name())) {
                    op = o;
                    break;
                }
            }
        }

        if (op == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain");
            resp.getWriter().print("FAIL " + opParameter + " is not defined or has invalid value ( valid: get, set)");
            return;
        }

        String key = req.getParameter(keyParameter);
        String data = req.getParameter(dataParameter);
        String topic = req.getParameter(topicParameter);

        if (op == Op.LISTJSON) {
            List<UserData> list = null;

            if (topic == null) {
                list = BackendConfig.getServiceManager().getUserManager().getAllUserData(sess.getUser());
            } else {
                list = BackendConfig.getServiceManager().getUserManager().getUserDataByTopic(sess.getUser(), topic);
            }

            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            out.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }

                out.append("\n").append(list.get(i).getData());
            }

            out.append("\n]");

            return;
        }

        if (op == Op.LIST) {
//   if( topic == null )
//    topic = "submission";   // This is temporary hack requested by Olga

            List<UserData> list = null;

            if (topic == null) {
                list = BackendConfig.getServiceManager().getUserManager().getAllUserData(sess.getUser());
            } else {
                list = BackendConfig.getServiceManager().getUserManager().getUserDataByTopic(sess.getUser(), topic);
            }

            JSONObject jsn = new JSONObject();

            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            PrintWriter out = resp.getWriter();
            for (int i = 0; i < list.size(); i++) {
                try {
                    jsn.put(list.get(i).getDataKey(), list.get(i).getData());
                } catch (JSONException e) {
                }
            }

            out.append(jsn.toString());

            return;
        }

        if (key == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain");
            resp.getWriter().print("FAIL " + keyParameter + " is not defined");
            return;
        }

        if (op == Op.GET) {
            UserData udata = null;

            udata = BackendConfig.getServiceManager().getUserManager().getUserData(sess.getUser(), key);

            if (udata != null && udata.getData() != null) {
                if (udata.getContentType() != null) {
                    resp.setContentType(udata.getContentType());
                }

                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().append(udata.getData());
            }
        } else if (op == Op.SET) {
            if (data == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().print("FAIL " + dataParameter + " is not defined");
                return;
            }

            UserData udata = new UserData();

            udata.setDataKey(key);
            udata.setData(data);
            udata.setTopic(topic);
            udata.setContentType(req.getParameter(ctypeParameter));
            udata.setUserId(sess.getUser().getId());

            BackendConfig.getServiceManager().getUserManager().storeUserData(udata);
        } else if (op == Op.DEL) {
            UserData udata = new UserData();

            udata.setDataKey(key);
            udata.setData(null);
            udata.setTopic(topic);
            udata.setUserId(sess.getUser().getId());

            BackendConfig.getServiceManager().getUserManager().storeUserData(udata);
        }
    }

    public enum Op {
        GET,
        SET,
        LIST,
        LISTJSON,
        DEL
    }

}
