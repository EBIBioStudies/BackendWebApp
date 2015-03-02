package uk.ac.ebi.biostd.webapp.server.endpoint;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONReqParameterPool implements ParameterPool
{
 private JSONObject obj;
 private String cliAddr;
 
 
 public JSONReqParameterPool( String txt, String addr ) throws JSONException
 {
  obj = new JSONObject(txt);
  cliAddr=addr;
 }
 
 
 @Override
 public String getParameter(String pName)
 {
   return obj.optString(pName, null );
 }


 @Override
 public String getClientAddress()
 {
  return cliAddr;
 }

 
 
}
