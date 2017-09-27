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

package uk.ac.ebi.biostd.webapp.server.export;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class FileFSProvider implements FSProvider {

    @Override
    public boolean exists(Path tmpDir) {
        return Files.exists(tmpDir);
    }

    @Override
    public void createDirectories(Path tmpDir) throws IOException {
        Files.createDirectories(tmpDir);
    }

    @Override
    public boolean isWritable(Path path) {
        return Files.isWritable(path);
    }

    @Override
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public PrintStream createPrintStream(Path path, String enc)
            throws UnsupportedEncodingException, FileNotFoundException {
        return new PrintStream(path.toFile(), enc);
    }

    @Override
    public void move(Path fromPath, Path toPath) throws IOException {
        Files.move(fromPath, toPath);
    }

    @Override
    public void copyDirectory(Path fromPath, Path toPath) throws IOException {
        BackendConfig.getServiceManager().getFileManager().copyDirectory(fromPath, toPath);
    }

    @Override
    public void deleteDirectoryContents(Path tmpDir) throws IOException {
        BackendConfig.getServiceManager().getFileManager().deleteDirectoryContents(tmpDir);
    }

    @Override
    public void deleteDirectory(Path dir) throws IOException {
        BackendConfig.getServiceManager().getFileManager().deleteDirectory(dir);
    }

    @Override
    public void close() {
    }

}
