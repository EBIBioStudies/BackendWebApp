package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;

public interface FileManager
{

 FilePointer checkFileExist(String name, User usr);

 FilePointer checkFileExist(String name, Submission oldSbm);

}
