package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
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
import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;
import uk.ac.ebi.biostd.webapp.server.vfs.InvalidPathException;
import uk.ac.ebi.biostd.webapp.server.vfs.PathInfo;

public class FileManagerImpl implements FileManager
{
 private static Logger log;

 public FileManagerImpl()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }

/* 
 @Override
 public FilePointer checkFileExist(String name, User usr)
 {
  return checkFileExist(name, BackendConfig.getUserDirPath(usr));
 }
*/
 
 @Override
 public FilePointer checkFileExist(String name, PathInfo rootPI, User usr, Submission sbm)
 {
  return checkFileExist(BackendConfig.getSubmissionFilesPath(sbm), rootPI.getRelPath(), rootPI.getGroup()!=null?rootPI.getGroup().getId():0);
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


/*
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
 */
 
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
 public FilePointer checkFileExist(String name, PathInfo rootPI, User user ) throws InvalidPathException
 {
  PathInfo filePi = PathInfo.getPathInfo(name, user);
  
  if( filePi.isAbsolute() )
   return checkFileExist(filePi.getRealBasePath(), filePi.getRelPath(), filePi.getGroup()==null?0:filePi.getGroup().getId());

  return checkFileExist(rootPI.getRealBasePath(), rootPI.getRelPath().resolve(filePi.getRelPath()), filePi.getGroup()==null?0:filePi.getGroup().getId());

 }

/* 
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
  
  return checkFileExist(fullPath,relPath, 0);
 }
*/
 
 private FilePointer checkFileExist(Path basePath, Path relPath, long grpId)
 {
  
  StringBuilder sb = new StringBuilder();
  
  sb.append(BackendConfig.getSubmissionFilesUGPath(grpId));
  
  FilePointer fp = null;
  
  
  int i=0;
  for( ; i < relPath.getNameCount()-1; i++ )
  {
   String part = FileNameUtil.encode( relPath.getName(i).toString() );
   
   sb.append('/').append(part);
   
   
   if( part.length() > 4 && part.substring(part.length()-4).equalsIgnoreCase(".zip")  )
   {
    Path cPath = basePath.resolve(sb.toString());

    if(  ! Files.isDirectory( cPath ) )
    {
     fp = checkZipPath(cPath, relPath.subpath(i+1, relPath.getNameCount()));
     break;
    }
    

   }
  }
  
  while( i < relPath.getNameCount() )
  {
   sb.append('/').append( FileNameUtil.encode( relPath.getName(i).toString() ) );
   i++;
  }

  String realRelPath = sb.toString();
  
  if( fp == null )
  {
   Path finalPath = basePath.resolve(realRelPath);
   
   if( ! Files.exists( finalPath ) )
    return null;
   
   fp = new FilePointer();

   fp.setDirectory( Files.isDirectory(finalPath) );

   try
   {
    fp.setSize( Files.size(finalPath) );
   }
   catch(IOException e)
   {
    e.printStackTrace();
    
    return null;
   }
   
  }
  
  fp.setRealRelativePath(realRelPath);
  fp.setRelativePath(relPath);
  fp.setGroupID(grpId);

  return fp;
 }

 private FilePointer checkZipPath(Path archPath, Path archRelPath)
 {
  
  String intFileName = null;
  
  StringBuilder sb = new StringBuilder();

  for( int i=0; i < archRelPath.getNameCount(); i++ )
   sb.append(archRelPath.getName(i).toString()).append('/');

  sb.setLength( sb.length()-1);
  
  intFileName = sb.toString();
  
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
 public void linkOrCopyFile(Path origFile, Path destFile) throws IOException
 {
  Files.createDirectories( destFile.getParent() );
  
  if( BackendConfig.isLinkingAllowed() )
  {
   try
   {
    Files.createLink(destFile, origFile);
   }
   catch(IOException e)
   {
    Files.copy(origFile, destFile);
   }
  }
  else
   Files.copy(origFile, destFile);
 }
 
 @Override
 public void linkOrCopy(Path dstBase, FilePointer fp) throws IOException
 {
  Path outPath = dstBase.resolve( BackendConfig.getSubmissionFilesUGPath(fp.getGroupID()) );
  outPath = outPath.resolve(fp.getRelativePath());

  if( fp.getArchivePath() == null )
  {

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
