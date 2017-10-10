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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ServiceRequest {

    private String command;
    private Map<String, String> params = new TreeMap<>();
    private Map<String, File> files = new HashMap<>();

    public String getHandlerName() {
        return command;
    }

    public void setHandlerName(String command) {
        this.command = command;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public void addFile(String name, File file) {
        files.put(name, file);
    }

    public void clearFiles() {
        files.clear();
    }

    public void addParam(String name, String val) {
        params.put(name, val);
    }
}
