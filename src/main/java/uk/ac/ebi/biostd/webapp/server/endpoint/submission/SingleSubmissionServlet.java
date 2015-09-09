package uk.ac.ebi.biostd.webapp.server.endpoint.submission;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class SingleSubmissionServlet
 */

public class SingleSubmissionServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 /**
  * @see HttpServlet#HttpServlet()
  */
 public SingleSubmissionServlet()
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
   
   out.print("{\n\"status\": \"FAIL\",\n\"message\": \"User not logged in\"\n}");
   return;
  }
  
  String acc = req.getPathInfo();
  
  if( acc == null )
   acc = req.getParameter("accno");
  
  if( acc == null || acc.length() < 1 )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   
   out.print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid request. Invalid submission accno\"\n}");
   return;
  }
  
  if( acc.charAt(0) == '/' )
   acc = acc.substring(1);
 
  Submission sub = BackendConfig.getServiceManager().getSubmissionManager().getSubmissionsByAccession(acc);

  if( sub == null )
  {
   resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
   
   out.print("{\n\"status\": \"FAIL\",\n\"message\": \"Submission with accno '"+acc+"' not found\"\n}");
   return;
  }
  
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserReadSubmission(sub, sess.getUser()) )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
   
   out.print("{\n\"status\": \"FAIL\",\n\"message\": \"User not allowed reading this submission\"\n}");
   return;
  }
  
  resp.setContentType("application/json; charset=utf-8");
  
  JSONFormatter jfmt = new JSONFormatter();
  
  jfmt.format(sub, out);
 
 }
}
