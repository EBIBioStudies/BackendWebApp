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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesParamPool implements ParamPool {

    private final Preferences rb;

    public PreferencesParamPool(Preferences rb) {
        this.rb = rb;
    }

    @Override
    public Enumeration<String> getNames() {
        try {
            return Collections.enumeration(Arrays.asList(rb.keys()));
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getParameter(String name) {
        String val = null;

        val = rb.get(name, null);

        return val;
    }


}
