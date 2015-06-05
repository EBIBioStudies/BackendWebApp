package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.treelog.Log2JSON;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager.Operation;

public class SubmitServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String validateOnlyParameter = "validateOnly";

 
 
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response, Session sess) throws ServletException, IOException
 {
  if(sess == null)
  {
   response.setStatus(HttpServletResponse.SC_FORBIDDEN);
   response.getWriter().print("FAIL User not logged in");
   return;
  }

  Operation act = null;

  String pi = request.getPathInfo();

  if(pi != null && pi.length() > 1)
  {
   pi=pi.substring(1);
   
   for( Operation op : Operation.values() )
   {
    if( op.name().equalsIgnoreCase(pi) )
    {
     act = op;
     break;
    }
   }
   
  }
  
  if( act == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Invalid path: " + pi);
   return;
  }

  if( act == Operation.DELETE )
  {
   processDelete( request, response, sess );
   return;
  }
  
  String cType = request.getContentType();
  
  if( cType == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL 'Content-type' header missing");
   return;
  }
  
  int pos = cType.indexOf(';');
  
  if( pos > 0 )
   cType = cType.substring(0,pos).trim();
  
  DataFormat fmt = null;
  
  for( DataFormat f : DataFormat.values() )
  {
   if( f.getContentType().equalsIgnoreCase(cType) )
   {
    fmt = f;
    break;
   }
  }
  
  if( fmt == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Content type '"+cType+"' is not supported");
   return;
  }

  byte[] data = IOUtils.toByteArray(request.getInputStream());


  if( data.length == 0 )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Empty request body");
   return;
  }
  
  String vldPrm = request.getParameter(validateOnlyParameter);
  
  boolean validateOnly = vldPrm != null && ("true".equalsIgnoreCase(vldPrm) || "yes".equalsIgnoreCase(vldPrm) || "1".equals(vldPrm) );

  LogNode topLn = BackendConfig.getServiceManager().getSubmissionManager().createSubmission(data, fmt, request.getCharacterEncoding(), act, sess.getUser(), validateOnly);
  
  
  response.setContentType("application/json");
  
  SimpleLogNode.setLevels(topLn);
  Log2JSON.convert(topLn, response.getWriter());
  
 }
 
 public void processDelete(HttpServletRequest request, HttpServletResponse response, Session sess) throws IOException
 {
  String sbmAcc = request.getParameter("id");
  
  if(sbmAcc == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL 'id' parameter is not specified");
   return;
  }
  
  response.setContentType("application/json");
  
  LogNode topLn = BackendConfig.getServiceManager().getSubmissionManager().deleteSubmissionByAccession(sbmAcc, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  Log2JSON.convert(topLn, response.getWriter());

 }

 
}
