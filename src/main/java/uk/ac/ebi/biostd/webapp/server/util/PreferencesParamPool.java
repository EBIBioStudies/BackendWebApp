package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesParamPool implements ParamPool
{
 private final Preferences rb;
 
 public PreferencesParamPool( Preferences rb )
 {
  this.rb = rb;
 }
 
 @Override
 public Enumeration<String> getNames()
 {
  try
  {
   return Collections.enumeration( Arrays.asList( rb.keys() ) );
  }
  catch(BackingStoreException e)
  {
   e.printStackTrace();
  }
  
  return null;
 }

 @Override
 public String getParameter(String name)
 {
  String val=null;
  
  val=rb.get(name, null);
 
  return val;
 }


}
