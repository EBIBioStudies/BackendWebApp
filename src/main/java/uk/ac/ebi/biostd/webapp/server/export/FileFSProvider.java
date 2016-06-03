package uk.ac.ebi.biostd.webapp.server.export;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class FileFSProvider implements FSProvider
{

 @Override
 public boolean exists(Path tmpDir)
 {
  return Files.exists(tmpDir);
 }

 @Override
 public void createDirectories(Path tmpDir) throws IOException
 {
  Files.createDirectories(tmpDir);
 }

 @Override
 public boolean isWritable(Path path)
 {
  return Files.isWritable(path);
 }

 @Override
 public boolean isDirectory(Path path)
 {
  return Files.isDirectory(path);
 }

 @Override
 public PrintStream createPrintStream(Path path, String enc) throws UnsupportedEncodingException, FileNotFoundException
 {
  return new PrintStream(path.toFile(),enc);
 }

 @Override
 public void move(Path fromPath, Path toPath) throws IOException
 {
  Files.move(fromPath,toPath);
 }

 @Override
 public void copyDirectory(Path fromPath, Path toPath) throws IOException
 {
  BackendConfig.getServiceManager().getFileManager().copyDirectory(fromPath,toPath);
 }

 @Override
 public void deleteDirectoryContents(Path tmpDir) throws IOException
 {
  BackendConfig.getServiceManager().getFileManager().deleteDirectoryContents(tmpDir);
 }

 @Override
 public void deleteDirectory(Path dir) throws IOException
 {
  BackendConfig.getServiceManager().getFileManager().deleteDirectory(dir);
 }

 @Override
 public void close()
 {
 }

}
