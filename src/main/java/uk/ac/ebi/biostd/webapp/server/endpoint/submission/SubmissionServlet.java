package uk.ac.ebi.biostd.webapp.server.endpoint.submission;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class DirServlet
 */

public class SubmissionServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 /**
  * @see HttpServlet#HttpServlet()
  */
 public SubmissionServlet()
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
   
   resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"User not logged in\"\n}");
   return;
  }
  
  out.print("{\n\"status\": \"OK\",\n\"submissions\": [\n");
  
 
  Collection<Submission> subs = BackendConfig.getServiceManager().getSubmissionManager().getSubmissionsByOwner( sess.getUser() );
  
  
  if( subs != null )
  {
   boolean first = true;

   for( Submission s: subs )
   {
    if( first )
     first = false;
    else
     out.print(",\n");
    
    exportSubmission(s, out);
   }
  }
  
 }

 private void exportSubmission(Submission s, Appendable out) throws IOException
 {
  out.append("{\n\"id:\": \"");
  out.append( String.valueOf(s.getId()) );
  out.append("\",\n\"accno\": \"");
  StringUtils.appendAsCStr(out, s.getAccNo() );
  out.append("\",\n\"description\": \"");
  StringUtils.appendAsCStr(out, s.getDescription() );
  out.append("\",\n\"ctime\": \"");
  out.append(String.valueOf(s.getCTime()));
  out.append("\",\n\"mtime\": \"");
  out.append(String.valueOf(s.getMTime()));
  out.append("\"\n}");

 }


}
