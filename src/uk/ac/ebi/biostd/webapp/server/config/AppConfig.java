package uk.ac.ebi.biostd.webapp.server.config;

import java.util.Map;

import uk.ac.ebi.biostd.mng.ServiceManager;


public class AppConfig
{
 private static ServiceManager defaultServiceManager;
 private static Map<String, ServiceManager> servMngrs;
 
 public static ServiceManager getServiceManager()
 {
  return getServiceManager(null);
 }
 
 public static ServiceManager getServiceManager( String profile )
 {
  
  if( profile == null )
   return defaultServiceManager;
  
  if( servMngrs == null )
   return null;
   
  
  return servMngrs.get( profile );
 }

 
}
