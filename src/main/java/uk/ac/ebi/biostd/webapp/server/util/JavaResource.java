package uk.ac.ebi.biostd.webapp.server.util;

import java.io.IOException;
import java.nio.charset.Charset;

import uk.ac.ebi.biostd.util.FileUtil;

public class JavaResource implements Resource
{
 private String resourcePath;
 
 public JavaResource( String pth )
 {
  resourcePath = pth;
 }

 @Override
 public boolean isValid()
 {
  return Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null;
 }

 @Override
 public String readToString(Charset cs) throws IOException
 {
  return FileUtil.readStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath), cs, 10000);
 }
}
