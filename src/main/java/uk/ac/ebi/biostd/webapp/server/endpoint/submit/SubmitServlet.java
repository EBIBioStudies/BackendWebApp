package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.treelog.Log2JSON;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class SubmitServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;


 enum Action
 {
  create,
  update,
  delete
 };
 
 
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response, Session sess) throws ServletException, IOException
 {
  if(sess == null)
  {
   response.setStatus(HttpServletResponse.SC_FORBIDDEN);
   response.getWriter().print("FAIL User not logged in");
   return;
  }

  Action act = null;

  String pi = request.getPathInfo();

  if(pi != null && pi.length() > 1)
  {
   try
   {
    act = Action.valueOf(pi.substring(1));
   }
   catch(Throwable e)
   {
   }
  }
  
  if( act == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Invalid path: " + pi);
   return;
  }

  if( act == Action.delete )
  {
   processDelete( request, response, sess);
   return;
  }
  
  boolean jsonReq = request.getContentType() != null && request.getContentType().startsWith("application/json");

  boolean xmlReq = !jsonReq && request.getContentType() != null && request.getContentType().startsWith("text/xml");

  boolean pageTabReq = !jsonReq && !xmlReq && request.getContentType() != null && request.getContentType().startsWith("application/pagetab");
  
  
  if( ! jsonReq && ! xmlReq && ! pageTabReq )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Invalid content type: application/json or text/xml or application/pagetab expected");
   return;
  }
  
  Charset cs = Charset.forName("utf-8");

  String enc = request.getCharacterEncoding();

  if(enc != null)
  {
   try
   {
    cs = Charset.forName(enc);
   }
   catch(Exception e)
   {
   }
  }

  String body = StringUtils.readFully(request.getInputStream(), cs);

  if(body.length() == 0)
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Empty request body");
   return;
  }

//  ErrorCounter ec = new ErrorCounterImpl();
//  SimpleLogNode topLn = new SimpleLogNode(Level.SUCCESS, "Processing '"+act.name()+"' request", ec);
//  
//  topLn.log(Level.INFO, "Body size: "+body.length()+" Type: "+(jsonReq?"JSON":(xmlReq?"XML":"PageTab")));

  LogNode topLn = null;
  
  if( act == Action.create )
  {
   if( jsonReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().createJSONSubmission(body, sess.getUser() );
   else if( xmlReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().createXMLSubmission(body, sess.getUser() );
   else if( pageTabReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().createPageTabSubmission(body, sess.getUser() );
  }
  else if( act == Action.update )
  {
   if( jsonReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().updateJSONSubmission(body, sess.getUser() );
   else if( xmlReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().updateXMLSubmission(body, sess.getUser() );
   else if( pageTabReq )
    topLn = BackendConfig.getServiceManager().getSubmissionManager().updatePageTabSubmission(body, sess.getUser() );
  }
  
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
