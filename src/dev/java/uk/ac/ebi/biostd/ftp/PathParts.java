package uk.ac.ebi.biostd.ftp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathParts
{

 public static void main(String[] args)
 {
  Path p = Paths.get("/a/b/c");

  System.out.println("Absolute: "+p.isAbsolute());
  
  System.out.println("Parts: "+p.getNameCount());
  
  for( int i=0 ; i < p.getNameCount(); i++ )
  {
   System.out.println("Part "+i+": "+p.getName(i));
   
  }
  
  
 }

}
