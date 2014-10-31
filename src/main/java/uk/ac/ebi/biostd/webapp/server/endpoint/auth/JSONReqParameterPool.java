package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONReqParameterPool implements ParameterPool
{
 private JSONObject obj;
 private Action defaultAction = null;
 
 
 @Override
 public Action getDefaultAction()
 {
  return defaultAction;
 }
 
 
 public JSONReqParameterPool( String txt, Action defAct ) throws JSONException
 {
  obj = new JSONObject(txt);
  defaultAction = defAct;
 }
 
 @Override
 public String getParameter(String pName)
 {
  return obj.getString(pName);
 }

 
 
}
