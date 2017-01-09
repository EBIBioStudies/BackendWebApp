package uk.ac.ebi.biostd.fname;

import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;

public class TestFName
{
 public static void main(String[] args)
 {
  System.out.println(FileNameUtil.encode("aaa"));
  System.out.println(FileNameUtil.encode("abc!cba"));
  System.out.println(FileNameUtil.encode("Привет"));
  
  StringBuilder sb = new StringBuilder();
  
  for( int i=0; i< 256; i++ )
   sb.append((char)i);
  
  System.out.println( FileNameUtil.encode(sb.toString()) );
  
 }
}
