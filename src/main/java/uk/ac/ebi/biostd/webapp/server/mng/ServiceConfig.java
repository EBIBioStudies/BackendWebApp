package uk.ac.ebi.biostd.webapp.server.mng;


public class ServiceConfig
{
 public static final String SessionDir = "sessions";
 
 public static final String DatabaseParameter = "database";
 public static final String WorkdirParameter = "workdir";
 
 private String serviceName;

 
 private String databaseProfile;
 private String workDirectory;



 public ServiceConfig(String svcName)
 {
  serviceName=svcName;
 }

 public String getServiceName()
 {
  return serviceName;
 }


 public boolean readParameter(String param, String val) throws ServiceConfigException
 {
  if( DatabaseParameter.equals(param) )
  {
   databaseProfile=val;
   return true;
  }
  
  if( WorkdirParameter.equals(param) )
  {
   workDirectory=val;
   return true;
  }
  return false;
 }

 public String getDatabaseProfile()
 {
  return databaseProfile;
 }
 
 
 public String getWorkDirectory()
 {
  return workDirectory;
 }

}
