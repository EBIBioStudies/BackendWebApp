package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
 
 @Override
 public void respondRedir(int code, String sts, String msg, String url) throws IOException
 {
  if( url != null )
  {
   try
   {
    url+="?msg="+URLEncoder.encode(msg,"UTF-8");
   }
   catch(UnsupportedEncodingException e)
   {
   }
   
   response.setHeader("Location", url);
   response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }
  else
   respond(code, sts, msg);
 }
}
