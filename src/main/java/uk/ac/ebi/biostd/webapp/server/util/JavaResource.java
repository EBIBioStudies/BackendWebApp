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

import java.io.IOException;
import java.nio.charset.Charset;
import uk.ac.ebi.biostd.util.FileUtil;

public class JavaResource implements Resource {

    private String resourcePath;

    public JavaResource(String pth) {
        resourcePath = pth;
    }

    @Override
    public boolean isValid() {
        return Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null;
    }

    @Override
    public String readToString(Charset cs) throws IOException {
        return FileUtil.readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath), cs,
                10000);
    }
}
