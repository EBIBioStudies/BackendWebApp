package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleParamPool implements ParamPool
{
 private final ResourceBundle rb;
 
 public ResourceBundleParamPool( ResourceBundle rb )
 {
  this.rb = rb;
 }
 
 @Override
 public Enumeration<String> getNames()
 {
  return rb.getKeys();
 }

 @Override
 public String getParameter(String name)
 {
  String val=null;
  
  try
  {
   val=rb.getString(name);
  }
  catch(MissingResourceException ex)
  {}
  
  return val;
 }


}
