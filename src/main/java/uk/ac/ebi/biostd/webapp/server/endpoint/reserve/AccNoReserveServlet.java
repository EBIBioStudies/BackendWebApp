package uk.ac.ebi.biostd.webapp.server.endpoint.reserve;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccNoReserveServlet extends HttpServlet
{
 public static final String prefixParameter = "prefix";
 public static final String suffixParameter = "suffix";
 public static final String countParameter = "count";
 
 public static final int MaxCount = 100; 

 @Override
 protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
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
 }
 
}
