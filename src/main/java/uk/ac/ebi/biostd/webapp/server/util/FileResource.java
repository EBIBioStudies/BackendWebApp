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
import java.nio.file.Files;
import java.nio.file.Path;
import uk.ac.ebi.biostd.util.FileUtil;

public class FileResource implements Resource {

    private Path filePath;

    public FileResource(Path pth) {
        filePath = pth;
    }

    @Override
    public boolean isValid() {
        return Files.exists(filePath) && Files.isReadable(filePath);
    }

    @Override
    public String readToString(Charset cs) throws IOException {
        return FileUtil.readFile(filePath.toFile(), cs);
    }

    public Path getPath() {
        return filePath;
    }

    public void setPath(Path filePath) {
        this.filePath = filePath;
    }

}
