package uk.ac.ebi.biostd.webapp.server.export.formatting;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Collections;

import uk.ac.ebi.biostd.out.TextStreamFormatter;

public class StreamPartitioner implements Appendable
{
 public static final String PartPrefix = "part";
 
 private TextStreamFormatter formatter;
 private Path outPath;
 private String fileName;
 private long chunkSize;
 private boolean unitChunk;
 
 private int chunkCounter;
 private long counter;
 
 private PrintStream cStream;

 public StreamPartitioner( TextStreamFormatter fmt, Path outPath, String fnPfx, String fnSfx, long chSize, boolean unit )
 {
  formatter = fmt;
  
  this.outPath=outPath;
  
  fileName = fnPfx+".part%03d."+fnSfx;
  
  chunkSize = chSize;
  
  unitChunk = unit;
  
  counter = 0;
  chunkCounter = 0;
 }
 
 @Override
 public Appendable append(CharSequence csq) throws IOException
 {
  if( unitChunk )
  {
   if( counter == chunkSize || cStream == null )
    nextChunk();

   counter++;
  }
  else 
  {
   if( counter+csq.length() > chunkSize || cStream == null )
    nextChunk();
   
   counter+=csq.length();
  }
  
  cStream.append(csq);
  
  return this;
 }

 @Override
 public Appendable append(CharSequence csq, int start, int end) throws IOException
 {
  return append(csq.subSequence(start, end));
 }

 @Override
 public Appendable append(char c) throws IOException
 {
  if( ! unitChunk )
  {
   if( counter+1 > chunkSize || cStream == null )
    nextChunk();
   
   counter++;
  }
  
  cStream.append(c);
  
  return this;
 }

 private void nextChunk() throws IOException
 {
  if( cStream != null )
  {
   formatter.footer(cStream);

   cStream.close();
  }
  
  chunkCounter++;
  
  String fn= String.format(fileName, chunkCounter);
  
  try
  {
   cStream = new PrintStream(outPath.resolve(fn).toFile(), "UTF-8" );
  }
  catch(UnsupportedEncodingException e)
  {
  }
  
  formatter.header(Collections.emptyMap(), cStream);
  
  counter = 0;
 }

 public void reset()
 {
  if( cStream != null )
  {
   cStream.close();
   cStream = null;
  }
  
  counter = 0;
  chunkCounter = 0;
 }

 public void close() throws IOException
 {
  if( cStream != null )
  {
   formatter.footer(cStream);

   cStream.close();
   cStream = null;
  }
 }
 

}
