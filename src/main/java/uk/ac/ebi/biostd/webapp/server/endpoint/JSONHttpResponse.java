package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.webapp.shared.util.KV;

import com.pri.util.StringUtils;

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
  StringUtils.appendAsJSONStr(out, sts);
  out.print("\"");
  
  if( msg != null )
  {
   out.print(",\n\"message\": \"");
   StringUtils.appendAsJSONStr(out, msg);
   out.print("\"");
  }
  
  Arrays.sort(kvs, new Comparator<KV>()
  {
   @Override
   public int compare(KV o1, KV o2)
   {
    return o1.getKey().compareTo(o2.getKey());
   }
  });
  
  for(int i=0; i < kvs.length; i++ )
  {
   String ckey = kvs[i].getKey();
   
   if( i == kvs.length-1 || ! ckey.equals(kvs[i+1].getKey()) )
   {
    out.print(",\n\"");
    StringUtils.appendAsJSONStr(out, ckey);
    out.print("\": \"");
    StringUtils.appendAsJSONStr(out, kvs[i].getValue());
    out.print("\"");
   }
   else
   {
    out.print(",\n\"");
    StringUtils.appendAsJSONStr(out, ckey);
    out.print("\": [\n\"");
    StringUtils.appendAsJSONStr(out, kvs[i].getValue());
      
    i++;
    
    while( kvs[i].getKey().equals(ckey) )
    {
     out.print("\",\n\"");
     StringUtils.appendAsJSONStr(out, kvs[i].getValue());
     i++;
    }
    
    i--;
    
    out.print("\"\n]");
   }
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
