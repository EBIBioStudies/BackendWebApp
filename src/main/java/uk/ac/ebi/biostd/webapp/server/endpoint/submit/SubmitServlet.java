package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.treelog.JSON4Log;
import uk.ac.ebi.biostd.treelog.JSON4Report;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager.Operation;

public class SubmitServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String validateOnlyParameter = "validateOnly";
 public static final String idParameter = "id";
 public static final String accnoParameter = "accno";
 public static final String accnoPatternParameter = "accnoPattern";

 
 
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
  
  if( act == Operation.TRANKLUCATE )
  {
   processTranklucate( request, response, sess );
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

  SubmissionReport res = BackendConfig.getServiceManager().getSubmissionManager().createSubmission(data, fmt, request.getCharacterEncoding(), act, sess.getUser(), validateOnly);
  
  LogNode topLn = res.getLog();
  
  SimpleLogNode.setLevels(topLn);

//  if( topLn.getLevel().getPriority() >= Level.ERROR.getPriority() )
//   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
  
  response.setContentType("application/json");
  
  JSON4Report.convert(res, response.getWriter());
  
 }
 
 public void processDelete(HttpServletRequest request, HttpServletResponse response, Session sess) throws IOException
 {
  String sbmAcc = request.getParameter("accno");
  
  if(sbmAcc == null )
   sbmAcc = request.getParameter("id");
  
  if(sbmAcc == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL 'id' parameter is not specified");
   return;
  }
  
  response.setContentType("application/json");
  
  LogNode topLn = BackendConfig.getServiceManager().getSubmissionManager().deleteSubmissionByAccession(sbmAcc, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  JSON4Log.convert(topLn, response.getWriter());

 }

 public void processTranklucate(HttpServletRequest request, HttpServletResponse response, Session sess ) throws IOException
 {
  String sbmID = request.getParameter(idParameter);
  String sbmAcc = request.getParameter(accnoParameter);
  String patAcc = request.getParameter(accnoPatternParameter);
  
  boolean clash=false;
  
  if( patAcc != null )
  {
   if( sbmAcc != null || sbmID != null )
    clash = true;
  }
  else if( sbmAcc != null )
  {
   if( patAcc != null || sbmID != null )
    clash = true;
  }
  else if( sbmID != null )
  {
   if( patAcc != null || sbmAcc != null )
    clash = true;
  }
  else
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL '"+idParameter+"' or '"+accnoParameter+"' or '"+accnoPatternParameter+"' parameter is not specified");
   return;
  }
  
  int id = -1;
  
  if( sbmID != null  )
  {
   try
   {
    id = Integer.parseInt(sbmID);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid '"+idParameter+"' parameter value. Must be integer");
    return;
   }
  }
  
  if( patAcc != null && patAcc.equals("%") )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Invalid '"+accnoPatternParameter+"' parameter value. Can't be single '%'");
   return;
  }
  
  response.setContentType("application/json");
  
  LogNode topLn = null;
  
  if( sbmID != null )
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionById(id, sess.getUser());
  else if( sbmAcc!= null )
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionByAccession(sbmAcc, sess.getUser());
  else
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionByAccessionPattern(sbmAcc, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  JSON4Log.convert(topLn, response.getWriter());

 }
 
}
