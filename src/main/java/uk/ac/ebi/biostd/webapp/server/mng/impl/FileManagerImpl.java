package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.util.FileUtil;
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
  File f = new File( BackendConfig.getSubmissionFilesDir(sbm), name );
  
  if( ! f.exists() || ! f.isFile() )
   return null;
  
  FilePointer fp = new FilePointer();
  
  fp.setFullPath(f.getAbsolutePath());
  
  return fp;
 }

 @Override
 public File createSubmissionDir(Submission submission)
 {
  File sbmFileDir = BackendConfig.getSubmissionFilesDir(submission);
  
  sbmFileDir.mkdirs();
  
  return BackendConfig.getSubmissionDir(submission);
 }

 @Override
 public void copyToSubmissionFilesDir(Submission submission, FilePointer fp) throws IOException
 {
  File outFile = new File( BackendConfig.getSubmissionFilesDir(submission), fp.getRelativePath() );
  outFile.getParentFile().mkdirs();

  if( fp.getArchivePath() == null )
  {
   FileUtil.copyFile(new File(fp.getFullPath()), outFile );
  }
  else
  {
   try ( ZipFile zf = new ZipFile(fp.getArchivePath()) )
   {
    ZipEntry ze = zf.getEntry( fp.getArchiveInternalPath() );
    
    byte[] buf = new byte[16384];
    
    try (
     InputStream fis = zf.getInputStream(ze);
     OutputStream fos = new FileOutputStream(outFile);
    )
    {
    
     int nread;
     while( (nread=fis.read(buf) ) > 0 )
      fos.write(buf, 0, nread);
    }
    
   }
  }
 }

 @Override
 public File createSubmissionDirFile(Submission submission, String srcFileName)
 {
  return new File( BackendConfig.getSubmissionDir(submission), srcFileName );
 }

}
