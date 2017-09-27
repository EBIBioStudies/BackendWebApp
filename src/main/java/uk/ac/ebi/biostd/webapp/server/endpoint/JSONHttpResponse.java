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

package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.ebi.biostd.webapp.server.endpoint.ReqResp.Format;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class JSONHttpResponse implements Response {

    private HttpServletResponse response;

    public JSONHttpResponse(HttpServletResponse resp) {
        response = resp;
    }

    @Override
    public void respond(int code, String sts, String msg, KV... kvs) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(code);

        PrintWriter out = response.getWriter();

        try {
            JSONObject resp = new JSONObject();

            resp.put("status", sts);

            if (msg != null) {
                resp.put("message", msg);
            }

            for (KV kv : kvs) {
                if (kv.getPrefix() != null) {
                    JSONObject sub = resp.optJSONObject(kv.getPrefix());

                    if (sub == null) {
                        sub = new JSONObject();
                        resp.put(kv.getPrefix(), sub);
                    }

                    sub.accumulate(kv.getKey(), kv.getValue());
                } else {
                    resp.accumulate(kv.getKey(), kv.getValue());
                }
            }

            out.println(resp.toString());
        } catch (JSONException e) {
        }

    }


    @Override
    public void respond(int code, String sts) throws IOException {
        respond(code, sts, null);
    }


    @Override
    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    @Override
    public void respondRedir(int code, String sts, String msg, String url) throws IOException {
        if (url != null) {
            try {
                url += "?msg=" + URLEncoder.encode(msg, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }

            response.setHeader("Location", url);
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        } else {
            respond(code, sts, msg);
        }
    }

    @Override
    public Format getFormat() {
        return Format.JSON;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return response;
    }
}
