package uk.ac.ebi.biostd.webapp.server.endpoint.userdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.UserData;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class UserDataServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String keyParameter = "key";
 public static final String dataParameter = "value";
 public static final String opParameter = "op";
 
 public enum Op
 {
  GET,
  SET,
  LIST,
  LISTJSON
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
  
  if( op == Op.LISTJSON )
  {
   List<UserData> list = BackendConfig.getServiceManager().getUserManager().getAllUserData(sess.getUser());
   
   resp.setCharacterEncoding("UTF-8");
   
   PrintWriter out  = resp.getWriter();
   out.append("[");
   for( int i=0; i < list.size(); i++ )
   {
    if( i > 0 )
     out.append(',');
     
    out.append("\n").append(list.get(i).getData());
   }
   
   out.append("\n]");
   
   return;
  }
  
  if( op == Op.LIST )
  {
   List<UserData> list = BackendConfig.getServiceManager().getUserManager().getAllUserData(sess.getUser());
   
   JSONObject jsn = new JSONObject();
   
   resp.setCharacterEncoding("UTF-8");
   
   PrintWriter out  = resp.getWriter();
   for( int i=0; i < list.size(); i++ )
   {
    try
    {
     jsn.put(list.get(i).getDataKey(),list.get(i).getData());
    }
    catch(JSONException e)
    {
    }
   }
   
   out.append(jsn.toString());
   
   return;
  }
  
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
