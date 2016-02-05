package uk.ac.ebi.biostd.webapp.server.endpoint.reserve;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

public class AccNoReserveServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String prefixParameter = "prefix";
 public static final String suffixParameter = "suffix";
 public static final String countParameter = "count";
 
 public static final int MaxCount = 100; 

 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  if(sess == null || sess.isAnonymouns())
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
   resp.getWriter().print("FAIL User not logged in");
   return;
  }

  
  String countPrm = req.getParameter(countParameter);
  
  if( countPrm == null )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL Parameter '"+countParameter+"' missing");
   return;
  }
  
  int count = -1;
  
  try
  {
   count = Integer.parseInt(countPrm);
  }
  catch(Exception e)
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL Invalid parameter '"+countParameter+"' value. Integer expexted");
   return;
  }
  
  if( count < 1 || count > MaxCount )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL Invalid parameter '"+countParameter+"' value. Should be from 1 to 100 ");
   return;
  }

  String prefix = req.getParameter(prefixParameter);
  String suffix = req.getParameter(suffixParameter);
  
  if( prefix == null && suffix == null )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL Either '"+prefixParameter+"' or '"+suffixParameter+"' or both must be defined");
   return;
  }
  
  long first = -1;
  
  try
  {
   first = BackendConfig.getServiceManager().getAccessionManager().incrementIdGen(prefix, suffix, count, sess.getUser());
  }
  catch(Exception e)
  {
   resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
   resp.getWriter().print("FAIL "+e.getMessage());
   return;
  }
  
  resp.getWriter().print("OK ["+first+","+(first+count-1)+"]");
  
 }

}
