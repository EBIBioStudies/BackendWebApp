package uk.ac.ebi.biostd.webapp.server.mng;

public class ServiceConfig
{
 private String serviceName;
 
 
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
  // TODO Auto-generated method stub
  return false;
 }
 
 

}
