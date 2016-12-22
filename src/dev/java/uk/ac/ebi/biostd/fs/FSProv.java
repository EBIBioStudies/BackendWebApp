package uk.ac.ebi.biostd.fs;

import java.nio.file.spi.FileSystemProvider;

public class FSProv
{

 public static void main(String[] args)
 {
  for( FileSystemProvider fsp : FileSystemProvider.installedProviders() )
   System.out.println(fsp.getScheme());

 }

}
