package uk.ac.ebi.biostd.webapp.server.endpoint.attachhost;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.ebi.biostd.model.Submission;

public class JSONRenderer
{
 public static void render( List<Submission> subs, Appendable out ) throws IOException
 {
  JSONObject rtObj = new JSONObject();
  
  try
  {
   rtObj.put("status", "OK");
   
   JSONArray arr = new JSONArray();
   
   rtObj.put("submissions", arr);
   
   for( Submission s : subs )
   {
    JSONObject jssub = new JSONObject();
    
    jssub.put("id", s.getId());
    jssub.put("accno", s.getAccNo());
    jssub.put("title", s.getTitle());
    jssub.put("ctime", s.getCTime());
    jssub.put("mtime", s.getMTime());
    jssub.put("rtime", s.getRTime());
    jssub.put("version", s.getVersion());
    jssub.put("type", s.getRootSection().getType() );
    jssub.put("rstitle", Submission.getNodeTitle(s.getRootSection()));
    
    arr.put(jssub);
   }
   
   out.append( rtObj.toString() );
  }
  catch(JSONException e)
  {
   e.printStackTrace();
  }
 }
}
