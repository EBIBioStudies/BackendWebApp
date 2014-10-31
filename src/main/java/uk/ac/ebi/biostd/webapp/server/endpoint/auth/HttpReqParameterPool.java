package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import javax.servlet.http.HttpServletRequest;

public class HttpReqParameterPool implements ParameterPool
{
 private HttpServletRequest req;
 private Action defaultAction = null;
 
 
 @Override
 public Action getDefaultAction()
 {
  return defaultAction;
 }


 public HttpReqParameterPool(HttpServletRequest req, Action defAct)
 {
  super();
  this.req = req;
  defaultAction = defAct;
 }


 @Override
 public String getParameter(String pName)
 {
  return req.getParameter(pName);
 }

}
