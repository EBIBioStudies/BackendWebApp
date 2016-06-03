package uk.ac.ebi.biostd.ftp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import uk.ac.ebi.biostd.webapp.server.export.FSProvider;
import uk.ac.ebi.biostd.webapp.server.export.FTPFSProvider;

public class TestFTPFSProvider
{
 public static void main(String[] args) throws IOException
 {
  
  FSProvider fsp = new FTPFSProvider("ftp://elinks:8VhrURVH@labslink.ebi.ac.uk");
  
  
  Path testDir = FileSystems.getDefault().getPath("/zs11dw62/testdir/aaa");
  
  fsp.createDirectories(testDir);
  
  PrintStream ps = fsp.createPrintStream(testDir.resolve("file.txt"), "UTF-8");
  
  ps.println("Hello world!");
  
  ps.close();

//  fsp.deleteDirectory(testDir);
  fsp.move(testDir, FileSystems.getDefault().getPath("/zs11dw62/testdir/bbb"));
  
  
  fsp.close();
  
 }
}
