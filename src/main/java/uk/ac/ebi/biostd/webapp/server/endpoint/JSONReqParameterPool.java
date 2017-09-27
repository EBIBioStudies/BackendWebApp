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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONReqParameterPool implements ParameterPool {

    private JSONObject obj;
    private String cliAddr;


    public JSONReqParameterPool(String txt, String addr) throws JSONException {
        obj = new JSONObject(txt);
        cliAddr = addr;
    }


    @Override
    public String getParameter(String pName) {
        return obj.optString(pName, null);
    }

    @Override
    public String[] getParameters(String pName) {
        Object vals;
        try {
            vals = obj.get(pName);

            if (vals instanceof String) {
                return new String[]{(String) vals};
            }

            if (vals instanceof JSONArray) {
                String[] strvals = new String[((JSONArray) vals).length()];

                for (int i = 0; i < strvals.length; i++) {
                    strvals[i] = ((JSONArray) vals).optString(i);
                }

                return strvals;
            }

        } catch (JSONException e) {
        }

        return null;
    }


    @Override
    public String getClientAddress() {
        return cliAddr;
    }


}
