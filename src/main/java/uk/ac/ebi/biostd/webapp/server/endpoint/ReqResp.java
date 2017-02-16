package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.util.StringUtils;

public class ReqResp
{
 public enum Format
 {
  JSON,
  TEXT
 }
 
 public static final String FormatParameter = "format";
 
 private ParameterPool params;
 private Response resp;
 
 private HttpServletRequest srltRequest;
 private HttpServletResponse srltResponse;
 
 
 public ReqResp( HttpServletRequest request, HttpServletResponse response  ) throws IOException
 {
  srltRequest = request;
  srltResponse = response;
  
  boolean jsonReq = false;
  
  String cType = request.getContentType();
  
  if( cType != null )
  {
   int pos = cType.indexOf(';');
   
   if( pos > 0 )
    cType = cType.substring(0,pos).trim();

   jsonReq = cType.equalsIgnoreCase("application/json");
  }

  
 
  String prm = request.getParameter(FormatParameter);
  
  boolean json = false;
  
  if( "json".equals(prm) || (! "text".equals(prm) && jsonReq) )
   json = true;

  resp = json? new JSONHttpResponse(response) : new TextHttpResponse(response);
 
  
  if( jsonReq )
  {
   Charset cs = Charset.defaultCharset();
   
   String enc = request.getCharacterEncoding();
   
   if( enc != null )
   {
    try
    {
     cs = Charset.forName(enc);
    }
    catch( Exception e )
    {}
   }
   
   String reqBody = null;
   
  
   reqBody = StringUtils.readFully(request.getInputStream(), cs);
  
   if( reqBody == null || reqBody.length() == 0 )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("text/plain");
    response.getWriter().print("FAIL Empty JSON request body");
    return;
   }
   
   try
   {
    params = new JSONReqParameterPool(reqBody, request.getRemoteAddr());
   }
   catch( Exception e )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid JSON request body");
    return;
   }
  }
  else
   params = new HttpReqParameterPool(request);

 }
 
 public ParameterPool getParameterPool()
 {
  return params;
 }


 public Response getResponse()
 {
  return resp;
 }


 public HttpServletRequest getHttpServletRequest()
 {
  return srltRequest;
 }


 public HttpServletResponse getHttpServletResponse()
 {
  return srltResponse;
 }
}
