package uk.ac.ebi.biostd.accpart;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;

public class AccPartMain
{

 public static void main(String[] args)
 {
  String acc;
  
  acc="ABC";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="ABC1";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="ABC01";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="ABC1234";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="ABC1Sfx";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="ABC1234Sfx";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="1234";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  acc="АБВ1234";
  System.out.println( acc+" -> "+ join( AccNoUtil.partition(acc) ) );

  Path pth = FileSystems.getDefault().getPath("c:/dev");
  
  System.out.println( pth.resolve( AccNoUtil.getPartitionedPath("ABC1234Sfx") ) );
  
 }
 
 static String join( String[] parts )
 {
  StringBuilder sb = new StringBuilder();
  
  for( String s : parts )
   sb.append(s).append('/');
  
  sb.setLength(sb.length()-1);
  
  return sb.toString();
 }

}
