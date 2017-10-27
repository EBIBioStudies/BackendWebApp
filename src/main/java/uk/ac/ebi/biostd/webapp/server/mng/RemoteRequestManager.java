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

package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

public class RemoteRequestManager {

    private final Map<String, RemoteRequestListener> lsnrs = new TreeMap<>();

    public void processUpload(ServiceRequest upReq, PrintWriter printWriter) {
        try {
            if (upReq.getHandlerName() == null) {
                return;
            }

            RemoteRequestListener lsnr = lsnrs.get(upReq.getHandlerName());

            if (lsnr != null) {
                lsnr.processRequest(upReq, printWriter);
            }

        } finally {
            for (File f : upReq.getFiles().values()) {
                if (f.exists()) {
                    f.delete();
                }
            }
        }
    }

    public void addRemoteRequestListener(String cmd, RemoteRequestListener ls) {
        lsnrs.put(cmd, ls);
    }

}
