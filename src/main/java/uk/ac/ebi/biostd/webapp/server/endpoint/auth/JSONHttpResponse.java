package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class JSONHttpResponse implements Response
{
 private HttpServletResponse response;
 
 public JSONHttpResponse( HttpServletResponse resp )
 {
  response = resp;
 }

 @Override
 public void respond(int code, String sts, String msg, KV... kvs) throws IOException
 {
  response.setContentType("application/json; charset=UTF-8");
  response.setStatus(code);
  
  PrintWriter out = response.getWriter();
  
  out.print("{\n\"status\": \"");
  out.print(sts);
  out.print("\"");
  
  if( msg != null )
  {
   out.print(",\n\"message\": \"");
   out.print(msg);
   out.print("\"");
  }
  
  
  for( KV res : kvs )
  {
   out.print(",\n\"");
   out.print(res.getKey());
   out.print("\": \"");
   out.print(res.getValue());
   out.print("\"");
  }
 
  out.print("\n}\n");
 
 }
 

 @Override
 public void respond(int code, String sts) throws IOException
 {
  respond(code,sts,null);
 }
 

 @Override
 public void addCookie(Cookie cookie)
 {
  response.addCookie(cookie);
 }
}
