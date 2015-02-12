package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.util.ZipPathCheck;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;

public class FileManagerImpl implements FileManager
{

 @Override
 public FilePointer checkFileExist(String name, User usr)
 {
  File ud = BackendConfig.getUserDir(usr);
  
  ZipPathCheck zch = new ZipPathCheck(ud);
  
  return zch.checkPath(name);
 }

 @Override
 public FilePointer checkFileExist(String name, Submission sbm)
 {
  File f = new File( BackendConfig.getSubmissionDir(sbm), name );
  
  if( ! f.exists() || ! f.isFile() )
   return null;
  
  FilePointer fp = new FilePointer();
  
  fp.setFullPath(f.getAbsolutePath());
  
  return fp;
 }

}
