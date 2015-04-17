package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;

public class FileManagerImpl implements FileManager
{

 
 @Override
 public FilePointer checkFileExist(String name, User usr)
 {
  return checkFileExist(name, BackendConfig.getUserDir(usr).toPath());
 }

 @Override
 public FilePointer checkFileExist(String name, Submission sbm)
 {
  File f = new File( BackendConfig.getSubmissionFilesDir(sbm), name );
  
  if( ! f.exists() || ! f.isFile() )
   return null;
  
  FilePointer fp = new FilePointer();
  
  fp.setFullPath(f.toPath());
  
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
  File sbmFile = BackendConfig.getSubmissionFilesDir(submission);
  


  if( fp.getArchivePath() == null )
  {
   File outFile = new File( sbmFile, fp.getRelativePath().toString() );

   if( fp.isDirectory() )
   {
    outFile.mkdirs();
    FileUtil.copyDirectory(fp.getFullPath().toFile(), outFile );
   }
   else
   {
    outFile.getParentFile().mkdirs();
    FileUtil.copyFile(fp.getFullPath().toFile(), outFile );
   }
  }
  else
  {
   try ( ZipFile zf = new ZipFile(fp.getArchivePath().toFile()) )
   {
    if( fp.isDirectory() )
    {
     Enumeration<? extends ZipEntry> eset = zf.entries();
     
     File outDir = new File( sbmFile, fp.getRelativePath().toString() );
     
     while( eset.hasMoreElements() )
     {
      ZipEntry ze  = eset.nextElement();
      
      if( ze.getName().startsWith(fp.getArchiveInternalPath()) && ! ze.isDirectory() )
      {
       File outFile = new File( outDir, ze.getName().substring(fp.getArchiveInternalPath().length()) );
       copyZipEntry(zf, ze, outFile );
      }
     }
    }
    else
    {
     File outFile = new File( sbmFile, fp.getRelativePath().toString() );
     outFile.getParentFile().mkdirs();

     copyZipEntry(zf, zf.getEntry( fp.getArchiveInternalPath() ), outFile);
    }
   }
  }
 }
 
 private void copyZipEntry( ZipFile zf, ZipEntry ze, File outFile ) throws IOException
 {
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
 
 @Override
 public File createSubmissionDirFile(Submission submission, String srcFileName)
 {
  return new File( BackendConfig.getSubmissionDir(submission), srcFileName );
 }

 @Override
 public FilePointer checkFileExist(String name, Path basePath)
 {
  Path relPath = FileSystems.getDefault().getPath(name);
  
  Path rt = relPath.getRoot();
  
  if( rt != null )
   relPath = rt.relativize(relPath);
  
  Path fullPath = basePath.resolve(relPath).normalize();
  
  if( ! fullPath.startsWith(basePath) )
   return null;
  
  File f = fullPath.toFile();
  
  if( f.exists() )
  {
   FilePointer fp = new FilePointer();
   
   fp.setFullPath(fullPath);
   fp.setRelativePath(relPath);
   fp.setDirectory( f.isDirectory() );
   
   return fp;
  }
  
  for( int i=0; i < relPath.getNameCount()-1; i++ )
  {
   String part = relPath.getName(i).toString();
   
   if( part.length() > 4 && part.substring(part.length()-4).equalsIgnoreCase(".zip") )
    return checkZipPath(fullPath, i+1);

  }
  
  return null;
 }

 private FilePointer checkZipPath(Path fullPath, int intStart)
 {
  
  String intFileName = null;
  
  StringBuilder sb = new StringBuilder();

  for( int i=intStart; i < fullPath.getNameCount(); i++ )
   sb.append(fullPath.getName(i).toString()).append('/');
  
  sb.setLength( sb.length()-1);
  
  intFileName = sb.toString();
  
  Path archPath = fullPath.subpath(0, intStart);
  
  try ( ZipFile zf = new ZipFile(archPath.toFile()) )
  {

   Enumeration<? extends ZipEntry> eset = zf.entries();
   
   while( eset.hasMoreElements() )
   {
    ZipEntry ze  = eset.nextElement();
    
    String zeName = ze.getName();
    
    if( zeName.equals(intFileName) || ( zeName.length() == intFileName.length()+1 && zeName.endsWith("/") )  )
    {
     FilePointer fp = new FilePointer();
     
     fp.setArchivePath(archPath);
     fp.setArchiveInternalPath(zeName);
     fp.setFullPath(fullPath);
     fp.setDirectory(ze.isDirectory());
     
     return fp;
    }
   }
   
  }
  catch(Exception e)
  {
  }
  
  
  return null;
 }


 
}
