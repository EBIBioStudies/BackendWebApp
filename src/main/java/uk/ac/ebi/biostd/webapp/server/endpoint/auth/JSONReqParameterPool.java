package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONReqParameterPool implements ParameterPool
{
 private JSONObject obj;
 private Action defaultAction = null;
 private String cliAddr;
 
 
 public JSONReqParameterPool( String txt, Action defAct, String addr ) throws JSONException
 {
  obj = new JSONObject(txt);
  defaultAction = defAct;
  cliAddr=addr;
 }
 
 @Override
 public Action getDefaultAction()
 {
  return defaultAction;
 }
 
 
 @Override
 public String getParameter(String pName)
 {
  try
  { 
   return obj.getString(pName);
  }
  catch( JSONException e )
  {}
  
  return null;
 }


 @Override
 public String getClientAddress()
 {
  return cliAddr;
 }

 
 
}
