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

import javax.servlet.http.HttpServletRequest;

public class HttpReqParameterPool implements ParameterPool {

    private HttpServletRequest req;


    public HttpReqParameterPool(HttpServletRequest req) {
        super();
        this.req = req;
    }


    @Override
    public String getParameter(String pName) {
        return req.getParameter(pName);
    }


    @Override
    public String getClientAddress() {
        return req.getRemoteAddr();
    }


    @Override
    public String[] getParameters(String pName) {
        return req.getParameterValues(pName);
    }

}
