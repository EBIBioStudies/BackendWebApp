package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.webapp.server.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.Session;

public abstract class ServiceServlet extends HttpServlet
{


 private static final long serialVersionUID = 1L;

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
  String sessID = null;
  
  sessID = req.getParameter(BackendConfig.getDefaultConfiguration().getSessionCookieName());
  
  if( sessID == null )
  {
   Cookie[] cuks = req.getCookies();
   
   if( cuks!= null && cuks.length != 0)
   {
    for (int i = cuks.length - 1; i >= 0; i--)
    {
     if (cuks[i].getName().equals(BackendConfig.getDefaultConfiguration().getSessionCookieName()) )
     {
      sessID = cuks[i].getValue();
      break;
     }
    }
   }
  }
  
  Session sess = null;
  
  if( sessID != null )
   sess = BackendConfig.getDefaultConfiguration().getSessionManager().checkin( sessID );
  
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
  finally
  {
   if( sess != null )
    BackendConfig.getDefaultConfiguration().getSessionManager().checkout();
  }
 }
 
 abstract protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException;

}
