package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;

public class FileManagerImpl implements FileManager
{
 private static Logger log;

 public FileManagerImpl()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }
 
 @Override
 public FilePointer checkFileExist(String name, User usr)
 {
  return checkFileExist(name, BackendConfig.getUserDirPath(usr));
 }

 @Override
 public FilePointer checkFileExist(String name, Submission sbm)
 {
  Path  p = BackendConfig.getSubmissionFilesPath(sbm).resolve(name);
  
  if( ! Files.exists(p) || ! Files.isRegularFile(p) )
   return null;
  
  FilePointer fp = new FilePointer();
  
  Path relPath = FileSystems.getDefault().getPath(name);
  
  Path rt = relPath.getRoot();
  
  if( rt != null )
   relPath = rt.relativize(relPath);
  
  fp.setFullPath(p);
  fp.setRelativePath(relPath);
  
  return fp;
 }

 @Override
 public void moveDirectory( Path src, Path dst ) throws IOException
 {
  Files.createDirectories(dst.getParent());
  Files.move(src, dst);
 }
 
 @Override
 public void moveToHistory( Submission submission ) throws IOException
 {
  Path origDir = BackendConfig.getSubmissionPath(submission);
  Path histDir = BackendConfig.getSubmissionHistoryPath(submission);
  
  if( Files.exists(histDir) )
   throw new IOException("moveToHistory: Destination directory (file) exists: "+histDir);
  
  try
  {
   moveDirectory(origDir, histDir);
   return;
  }
  catch(Exception e )
  {}
  
  try
  {
   copyDirectory( origDir, histDir );
  }
  catch( IOException e )
  {
   try
   {
    deleteDirectory( histDir );
   }
   catch( IOException de )
   {
    log.error("moveToHistory: Rolling back error: "+de.getMessage());
   }
   
   throw e;
  }
  
  deleteDirectory( origDir );
 }
 
 @Override
 public void deleteDirectoryContents(Path origDir) throws IOException
 {
  if( ! Files.exists(origDir) )
   return;

  Files.walkFileTree(origDir, new SimpleFileVisitor<Path>()
  {

   @Override
   public FileVisitResult postVisitDirectory(Path dir, IOException ex ) throws IOException
   {
    if( ex != null )
     throw ex;
    
    if( ! dir.equals(origDir) )
     Files.delete(dir);

    return FileVisitResult.CONTINUE;
   }
   
   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
   {
    Files.delete(file);
    
    return FileVisitResult.CONTINUE;
   }

  });
 }
 
 @Override
 public void deleteDirectory(Path origDir) throws IOException
 {
  if( ! Files.exists(origDir) )
   return;
  
  deleteDirectoryContents(origDir);
  
  Files.delete(origDir);

 }

 @Override
 public void copyDirectory(final Path srcDir, final Path dstDir) throws IOException
 {
  Files.createDirectories(dstDir.getParent());

  Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>()
  {

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
   {
    Path rel = srcDir.relativize(dir);
    
    try
    {
     Files.createDirectory(dstDir.resolve(rel));
    }
    catch( FileAlreadyExistsException e )
    {}
    
    return FileVisitResult.CONTINUE;
   }
   
   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
   {
    Path rel = srcDir.relativize(file);
    
    Files.copy(file, dstDir.resolve(rel) );
    
    return FileVisitResult.CONTINUE;
   }

  });
  
 }
 
 @Override
 public void linkOrCopyDirectory(final Path srcDir, final Path dstDir) throws IOException
 {
  Files.createDirectories(dstDir.getParent());

  Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>()
  {
   boolean tryLink = BackendConfig.isLinkingAllowed();

   @Override
   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
   {
    Path rel = srcDir.relativize(dir);
    
    Files.createDirectory(dstDir.resolve(rel));

    return FileVisitResult.CONTINUE;
   }
   
   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
   {
    Path rel = srcDir.relativize(file);
    Path dst = dstDir.resolve(rel);
    
    if( tryLink )
    {
     try
     {
      Files.createLink(dst, file);
     }
     catch( IOException e )
     {
      Files.copy( file, dst );
      tryLink = false;
     }
    }
    else
     Files.copy( file, dst );
    
    
    return FileVisitResult.CONTINUE;
   }

  });
  
 }

// @Override
// public File createSubmissionDir(Submission submission)
// {
//  File sbmFileDir = BackendConfig.getSubmissionFilesDir(submission);
//  
//  sbmFileDir.mkdirs();
//  
//  return BackendConfig.getSubmissionDir(submission);
// }

 
 @Override
 public void copyToSubmissionFilesDir(Submission submission, FilePointer fp) throws IOException
 {
  Path sbmFile = BackendConfig.getSubmissionFilesPath(submission);
  

  if( fp.getArchivePath() == null )
  {
   Path outFile = sbmFile.resolve( fp.getRelativePath() );

   if( fp.isDirectory() )
   {
    Files.createDirectories(outFile);
    copyDirectory(fp.getFullPath(), outFile);
   }
   else
   {
    Files.createDirectories(outFile.getParent());
    Files.copy(fp.getFullPath(), outFile);
   }
  }
  else
  {
   try ( ZipFile zf = new ZipFile(fp.getArchivePath().toFile()) )
   {
    if( fp.isDirectory() )
    {
     Enumeration<? extends ZipEntry> eset = zf.entries();
     
     Path outDir = sbmFile.resolve( fp.getRelativePath() );
//     File outDir = new File( sbmFile, fp.getRelativePath().toString() );
     
     while( eset.hasMoreElements() )
     {
      ZipEntry ze  = eset.nextElement();
      
      if( ze.getName().startsWith(fp.getArchiveInternalPath()) && ! ze.isDirectory() )
      {
       //File outFile = new File( outDir, ze.getName().substring(fp.getArchiveInternalPath().length()) );
       Path outFile = outDir.resolve(ze.getName().substring(fp.getArchiveInternalPath().length()) );
       copyZipEntry(zf, ze, outFile );
      }
     }
    }
    else
    {
     
     Path outFile = sbmFile.resolve( fp.getRelativePath() );
     Files.createDirectories(outFile.getParent());

     copyZipEntry(zf, zf.getEntry( fp.getArchiveInternalPath() ), outFile);
    }
   }
  }
 }
 
 @SuppressWarnings("unused")
 private void copyZipEntry( ZipFile zf, ZipEntry ze, File outFile ) throws IOException
 {
  byte[] buf = new byte[16384];
  
  outFile.getParentFile().mkdirs();
  
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
 
 private void copyZipEntry( ZipFile zf, ZipEntry ze, Path outFile ) throws IOException
 {
  byte[] buf = new byte[16384];
  
  Files.createDirectories(outFile.getParent());
  

  try (
   InputStream fis = zf.getInputStream(ze);
   OutputStream fos = Files.newOutputStream(outFile); 
  )
  {
  
   int nread;
   while( (nread=fis.read(buf) ) > 0 )
    fos.write(buf, 0, nread);
  }

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
   fp.setSize( f.length() );
   
   return fp;
  }
  
  for( int i=0; i < relPath.getNameCount()-1; i++ )
  {
   String part = relPath.getName(i).toString();
   
   if( part.length() > 4 && part.substring(part.length()-4).equalsIgnoreCase(".zip") )
   {
    FilePointer fp = checkZipPath(fullPath, fullPath.getNameCount()-relPath.getNameCount()+i+1);
    
    fp.setRelativePath(relPath);
    
    return fp;
   }
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
  
  Path archPath = fullPath.getRoot().resolve( fullPath.subpath(0, intStart) );
  
  
  try ( ZipFile zf = new ZipFile(archPath.toFile()) )
  {

   Enumeration<? extends ZipEntry> eset = zf.entries();
   
   while( eset.hasMoreElements() )
   {
    ZipEntry ze  = eset.nextElement();
    
    String zeName = ze.getName();
    
    if( zeName.equals(intFileName) || ( zeName.length() == intFileName.length()+1 && zeName.startsWith(intFileName) && zeName.endsWith("/") )  )
    {
     FilePointer fp = new FilePointer();
     
     fp.setArchivePath(archPath);
     fp.setArchiveInternalPath(zeName);
     fp.setFullPath(fullPath);
     fp.setDirectory(ze.isDirectory());

     fp.setSize( ze.getSize() );
     
     return fp;
    }
   }
   
  }
  catch(Exception e)
  {
  }
  
  
  return null;
 }


 @Override
 public void linkOrCopy(Path origDir, FilePointer fp) throws IOException
 {
  Path outPath = origDir.resolve(fp.getRelativePath());

  if( fp.getArchivePath() == null )
  {
//   File outFile = new File( sbmFile, fp.getRelativePath().toString() );

   if( fp.isDirectory() )
   {
    Files.createDirectories( outPath.getParent() );

    if( BackendConfig.isLinkingAllowed() )
     linkOrCopyDirectory(fp.getFullPath(), outPath);
    else
     copyDirectory(fp.getFullPath(), outPath);
    
   }
   else
   {
    Files.createDirectories(outPath.getParent());

    if( BackendConfig.isLinkingAllowed() )
    {
     try
     {
      Files.createLink(outPath, fp.getFullPath());
     }
     catch(IOException e)
     {
      Files.copy(fp.getFullPath(), outPath);
     }
    }
    else
     Files.copy(fp.getFullPath(), outPath);
     
   }
  }
  else
  {
   try ( ZipFile zf = new ZipFile(fp.getArchivePath().toFile()) )
   {
    if( fp.isDirectory() )
    {
     Enumeration<? extends ZipEntry> eset = zf.entries();
     
     while( eset.hasMoreElements() )
     {
      ZipEntry ze  = eset.nextElement();
      
      if( ze.getName().startsWith(fp.getArchiveInternalPath()) && ! ze.isDirectory() )
      {
       Path outFile = outPath.resolve(ze.getName().substring(fp.getArchiveInternalPath().length()));
       copyZipEntry( zf, ze, outFile );
      }
     }
    }
    else
     copyZipEntry(zf, zf.getEntry( fp.getArchiveInternalPath() ), outPath);
   }
  }
 }


 
}
