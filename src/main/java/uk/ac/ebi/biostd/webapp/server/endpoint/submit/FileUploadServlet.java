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
  
  if( sess == null )
  {
   respond(HttpServletResponse.SC_FORBIDDEN, "User not logged in", resp);
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
  
  User user  = sess.getUser();
  
  File udir = BackendConfig.getUserDir( user );
  
  int pos = fileName.lastIndexOf('/');
  
  if( pos > 0 )
   fileName = fileName.substring(pos+1);
  
  pos = fileName.lastIndexOf('\\');
  
  if( pos > 0 )
   fileName = fileName.substring(pos+1);

  if( relPath != null )
  {
   int i=0;
   while( i < relPath.length() && ( relPath.charAt(i) == '/' || relPath.charAt(i) == '\\' )  )
    i++;
   
   if( i > 0 )
    relPath = relPath.substring(i);
  }

  
  Path uDirPath = udir.toPath();
  
  Path fPath = uDirPath;
  
  if( relPath != null )
   fPath = fPath.resolve(relPath);
  
  fPath = fPath.resolve(fileName).normalize();
  
  if( ! fPath.startsWith(uDirPath) )
  {
   respond(HttpServletResponse.SC_BAD_REQUEST, "Invalid relative path", resp);
   return;
  }
  
  File outFile = fPath.toFile();
  
  if( outFile.isDirectory() )
  {
   respond(HttpServletResponse.SC_FORBIDDEN, "Output file is directory", resp);
   return;
  }
  
  outFile.getParentFile().mkdirs();
  
  byte[] buf = new byte[1024*4];
  
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
