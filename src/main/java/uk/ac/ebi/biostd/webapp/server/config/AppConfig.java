package uk.ac.ebi.biostd.webapp.server.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.mng.ServiceManager;


public class AppConfig
{
 public static final String AppName = "pageant";
 
 public static final String                 DataDirParameter       = AppName + ".dataDir";
 public static final String                 DataMountPathParameter = AppName + ".dataMountPath"; 
 
 private static ServiceManager defaultServiceManager;
 private static Map<String, ServiceManager> servMngrs;
 
 private static Map<String, EntityManagerFactory> factoryMap = new HashMap<String, EntityManagerFactory>();
 
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

 public static void setServiceManager(ServiceManager serviceManager)
 {
  defaultServiceManager = serviceManager;
 }

 public static void setServiceManager(String prof, ServiceManager serviceManager)
 {
  if( servMngrs == null )
   servMngrs = new HashMap<String, ServiceManager>();
  
  servMngrs.put(prof, serviceManager);
 }

 public static void addFactory(String key, EntityManagerFactory emf)
 {
  factoryMap.put(key, emf);
 }
}
