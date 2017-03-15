package uk.ac.ebi.biostd.webapp.server.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.ac.ebi.biostd.util.FileUtil;

public class FileResource implements Resource
{
 private Path filePath;

 public FileResource(Path pth )
 {
  filePath = pth;
 }

 @Override
 public boolean isValid()
 {
  return Files.exists(filePath) && Files.isReadable(filePath);
 }

 @Override
 public String readToString(Charset cs) throws IOException
 {
  return FileUtil.readFile(filePath.toFile(), cs);
 }

 public Path getPath()
 {
  return filePath;
 }

 public void setPath(Path filePath)
 {
  this.filePath = filePath;
 }

}
