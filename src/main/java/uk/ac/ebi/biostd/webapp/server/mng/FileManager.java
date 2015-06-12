package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.IOException;
import java.nio.file.Path;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;

public interface FileManager
{

 FilePointer checkFileExist(String name, Path basePath);

 FilePointer checkFileExist(String name, User usr);

 FilePointer checkFileExist(String name, Submission oldSbm);

// File createSubmissionDir(Submission submission);
// File createSubmissionDirFile(Submission submission, String srcFileName);

 void copyToSubmissionFilesDir(Submission submission, FilePointer filePointer) throws IOException;


 void moveToHistory(Submission submission) throws IOException;

 void moveDirectory(Path src, Path dst) throws IOException;

 void copyDirectory(Path src, Path dstp) throws IOException;

 void linkOrCopy(Path origDir, FilePointer filePointer) throws IOException;

 void linkOrCopyDirectory(Path srcDir, Path dstDir) throws IOException;

 void deleteDirectory(Path origDir) throws IOException;

}
