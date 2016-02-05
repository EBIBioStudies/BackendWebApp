package uk.ac.ebi.biostd.webapp.server.endpoint.export;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.export.ExportTask;

public class ExportControlServlet extends ServiceServlet
{
 static final long serialVersionUID = 1L;
 
 static final String CommandForceTask = "force";
 static final String CommandInterruptTask = "interrupt";
 static final String ThreadsParameter = "threads";
 static final int MaxThreads = 32;
 
 private final Logger log = LoggerFactory.getLogger(ExportControlServlet.class);

 
 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  if( ! sess.getUser().isSuperuser() )
  {
   sendMessageNoExp("Only superuser can run this service", resp.getWriter(), "red");
   return;
  }
  
  String cmd = req.getPathInfo();
  
  
  if( BackendConfig.getExportTask() == null )
  {
   sendMessageNoExp("Export task is not configured", resp.getWriter(), "red");
   return;
  }
  
  ExportTask task = BackendConfig.getExportTask().getTask();
  
  if( cmd != null && cmd.charAt(0) == '/' )
   cmd= cmd.substring(1);
  
  if( CommandForceTask.equals(cmd) )
  {
   if( task.isBusy() )
   {
    sendMessageNoExp("Export task is busy", resp.getWriter(), "orange");
    return;
   }

   int threads = task.getTaskConfig().getThreads(-1);
   
   String thrStr = req.getParameter(ThreadsParameter);
   
   if( thrStr != null )
   {
    try
    {
     threads = Integer.parseInt(thrStr);
    }
    catch( Exception e )
    {
     sendMessageNoExp("Invalid parameter value "+ThreadsParameter+"="+thrStr, resp.getWriter(), "red");
     return;
    }
    
    if( threads > MaxThreads )
    {
     sendMessageNoExp("Max threads allowed: "+MaxThreads, resp.getWriter(), "red");
     return;
    }
   }
   
   final int tnum = threads;
   new Thread(new Runnable()
   {

    @Override
    public void run()
    {
     log.info("Starting export task by administrator's requst");

     try
     {
      task.export(task.getTaskConfig().getLimit(-1), tnum);
     }
     catch(Throwable e)
     {
      log.error("Export error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
      e.printStackTrace();
     }

     log.info("Finishing manual task");

    }
   }, "Manual export task").start();
   
   sendMessageNoExp("Export task started", resp.getWriter(), "black");
  }
  else if( CommandInterruptTask.equals(cmd) )
  {


   
   if( ! task.interrupt() )
   {
    sendMessageNoExp("Export task is not busy", resp.getWriter(), "orange");
    return;
   }
   
   sendMessageNoExp("Export task interrupted", resp.getWriter(), "black");
  }
  else if( cmd == null || cmd.length() == 0 )
   sendMessageNoExp("Export task is "+(task.isBusy()?"":"not ")+"busy", resp.getWriter(), "black");
  else
   sendMessageNoExp("Command '"+cmd+"' is not recognized", resp.getWriter(), "black");
   

  
 }
 
 private void sendMessageNoExp(String msg, PrintWriter out, String color)
 {
  try
  {
   sendMessage(msg, out, color);
  }
  catch(IOException e)
  {
   log.error("Can't send info message to the client. "+e.getMessage());
  }

 }

 private void sendMessage(String msg, PrintWriter out, String color) throws IOException
 {
  out.print("<html><body><span");
  
  if( color != null )
   out.print(" style=\"color: "+color+"\"");
  
  out.print(">\n");
  out.print(msg);
  out.print("\n</span></body></html>");
  
 }
}
