package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class MapParamPool implements ParamPool
{
 private final Map<String, ? extends Object> map;
 
 public MapParamPool( Map<String, ? extends Object> map )
 {
  this.map = map;
 }
 
 @Override
 public Enumeration<String> getNames()
 {
  return Collections.enumeration(map.keySet());
 }

 @Override
 public String getParameter(String name)
 {
  Object val  = map.get(name);
  
  if( val == null )
   return null;
  
  return val.toString();
 }

}
