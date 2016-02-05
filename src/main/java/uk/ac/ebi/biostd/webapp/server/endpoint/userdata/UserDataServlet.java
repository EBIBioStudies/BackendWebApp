package uk.ac.ebi.biostd.webapp.server.endpoint.userdata;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class UserDataServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String keyParameter = "key";
 public static final String dataParameter = "data";
 public static final String opParameter = "op";
 
 public enum Op
 {
  GET,
  SET
 }

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  if(sess == null || sess.isAnonymouns() )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
   resp.getWriter().print("FAIL User not logged in");
   return;
  }
  
  String opstr = req.getParameter(opParameter);
  
  Op op = null;
  
  if( opstr == null )
  {
   String pi = req.getPathInfo();

   if(pi != null && pi.length() > 1)
    opstr=pi.substring(1);
  }
  
  if( opstr != null )
  {
   for( Op o : Op.values() )
   {
    if( opstr.equalsIgnoreCase(o.name()) )
    {
     op = o;
     break;
    }
   }
  }
 
  if( op == null )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL "+opParameter+" is not defined or has invalid value ( valid: get, set)");
   return;
  }
  
  
  String key = req.getParameter(keyParameter);
  String data = req.getParameter(dataParameter);
  
  if( key == null )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL "+keyParameter+" is not defined");
   return;
  }
  
  
  if( op == Op.GET )
  {
   UserData udata = BackendConfig.getServiceManager().getUserManager().getUserData(sess.getUser(),key);

   if( udata != null && udata.getData() != null )
   {
    resp.setCharacterEncoding("UTF-8");
    resp.getWriter().append(udata.getData());
   }
  }
  else
  {
   UserData udata = new UserData();

   udata.setDataKey(key);
   udata.setData(data);
   udata.setUserId(sess.getUser().getId());
   
   BackendConfig.getServiceManager().getUserManager().storeUserData(udata);
  }
  
  
  
 }

}
