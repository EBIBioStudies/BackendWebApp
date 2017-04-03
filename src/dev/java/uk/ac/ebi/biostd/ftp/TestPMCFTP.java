/**

Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Mikhail Gostev <gostev@gmail.com>

**/

package uk.ac.ebi.biostd.ftp;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

public class TestPMCFTP
{

 public static void main(String[] args) throws IOException
 {
  FTPClient ftp = new FTPClient();
  FTPClientConfig config = new FTPClientConfig();
  
  ftp.configure( config );
  
  ftp.connect("labslink.ebi.ac.uk");
  ftp.login("elinks", "8VhrURVH");
  ftp.enterLocalPassiveMode();
//  ftp.enterRemotePassiveMode();

    
  FTPFile[] fls = ftp.listFiles("/zs11dw62");
  
  for( FTPFile n : fls )
   System.out.println(n.toString());

  System.out.println("Names");
  
  String[] nms = ftp.listNames("/zs11dw62");
  
  for( String n : nms )
   System.out.println(n.toString());

  
  Path p = FileSystems.getDefault().getPath("\\zs11dw62\\a\\b\\");
  
  String pathstr = p.toString();
  
  if( ! p.getFileSystem().getSeparator().equals("/") )
   pathstr = FilenameUtils.separatorsToUnix(pathstr);
  
  System.out.println(p.getFileSystem().getSeparator());
  System.out.println(pathstr);
  
  
  for( int i=0; i < p.getNameCount(); i++ )
  {
   Path sp = p.subpath(0, i+1);
   System.out.println(sp);
  }
  
  pathstr = pathstr+"/*";
  
  System.out.println("Deleting "+pathstr);
  
  boolean res = ftp.deleteFile(pathstr);
  
  System.out.println("Deleted: "+res+" "+ftp.getReplyString());
  
  ftp.logout();
  ftp.disconnect();
  
 }

}
