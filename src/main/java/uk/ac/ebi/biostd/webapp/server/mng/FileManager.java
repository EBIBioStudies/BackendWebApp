package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.File;
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

 File createSubmissionDir(Submission submission);

 void copyToSubmissionFilesDir(Submission submission, FilePointer filePointer) throws IOException;

 File createSubmissionDirFile(Submission submission, String srcFileName);

}
