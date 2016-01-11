package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class TextHttpResponse implements Response
{
 private HttpServletResponse response;
 
 public TextHttpResponse( HttpServletResponse resp )
 {
  response = resp;
 }

 @Override
 public void respond(int code, String sts, String msg, KV... kvs) throws IOException
 {
  response.setContentType("text/plain; charset=UTF-8");
  response.setStatus(code);
  
  PrintWriter out = response.getWriter();
  
  out.print(sts);
 
  if( msg != null )
  {
   out.print(" ");
   out.print(msg);
  }
  
  out.print("\n");
  
  for( KV res : kvs )
  {
   out.print(res.getKey());
   out.print(": ");
   out.println(res.getValue());
  }
  
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
