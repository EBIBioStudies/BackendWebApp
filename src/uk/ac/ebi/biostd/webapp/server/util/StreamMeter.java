package uk.ac.ebi.biostd.webapp.server.util;

import java.io.IOException;
import java.io.InputStream;

public class StreamMeter extends InputStream
{
 private InputStream stream;
 private int count=0;
 
 public StreamMeter( InputStream is)
 {
  super();
  stream = is;
 }

 @Override
 public int read() throws IOException
 {
  int ch = stream.read();
  
  if( ch != -1 )
   count++;
  
  return ch;
 }
 
 @Override
 public int read(byte b[], int off, int len) throws IOException
 {
  int rd = stream.read(b,off, len);
  count+=rd;
  return rd;
 }
 
 @Override
 public int read(byte b[]) throws IOException
 {
  int rd = stream.read(b);
  count+=rd;
  return rd;
 }
 
 public int getStreamSize()
 {
  return count;
 }
 
 @Override
 public void close() throws IOException
 {
  stream.close();
 }

 @Override
 public int available() throws IOException
 {
  return stream.available();
 }

 @Override
 public boolean equals(Object obj)
 {
  return stream.equals(obj);
 }

 @Override
 public int hashCode()
 {
  return stream.hashCode();
 }

 @Override
 public void mark(int readlimit)
 {
  stream.mark(readlimit);
 }

 @Override
 public boolean markSupported()
 {
  return stream.markSupported();
 }

 @Override
 public void reset() throws IOException
 {
  stream.reset();
 }

 @Override
 public long skip(long n) throws IOException
 {
  return stream.skip(n);
 }

 @Override
 public String toString()
 {
  return stream.toString();
 }
}
