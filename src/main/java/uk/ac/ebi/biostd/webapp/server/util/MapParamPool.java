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

package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class MapParamPool implements ParamPool {

    private final Map<String, ? extends Object> map;

    public MapParamPool(Map<String, ? extends Object> map) {
        this.map = map;
    }

    @Override
    public Enumeration<String> getNames() {
        return Collections.enumeration(map.keySet());
    }

    @Override
    public String getParameter(String name) {
        Object val = map.get(name);

        if (val == null) {
            return null;
        }

        return val.toString();
    }

}
