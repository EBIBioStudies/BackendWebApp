package uk.ac.ebi.biostd.jsonresp;

import java.io.IOException;

import uk.ac.ebi.biostd.webapp.server.endpoint.JSONHttpResponse;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class TestJSONresp
{

 public static void main(String[] args) throws IOException
 {
  JSONHttpResponse resp = new JSONHttpResponse(null);
  
  resp.respond(200, "OK", "OK msg", new KV("a","b1"),new KV("aux","c","b2"), new KV("f","d"),new KV("aux","e","b3"));

  
 }

}
