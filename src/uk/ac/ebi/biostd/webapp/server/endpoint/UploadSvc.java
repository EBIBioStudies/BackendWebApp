package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import uk.ac.ebi.biostd.webapp.Constants;
import uk.ac.ebi.biostd.webapp.server.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceRequest;
import uk.ac.ebi.biostd.webapp.server.mng.Session;
import uk.ac.ebi.biostd.webapp.server.util.StreamPump;

public class UploadSvc extends ServiceServlet
{

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException
 {

  String threadName = Thread.currentThread().getName();

  try
  {
   Thread.currentThread().setName( "Service request from "+req.getRemoteAddr() );
   
   if(sess == null)
   {
    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    return;
   }

   if(!req.getMethod().equals("POST"))
   {
    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    return;
   }

   ServiceRequest upReq = new ServiceRequest();

   boolean isMultipart = ServletFileUpload.isMultipartContent(req);

   if(!isMultipart)
   {

    for(Enumeration< ? > pnames = req.getParameterNames(); pnames.hasMoreElements();)
    {
     String pname = (String) pnames.nextElement();

     if(Constants.serviceHandlerParameter.equals(pname))
      upReq.setHandlerName(req.getParameter(pname));
     else
      upReq.addParam(pname, req.getParameter(pname));
    }

    if(upReq.getHandlerName() == null)
     resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    else
     BackendConfig.getDefaultConfiguration().getRemoteRequestManager().processUpload(upReq, resp.getWriter());

    return;
   }

   // Create a new file upload handler
   ServletFileUpload upload = new ServletFileUpload();

   try
   {
    // Parse the request
    FileItemIterator iter = upload.getItemIterator(req);

    while(iter.hasNext())
    {
     FileItemStream item = iter.next();
     String name = item.getFieldName();
     InputStream stream = item.openStream();

     if(item.isFormField())
     {
      if(Constants.serviceHandlerParameter.equals(name))
      {
       try
       {
        upReq.setHandlerName(Streams.asString(stream));
        stream.close();
       }
       catch(Exception e)
       {
       }
      }
      else
      {
       upReq.addParam(name, Streams.asString(stream));
      }

      //     System.out.println("Form field " + name + " with value " + Streams.asString(stream) + " detected.");
     }
     else
     {
      //     System.out.println("File field " + name + " with file name " + item.getName() + " detected.");
      InputStream uploadedStream = item.openStream();

      File tmpf = sess.makeTempFile();

      StreamPump.doPump(uploadedStream, new FileOutputStream(tmpf), true);

      upReq.addFile(name, tmpf);
     }
    }
   
    Thread.currentThread().setName( "Upload ("+upReq.getHandlerName()+") from "+req.getRemoteAddr() );

    BackendConfig.getDefaultConfiguration().getRemoteRequestManager().processUpload(upReq, resp.getWriter());
   }
   catch(Throwable ex)
   {
    ex.printStackTrace();
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    return;
   }

  }
  finally
  {
   Thread.currentThread().setName(threadName);
  }
 }



}
