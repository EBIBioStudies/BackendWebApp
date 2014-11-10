package uk.ac.ebi.biostd.webapp.server.endpoint.dir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.AppConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class DirServlet
 */

public class DirServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 /**
  * @see HttpServlet#HttpServlet()
  */
 public DirServlet()
 {
  super();
  // TODO Auto-generated constructor stub
 }

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  PrintWriter out = resp.getWriter();
  
  if( sess == null )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
   
   resp.getWriter().print("{\nstatus: \"FAIL\",\nmessage: \"User not logged in\"\n}");
   return;
  }
  
  out.print("{\nstatus: \"OK\",\nfiles: [\n");
  
  User user  = sess.getUser();
  
  File udir = AppConfig.getUserDir( user );
  
  if( udir.exists() )
  {
   out.print("{\nname: \"User\",\n type: \"DIR\",\npath: \"/User\",\n files: ");
   
   listDirectory( new FileNode(udir), "/User", out);
  }
  
 }

 interface Node
 {
  String getName();
  boolean isDirectory();
  long getSize();
  Collection<Node> getSubnodes();
  File getFile();
 }
 
 private static class FileNode implements Node
 {
  File file;
  
  FileNode( File f )
  {
   file=f;
  }

  @Override
  public String getName()
  {
   return file.getName();
  }

  @Override
  public boolean isDirectory()
  {
   return file.isDirectory();
  }

  @Override
  public long getSize()
  {
   return file.length();
  }

  @Override
  public Collection<Node> getSubnodes()
  {
   File[] list = file.listFiles();
   
   ArrayList<Node> sbn = new ArrayList<DirServlet.Node>(list.length);
   
   for( File f : list )
    sbn.add( new FileNode(f) );
   
   return sbn;
  }

  @Override
  public File getFile()
  {
   return file;
  }
 }
 
 
 private void listDirectory( Node dir, String path, Appendable out ) throws IOException
 {
  out.append("[");
  
  Collection<Node> list = dir.getSubnodes();
  
  boolean first = true;
  
  for( Node f : list )
  {
   if( ! first )
    out.append(",\n");
   else
   {
    out.append("\n");
    first = false;
   }
   
   String fname = f.getName();
   String npath =  path+"/"+fname;
   
   out.append("{\nname: \"");
   StringUtils.appendAsCStr(out, fname );
   out.append("\",\npath: \"");
   StringUtils.appendAsCStr(out, npath );
   out.append("\",\nsize: ");
   out.append(String.valueOf(f.getSize()));
   out.append(",\ntype: \"");
   
   if( f.isDirectory() )
   {
    out.append("DIR\",\nfiles:");
    
    listDirectory(f, npath, out);
   }
   else if( f.getFile() != null && fname.length() > 4 && fname.substring(fname.length()-1).equalsIgnoreCase(".zip") )
   {
    out.append("ARCHIVE\",\nfiles:");
    
    listDirectory( listZipArchive(f.getFile(), npath), npath, out);
   }
   else
    out.append("FILE\"");
  
   out.append("}");
  }
  
  out.append("\n]\n");

 }

 private static class ZFile implements Node
 {
  long size;
  String name;
  
  @Override
  public String getName()
  {
   return name;
  }
  @Override
  public boolean isDirectory()
  {
   return false;
  }
  @Override
  public long getSize()
  {
   return size;
  }
  @Override
  public Collection<Node> getSubnodes()
  {
   return null;
  }
  @Override
  public File getFile()
  {
   return null;
  }
 }
 
 private static class ZDir implements Node
 {
  String name;
  List<ZFile> files = new ArrayList<>();
  Map<String,ZDir> dirs = new HashMap<>();
  
  @Override
  public String getName()
  {
   return name;
  }
  @Override
  public boolean isDirectory()
  {
   return true;
  }
  @Override
  public long getSize()
  {
   return 0;
  }
  @Override
  public File getFile()
  {
   return null;
  }
  @Override
  public Collection<Node> getSubnodes()
  {
   ArrayList<Node> sbn = new ArrayList<DirServlet.Node>(files.size()+dirs.size());
   
   sbn.addAll( dirs.values() );
   sbn.addAll(files);
   
   return sbn;
  }
 }
 
 private ZDir listZipArchive(File f, String npath) throws IOException
 {
  ZDir root = new ZDir();
  
  try ( ZipFile zipFile = new ZipFile(f) )
  {

   Enumeration< ? extends ZipEntry> entries = zipFile.entries();

   while(entries.hasMoreElements())
   {
    ZipEntry entry = entries.nextElement();

    List<String> parts = StringUtils.splitString(entry.getName(), '/');

    ZDir cDir = root;

    for(int i = 0; i < parts.size() - 1; i++)
    {
     ZDir d = cDir.dirs.get(parts.get(i));

     if(d == null)
     {
      d = new ZDir();

      d.name = parts.get(i);

      cDir.dirs.put(d.name, d);

      cDir = d;
     }
    }

    if(!entry.isDirectory())
    {
     String fName = parts.get(parts.size() - 1);

     ZFile zf = new ZFile();

     zf.name = fName;
     zf.size = entry.getSize();

     cDir.files.add(zf);
    }
   }

  }
  catch(Exception e)
  {
  }

  return root;
 }
 
 
}
