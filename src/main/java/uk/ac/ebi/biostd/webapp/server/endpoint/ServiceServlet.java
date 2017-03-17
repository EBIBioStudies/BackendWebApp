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
  Session sess = null;
  
 
  if( BackendConfig.isConfigValid() )
  {
   String sessID = req.getHeader(BackendConfig.getSessionTokenHeader());
   
   if( sessID == null )
   {
    sessID = req.getParameter(BackendConfig.getSessionCookieName());

    if(sessID == null && !"GET".equalsIgnoreCase(req.getMethod()))
    {

     String qryStr = req.getQueryString();

     if(qryStr != null)
     {
      String[] parts = qryStr.split("&");

      String pfx = BackendConfig.getSessionCookieName() + "=";

      for(String prm : parts)
      {
       if(prm.startsWith(pfx))
       {
        sessID = prm.substring(pfx.length());
        break;
       }
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
      if (cuks[i].getName().equals(BackendConfig.getSessionCookieName()) )
      {
       sessID = cuks[i].getValue();
       break;
      }
     }
    }
   }
   
   if( sessID != null )
    sess = BackendConfig.getServiceManager().getSessionManager().checkin( sessID );
   
   if( sess == null )
    sess = BackendConfig.getServiceManager().getSessionManager().createAnonymousSession();

  }
  else if( ! isIgnoreInvalidConfig() )
  {
   resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Web application is out of service");
   return;
  }


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
 
 protected boolean isIgnoreInvalidConfig()
 {
  return false;
 }

 abstract protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException;

}
