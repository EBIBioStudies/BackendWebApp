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

package uk.ac.ebi.biostd.ftp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import uk.ac.ebi.biostd.webapp.server.export.FSProvider;
import uk.ac.ebi.biostd.webapp.server.export.FTPFSProvider;

public class TestFTPFSProvider {

    public static void main(String[] args) throws IOException {

        FSProvider fsp = new FTPFSProvider("ftp://elinks:8VhrURVH@labslink.ebi.ac.uk");

        Path testDir = FileSystems.getDefault().getPath("/zs11dw62/testdir/aaa");

        fsp.createDirectories(testDir);

        PrintStream ps = fsp.createPrintStream(testDir.resolve("file.txt"), "UTF-8");

        ps.println("Hello world!");

        ps.close();

//  fsp.deleteDirectory(testDir);
        fsp.move(testDir, FileSystems.getDefault().getPath("/zs11dw62/testdir/bbb"));

        fsp.close();

    }
}
