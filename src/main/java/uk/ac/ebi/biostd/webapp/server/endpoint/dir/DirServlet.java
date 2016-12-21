package uk.ac.ebi.biostd.webapp.server.endpoint.dir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class DirServlet
 */

public class DirServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 private static final String SHOW_ARCHIVE_PARAMETER="showArchive";
 private static final String PATH_PARAMETER="path";
 private static final String DEPTH_PARAMETER="depth";
 
 private static final String USER_VIRT_DIR = "User";
 private static final String GROUP_VIRT_DIR = "Groups";
 
 private static enum DirTarget
 {
  DIR,
  GROUPS,
  ROOT
 }
 
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
  
  if( sess == null || sess.isAnonymouns() )
  {
   resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
   
   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"User not logged in\"\n}");
   return;
  }
  
  String cmd = req.getParameter("command");
  
  if( cmd == null )
   cmd = "dir";
  
  switch(cmd)
  {
   case "dir":
    
    String shwAparm = req.getParameter(SHOW_ARCHIVE_PARAMETER);
    boolean showArch = shwAparm != null && ( "1".equals(shwAparm) || "true".equalsIgnoreCase(shwAparm) || "yes".equalsIgnoreCase(shwAparm) );
    String dpthPrm = req.getParameter(DEPTH_PARAMETER);
    
    int depth = 1;
    
    if( dpthPrm  != null )
    {
     try
     {
      depth = -1;
      depth = Integer.parseInt(dpthPrm);
     }
     catch(Exception e)
     {
     }
     
    }
    
    dirList( req.getParameter(PATH_PARAMETER), depth, showArch, resp, sess );
    
    break;

   case "rename":
    
     rename( req, resp, sess );
   
    break;

   case "delete":
    
    delete( req, resp, sess );
   
   break;

    
   default:
    
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    
    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid command\"\n}");
    return;

  }
  
 
 }

 private void delete(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException
 {
  String from = req.getParameter("file");

  if(from == null)
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'file' parameter is not defined\"\n}");
   return;
  }

  Path udir = BackendConfig.getUserDirPath(sess.getUser());


  int i = 0;
  while(i < from.length() && (from.charAt(i) == '/' || from.charAt(i) == '\\'))
   i++;

  if(i > 0)
   from = from.substring(i);
  
  Path fp = udir.resolve(from).normalize();
  
  if( ! fp.startsWith(udir) )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid 'file' parameter value\"\n}");
   return;
  }
  
  File f = fp.toFile();
  
  if( ! f.exists() )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File not found\"\n}");
   return;
  }
  
  if( ! f.delete() )
  {
   resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File delete failed\"\n}");
   return;
  }
  
  resp.getWriter().print("{\n\"status\": \"OK\",\n\"message\": \"File delete success\"\n}");
 }
 
 private void rename(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException
 {
  String from = req.getParameter("from");

  if(from == null)
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'from' parameter is not defined\"\n}");
   return;
  }

  Path udir = BackendConfig.getUserDirPath(sess.getUser());


  int i = 0;
  while(i < from.length() && (from.charAt(i) == '/' || from.charAt(i) == '\\'))
   i++;

  if(i > 0)
   from = from.substring(i);
  
  Path fp = udir.resolve(from).normalize();
  
  if( ! fp.startsWith(udir) )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid 'from' parameter value\"\n}");
   return;
  }
  
  File f = fp.toFile();
  
  if( ! f.exists() )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File not found\"\n}");
   return;
  }
  
  String to = req.getParameter("to");
  
  if(to == null)
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'to' parameter is not defined \"\n}");
   return;
  }

  i = 0;
  while(i < to.length() && (to.charAt(i) == '/' || to.charAt(i) == '\\'))
   i++;

  if(i > 0)
   to = to.substring(i);
  
  Path toFp = udir.resolve(to).normalize();
  
  if( ! toFp.startsWith(udir) )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid 'to' parameter value\"\n}");
   return;
  }
  
  File toFile = toFp.toFile();
  
  toFile.getParentFile().mkdirs();
  
  if( ! f.renameTo(toFile) )
  {
   resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File rename failed\"\n}");
   return;
  }
  
  resp.getWriter().print("{\n\"status\": \"OK\",\n\"message\": \"File rename success\"\n}");
 }

 private void dirList( String path, int depth, boolean showArch, HttpServletResponse resp, Session sess) throws IOException
 {

  if( depth <= 0 )
  {
   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid depth\"\n}");
   return;
  }
  
  User user = sess.getUser();

  Collection<UserGroup> grps = user.getGroups();

  DirTarget tgt = DirTarget.ROOT;
  
  Path udir = BackendConfig.getUserDirPath(user);
  
  Path dirpath = udir;
  Path relPath = null;
  
  if( path != null )
  {
   path = path.trim();
   
   int i = 0;
   while(i < path.length() && (path.charAt(i) == '/' || path.charAt(i) == '\\'))
    i++;

   if(i > 0)
    path = path.substring(i);
   
   relPath = Paths.get(path).normalize();
   
   String frstComp = relPath.getName(0).toString();
   
   if( ! ( "".equals( frstComp ) || USER_VIRT_DIR.equals( frstComp ) || GROUP_VIRT_DIR.equals( frstComp ) ) )
   {
    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
    return;
   }
   
   if( USER_VIRT_DIR.equals( frstComp ) )
   {
    dirpath = udir.resolve(relPath.subpath(1,relPath.getNameCount()) );
    
    if( ! dirpath.startsWith(udir) )
    {
     resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
     return;
    }

    tgt = DirTarget.DIR;
   }
   else if( GROUP_VIRT_DIR.equals( frstComp ) )
   {
    if( relPath.getNameCount() == 1 )
     tgt = DirTarget.GROUPS;
    else
    {
     String gName = relPath.getName(1).toString();
     
     Path gPath = null;
     
     if( grps != null )
     {
      for( UserGroup g : grps )
      {
       if( g.isProject() && gName.equals( g.getName().toString() ) )
       {
        gPath = BackendConfig.getGroupDirPath(g);
        break;
       }
      }
     }
     
     if( gPath == null )
     {
      resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
      return;
     }

     if( relPath.getNameCount() > 2 )
     {
      dirpath = gPath.resolve(relPath.subpath(2, relPath.getNameCount()));

      if( ! dirpath.startsWith(gPath) )
      {
       resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
       return;
      }
      
     }
     else
      dirpath = gPath;
     
     tgt = DirTarget.DIR;

    }

   }
   
   System.out.println( "Target: "+tgt.name()+" "+dirpath );
   
//   if( dirpath.getName(0) )
   
   if( Files.exists(udir) && ( ! dirpath.startsWith(udir) || ! Files.exists(dirpath) )  )
   {
    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
    return;
   }

  }
  
  PrintWriter out = resp.getWriter();
  
  out.print("{\n\"status\": \"OK\",\n\"files\": [\n");

  boolean first = true;

  if( dirpath == udir  )
  {
   if( Files.exists( dirpath ) )
   {
    out.print("{\n\"name\": \"User\",\n \"type\": \"DIR\",\n\"path\": \"/User\",\n \"files\": ");
    
    listDirectory( new FileNode(dirpath), "/User", showArch, out);
    
    out.print("\n}");
    
    first = false;
   }
  }
  else
  {
   listDirectory( new FileNode(dirpath), "", showArch, out);
  }


  if(grps != null && grps.size() > 0)
  {
   if(!first)
    out.print(",\n");

   out.print("{\n\"name\": \"Groups\",\n \"type\": \"DIR\",\n\"path\": \"/Groups\",\n \"files\": [\n");

   first = true;

   for(UserGroup g : grps)
   {
    Path gdir = BackendConfig.getGroupDirPath(g);

    if( Files.exists( gdir ))
    {
     if(!first)
      out.print(",\n");
     else
      first = false;

     String gname = StringUtils.escapeCStr(g.getName());

     out.print("{\n\"name\": \""); // User\",\n type: \"DIR\",\npath: \"/User\",\n files: ");
     out.print(gname);
     out.print("\",\n \"type\": \"DIR\",\n\"path\": \"/Groups/"); ///User\",\n files: ");
     out.print(gname);
     out.print("\",\n \"files\": ");

     listDirectory( new FileNode(gdir), "/Groups/" + gname, showArch,  out);

     out.print("\n}");
    }

   }

   out.print("\n}");

  }

  out.print("\n]\n}\n");

 }
 
 interface Node
 {
  String getName();
  boolean isDirectory();
  long getSize() throws IOException;
  Collection<Node> getSubnodes() throws IOException;
  Path getFile();
 }
 
 private static class FileNode implements Node
 {
  Path file;
  
  FileNode( Path f )
  {
   file=f;
  }

  @Override
  public String getName()
  {
   return file.getFileName().toString();
  }

  @Override
  public boolean isDirectory()
  {
   return Files.isDirectory( file );
  }

  @Override
  public long getSize() throws IOException
  {
   return Files.size( file );
  }

  @Override
  public Collection<Node> getSubnodes() throws IOException
  {
   
   try( Stream<Path> list = Files.list( file ) )
   {
    ArrayList<Node> sbn = new ArrayList<DirServlet.Node>();
    
    list.forEach( f -> sbn.add( new FileNode(f) ) );

    return sbn;
   }
   
  }

  @Override
  public Path getFile()
  {
   return file;
  }
 }
 
 
 private void listDirectory( Node dir, String path, boolean showArch, Appendable out ) throws IOException
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
   
   out.append("{\n\"name\": \"");
   StringUtils.appendAsCStr(out, fname );
   out.append("\",\n\"path\": \"");
   StringUtils.appendAsCStr(out, npath );
   out.append("\",\n\"size\": ");
   out.append(String.valueOf(f.getSize()));
   out.append(",\n\"type\": \"");
   
   if( f.isDirectory() )
   {
    out.append("DIR\",\n\"files\":");
    
    listDirectory(f, npath, showArch, out);
   }
   else if( f.getFile() != null && fname.length() > 4 && fname.substring(fname.length()-4).equalsIgnoreCase(".zip") )
   {
    out.append("ARCHIVE\"");
    
    if( showArch )
    {
     out.append(",\n\"files\":");
   
     listDirectory( listZipArchive(f.getFile().toFile(), npath), npath, showArch, out);
    }
   }
   else
    out.append("FILE\"");
  
   out.append("\n}");
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
  public Path getFile()
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
  public Path getFile()
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
 
 private ZDir listZipArchive( File f, String npath) throws IOException
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

//    int n = entry.isDirectory()?parts.size()-1 : parts.size();
    
    for(int i = 0; i < parts.size()-1; i++)
    {
     ZDir d = cDir.dirs.get(parts.get(i));

     if(d == null)
     {
      d = new ZDir();

      d.name = parts.get(i);

      cDir.dirs.put(d.name, d);
     }

     cDir = d;
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
