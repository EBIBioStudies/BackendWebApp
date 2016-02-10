package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public abstract class ServiceServlet extends HttpServlet
{


 private static final long serialVersionUID = 1L;

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
  String sessID = null;
  
  sessID = req.getParameter(BackendConfig.SessionCookie);
  
  if( sessID == null && ! "GET".equalsIgnoreCase(req.getMethod()) )
  {
   
   String qryStr = req.getQueryString();
   
   if( qryStr != null )
   {
    String[] parts = qryStr.split("&");
    
    String pfx = BackendConfig.SessionCookie+"=";
    
    for( String prm : parts )
    {
     if( prm.startsWith(pfx) )
     {
      sessID = prm.substring(pfx.length());
      break;
     }
    }
   }
   
  }
  
  
  if( sessID == null )
  {
   Cookie[] cuks = req.getCookies();
   
   if( cuks!= null && cuks.length != 0)
   {
    for (int i = cuks.length - 1; i >= 0; i--)
    {
     if (cuks[i].getName().equals(BackendConfig.SessionCookie) )
     {
      sessID = cuks[i].getValue();
      break;
     }
    }
   }
  }
  
  Session sess = null;
  
  if( sessID != null )
   sess = BackendConfig.getServiceManager().getSessionManager().checkin( sessID );
  
  if( sess == null )
   sess = BackendConfig.getServiceManager().getSessionManager().createAnonymousSession();
  
//  if (sessID == null)
//  {
//   resp.sendError(HttpServletResponse.SC_FORBIDDEN);
//   return;
//  }
//
//  Session sess = Configuration.getDefaultConfiguration().getSessionManager().checkin( sessID );
//  
//  if (sess == null)
//  {
//   resp.sendError(HttpServletResponse.SC_FORBIDDEN);
//   return;
//  }

  try
  {
   service(req,resp,sess);
  }
  catch( Throwable e )
  {
   e.printStackTrace();
   
   resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
  finally
  {
   if( sess != null )
    BackendConfig.getServiceManager().getSessionManager().checkout();
  }
 }
 
 abstract protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException;

}
