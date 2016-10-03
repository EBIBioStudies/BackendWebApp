package uk.ac.ebi.biostd.webapp.server.endpoint;

import javax.servlet.http.HttpServletRequest;

public class HttpReqParameterPool implements ParameterPool
{
 private HttpServletRequest req;
 

 public HttpReqParameterPool(HttpServletRequest req)
 {
  super();
  this.req = req;
 }


 @Override
 public String getParameter(String pName)
 {
  return req.getParameter(pName);
 }


 @Override
 public String getClientAddress()
 {
  return req.getRemoteAddr();
 }


 @Override
 public String[] getParameters(String pName)
 {
  return req.getParameterValues(pName);
 }

}
