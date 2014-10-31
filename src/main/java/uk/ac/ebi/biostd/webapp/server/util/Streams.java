package uk.ac.ebi.biostd.webapp.server.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class Streams
{
 
 public static String readFully( InputStream is, Charset cs ) throws IOException
 {
  
  ByteArrayOutputStream os = new ByteArrayOutputStream();
  
  StreamPump.doPump(is, os);
  
  if( cs == null )
   cs=Charset.defaultCharset();
  
  return new String( os.toByteArray(), cs );
 }

}
