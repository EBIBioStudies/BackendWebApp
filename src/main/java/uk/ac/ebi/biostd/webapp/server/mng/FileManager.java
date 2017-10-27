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

import java.io.IOException;
import java.nio.file.Path;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.webapp.server.vfs.InvalidPathException;
import uk.ac.ebi.biostd.webapp.server.vfs.PathInfo;

public interface FileManager {

    FilePointer checkFileExist(String name, PathInfo rootPi, User user) throws InvalidPathException;

    FilePointer checkFileExist(String name, PathInfo rootPI, User usr, Submission oldSbm) throws InvalidPathException;

    void moveToHistory(Submission submission) throws IOException;

    void moveDirectory(Path src, Path dst) throws IOException;

    long countDirectorySize(Path finalPath) throws IOException;

    void copyDirectory(Path src, Path dstp) throws IOException;

    String linkOrCopy(Path origDir, FilePointer filePointer) throws IOException;

    void linkOrCopyDirectory(Path srcDir, Path dstDir) throws IOException;

    void linkOrCopyFile(Path origFile, Path destFile) throws IOException;

    void deleteDirectory(Path origDir) throws IOException;

    void deleteDirectoryContents(Path origDir) throws IOException;


}
