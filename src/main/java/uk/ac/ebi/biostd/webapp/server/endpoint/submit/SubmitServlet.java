package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.treelog.Log2JSON;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class SubmitServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;


 enum Action
 {
  create,
  update
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
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid path: " + pi);
    return;
   }
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
  
  Charset cs = Charset.defaultCharset();

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
  
  response.setContentType("application/json");
  
  Log2JSON.convert(topLn, response.getWriter());
  
 }
 
}
