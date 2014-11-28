package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.client.BioStdService;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class AdminServiceGWTServlet extends RemoteServiceServlet implements BioStdService
{

 private static final long serialVersionUID = 1L;

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
  
  String sessID = null;
  
  sessID = req.getParameter(BackendConfig.SessionCookie);
  
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
  

  try
  {
   super.service(req,resp);
  }
  finally
  {
   if( sess != null )
    BackendConfig.getServiceManager().getSessionManager().checkout();
  }
 }
 
 @Override
 public User getCurrentUser()
 {
  return BackendConfig.getServiceManager().getSessionManager().getEffectiveUser();
 }

 @Override
 public User login(String login, String pass)
 {
  User user = BackendConfig.getServiceManager().getUserManager().getUserByName(login);
  
  if( user == null )
   return null;
  
  if( pass == null ||  ! user.checkPassword(pass) )
   return null;

  Session sess = BackendConfig.getServiceManager().getSessionManager().getSessionByUser(user.getLogin());
  
  if( sess == null )
   sess = BackendConfig.getServiceManager().getSessionManager().createSession(user);    
  
  String skey = sess.getSessionKey();
  
  Cookie cke =  new Cookie(BackendConfig.SessionCookie, skey);
  cke.setPath(getServletContext().getContextPath());
  
  getThreadLocalResponse().addCookie( cke );
  
  
  return user;
 }

}
