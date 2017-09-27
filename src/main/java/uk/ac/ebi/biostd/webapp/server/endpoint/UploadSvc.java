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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.util.StreamPump;
import uk.ac.ebi.biostd.webapp.Constants;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceRequest;

public class UploadSvc extends ServiceServlet {

    private static final long serialVersionUID = 1L;

    // @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException {

        String threadName = Thread.currentThread().getName();

        try {
            Thread.currentThread().setName("Service request from " + req.getRemoteAddr());

            if (sess == null) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!req.getMethod().equals("POST")) {
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

            ServiceRequest upReq = new ServiceRequest();

            boolean isMultipart = ServletFileUpload.isMultipartContent(req);

            if (!isMultipart) {

                for (Enumeration<?> pnames = req.getParameterNames(); pnames.hasMoreElements(); ) {
                    String pname = (String) pnames.nextElement();

                    if (Constants.serviceHandlerParameter.equals(pname)) {
                        upReq.setHandlerName(req.getParameter(pname));
                    } else {
                        upReq.addParam(pname, req.getParameter(pname));
                    }
                }

                if (upReq.getHandlerName() == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    BackendConfig.getServiceManager().getRemoteRequestManager().processUpload(upReq, resp.getWriter());
                }

                return;
            }

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload();

            try {
                // Parse the request
                FileItemIterator iter = upload.getItemIterator(req);

                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    String name = item.getFieldName();
                    InputStream stream = item.openStream();

                    if (item.isFormField()) {
                        if (Constants.serviceHandlerParameter.equals(name)) {
                            try {
                                upReq.setHandlerName(Streams.asString(stream));
                                stream.close();
                            } catch (Exception e) {
                            }
                        } else {
                            upReq.addParam(name, Streams.asString(stream));
                        }

                        //     System.out.println("Form field " + name + " with value " + Streams.asString(stream) +
                        // " detected.");
                    } else {
                        //     System.out.println("File field " + name + " with file name " + item.getName() + "
                        // detected.");
                        InputStream uploadedStream = item.openStream();

                        File tmpf = sess.makeTempFile();

                        StreamPump.doPump(uploadedStream, new FileOutputStream(tmpf), true);

                        upReq.addFile(name, tmpf);
                    }
                }

                Thread.currentThread().setName("Upload (" + upReq.getHandlerName() + ") from " + req.getRemoteAddr());

                BackendConfig.getServiceManager().getRemoteRequestManager().processUpload(upReq, resp.getWriter());
            } catch (Throwable ex) {
                ex.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

        } finally {
            Thread.currentThread().setName(threadName);
        }
    }


}
