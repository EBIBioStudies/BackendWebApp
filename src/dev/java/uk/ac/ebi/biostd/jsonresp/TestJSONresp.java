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

package uk.ac.ebi.biostd.jsonresp;

import java.io.IOException;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONHttpResponse;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class TestJSONresp {

    public static void main(String[] args) throws IOException {
        JSONHttpResponse resp = new JSONHttpResponse(null);

        resp.respond(200, "OK", "OK msg", new KV("a", "b1"), new KV("aux", "c", "b2"), new KV("f", "d"),
                new KV("aux", "e", "b3"));


    }

}
