package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;
import uk.ac.ebi.biostd.webapp.server.vfs.InvalidPathException;
import uk.ac.ebi.biostd.webapp.server.vfs.PathInfo;
import uk.ac.ebi.biostd.webapp.server.vfs.PathTarget;

/**
 * Servlet implementation class FileUploadServlet
 */

@MultipartConfig
public class FileUploadServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 /**
  * @see HttpServlet#HttpServlet()
  */
 public FileUploadServlet()
 {
  super();
  // TODO Auto-generated constructor stub
 }


 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  if( ! req.getMethod().equalsIgnoreCase("POST") )
  {
   respond(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method "+req.getMethod()+" is not allowed", resp);
   return;
  }
  
  if( sess == null || sess.isAnonymouns() )
  {
   respond(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in", resp);
   return;
  }
  
  String relPath = req.getParameter("relPath");
  String fileName = req.getParameter("fileName");

  Part filePart = req.getPart("file"); // Retrieves <input type="file" name="file">
  
  if( filePart == null )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Can't retrive file body", resp);
   return;
  }
  
  InputStream fileContent = filePart.getInputStream();
  
  if( fileName == null || fileName.trim().length() == 0 )
   fileName = filePart.getSubmittedFileName();
  
  if( fileName != null )
   fileName = fileName.trim();
  
  if( fileName == null || fileName.length() == 0 )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Can't retrive file name", resp);
   return;
  }
  
  
  if( fileName.indexOf('/') >= 0 )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Can't retrive file name", resp);
   return;
  }

  PathInfo pi = null;
  
  if( relPath == null )
   relPath = "";
  
  User user  = sess.getUser();

  try
  {
   pi = PathInfo.getPathInfo(relPath, user);
  }
  catch(InvalidPathException e)
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Invalid path", resp);
   return;
  }

  if( pi.getTarget() == PathTarget.GROUPS || pi.getTarget() == PathTarget.ROOT )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Invalid path", resp);
   return;
  }
  
  if( pi.getTarget() == PathTarget.GROUP || pi.getTarget() == PathTarget.GROUPREL )
  {
   if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserWriteGroupFiles( user, pi.getGroup() )  )
   {
    respond(HttpServletResponse.SC_FORBIDDEN, "User has no permission to write to group's directory", resp);
    return;
   }
  }
  
  Path fPath = pi.getRealBasePath();
  
  Path rlPath = pi.getRelPath();
  
  for(int i=0; i< rlPath.getNameCount(); i++ )
   fPath = fPath.resolve(FileNameUtil.encode( rlPath.getName(i).toString() ));
  
  rlPath = fPath;
  
  fPath = fPath.resolve(FileNameUtil.encode(fileName)).normalize();

  
  if( ! rlPath.startsWith(pi.getRealBasePath()) || ! fPath.startsWith(fPath) )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name", resp);
   return;
  }
  
  File outFile = fPath.toFile();
  
  if( outFile.isDirectory() )
  {
   respond(HttpServletResponse.SC_FORBIDDEN, "Output file is directory", resp);
   return;
  }
  
  outFile.getParentFile().mkdirs();
  
  byte[] buf = new byte[1024*1024];
  
  try(FileOutputStream fos = new FileOutputStream(outFile) )
  {
   int read;
   while( ( read = fileContent.read(buf) ) > 0 )
   {
    fos.write(buf, 0, read);
   }
   
  }
  catch(Exception e)
  {
   respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "File write error: "+e.getMessage(), resp);
   
   outFile.delete();
   
   return;
  }
  
  respond(HttpServletResponse.SC_OK, "Upload successful", resp);
  
 }

 private void respond( int code, String msg, HttpServletResponse resp ) throws IOException
 {
  PrintWriter out = resp.getWriter();
  
  resp.setStatus(code);
  
  out.append("<HTML><BODY onLoad=\"if(typeof(parent.onFileUploaded) == 'function') parent.onFileUploaded(")
  .append(String.valueOf(code) ).append(",'");
  StringUtils.appendAsCStr(out, msg);
  out.append("');\">"); //</BODY></HTML>");
  
  if( code == HttpServletResponse.SC_OK )
   out.append("SUCCESS: ");
  else
   out.append("ERROR ").append(String.valueOf(code)).append(": ");
  
  StringUtils.xmlEscaped(msg, out);
  
  out.append("</BODY></HTML>");
 }
 
}
