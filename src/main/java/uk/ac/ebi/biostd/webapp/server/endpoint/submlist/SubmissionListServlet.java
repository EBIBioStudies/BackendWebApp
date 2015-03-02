package uk.ac.ebi.biostd.webapp.server.endpoint.submlist;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.HttpReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class DirServlet
 */

public class SubmissionListServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 /**
  * @see HttpServlet#HttpServlet()
  */
 public SubmissionListServlet()
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
  
  
  boolean jsonReq = req.getContentType()!=null && req.getContentType().startsWith("application/json");
  

  ParameterPool params = null;
  
  if( jsonReq )
  {
   Charset cs = Charset.defaultCharset();
   
   String enc = req.getCharacterEncoding();
   
   if( enc != null )
   {
    try
    {
     cs = Charset.forName(enc);
    }
    catch( Exception e )
    {}
   }
   
   String json = StringUtils.readFully(req.getInputStream(), cs);
   
   if( json.length() == 0 )
   {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    out.print("{\n\"status\": \"FAIL\",\n\"message\": \"Empty JSON request body\"\n}");
    return;
   }
   
   try
   {
    params = new JSONReqParameterPool(json, req.getRemoteAddr());
   }
   catch( Exception e )
   {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    out.print("{\n\"status\": \"FAIL\",\n\"message\": \"Empty JSON request body\"\n}");
    return;
   }
  }
  else
   params = new HttpReqParameterPool(req);

  
  int offset = 0;
  
  String val = params.getParameter("offset");
  
  if( val != null )
  {
   try
   {
    offset = Integer.parseInt(val);
   }
   catch(Exception e)
   {
   }
  }
  
  
  int limit = -1;
  
  val = params.getParameter("limit");
  
  if( val != null )
  {
   try
   {
    limit = Integer.parseInt(val);
   }
   catch(Exception e)
   {
   }
  }

 
  Collection<Submission> subs = BackendConfig.getServiceManager().getSubmissionManager().getSubmissionsByOwner( sess.getUser(), offset, limit );

  out.print("{\n\"status\": \"OK\",\n\"submissions\": [\n");
  
  
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
  
  out.print("]\n}\n");
  
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
  out.append("\",\n\"rtime\": \"");
  out.append(String.valueOf(s.getRTime()));
  out.append("\"\n}");

 }


}
