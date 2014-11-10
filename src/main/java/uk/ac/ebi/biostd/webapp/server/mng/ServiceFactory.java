package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.File;

import uk.ac.ebi.biostd.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.config.AppConfig;

public class ServiceFactory
{
 
 public static ServiceManager createService() throws ServiceInitExceprion
 {
  
 
  if( AppConfig.getWorkDirectory() == null )
   throw new ServiceInitExceprion("Service init error: work directory parameter is not defined");
  
  File wd = new File( AppConfig.getWorkDirectory() );
  
  if( wd.exists() )
  {
   if( ! wd.isDirectory() )
    throw new ServiceInitExceprion("Service init error: work directory path '"+AppConfig.getWorkDirectory()+"' should point to directory");
   
   if( ! wd.canWrite() )
    throw new ServiceInitExceprion("Service init error: work directory '"+AppConfig.getWorkDirectory()+"' is not writable");
  }
  else
  {
   if( ! wd.mkdirs() )
    throw new ServiceInitExceprion("Service init error: can't create work directory '"+AppConfig.getWorkDirectory()+"'");
  }
  
  File sessDir = new File(wd, ServiceConfig.SessionDir);
  
  if( ! sessDir.exists() && ! sessDir.mkdirs() )
   throw new ServiceInitExceprion("Service init error: can't create session directory '"+AppConfig.getWorkDirectory()+"/"+AppConfig.SessionDir+"'");
 
  ServiceManagerImpl svc  = new ServiceManagerImpl();

  
  svc.setUserManager( new JPAUserManager( AppConfig.getEntityManagerFactory() ) );
  svc.setSessionManager( new SessionManagerImpl(sessDir) );
  
  return svc;
  
 }

}
