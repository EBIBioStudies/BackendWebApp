package uk.ac.ebi.biostd.webapp.server.endpoint.export;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.endpoint.ReqResp;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class ExportControlServlet extends ServiceServlet
{
 static final long serialVersionUID = 1L;
 
 static final String CommandForceTask = "force";
 static final String CommandInterruptTask = "interrupt";
 static final String CommandLockExport = "lock";
 static final String CommandUnlockExport = "unlock";
 
 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  ReqResp rqrs = new ReqResp(req, resp);

  
  if(sess == null || sess.isAnonymouns() )
  {
   rqrs.getResponse().respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
   return;
  }
  
  
  String cmd = req.getPathInfo();
  
  
 
  if( cmd != null && cmd.charAt(0) == '/' )
   cmd= cmd.substring(1);
  
  if( CommandForceTask.equals(cmd) )
  {
   ECTasks.forceExport(rqrs, sess.getUser());
  }
  else if( CommandInterruptTask.equals(cmd) )
  {
   ECTasks.forceInterrupt(rqrs, sess.getUser());
  }
  else if( CommandLockExport.equals(cmd) )
  {
   ECTasks.lockExport(rqrs, sess.getUser());
  }
  else if( CommandUnlockExport.equals(cmd) )
  {
   ECTasks.unlockExport(rqrs, sess.getUser());
  }
  else if( cmd == null || cmd.length() == 0 )
  {
   ECTasks.reportTaskState(rqrs, sess.getUser());
  }
  else
   rqrs.getResponse().respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid operation" );
   

  
 }
 

}
