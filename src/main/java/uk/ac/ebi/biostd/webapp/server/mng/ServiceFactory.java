package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.File;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.impl.JPASubmissionManager;
import uk.ac.ebi.biostd.webapp.server.mng.impl.JPAUserManager;
import uk.ac.ebi.biostd.webapp.server.mng.impl.SessionManagerImpl;

public class ServiceFactory
{
 
 public static ServiceManager createService() throws ServiceInitExceprion
 {
  
 
  if( BackendConfig.getWorkDirectory() == null )
   throw new ServiceInitExceprion("Service init error: work directory parameter is not defined");
  
  File wd = new File( BackendConfig.getWorkDirectory() );
  
  if( wd.exists() )
  {
   if( ! wd.isDirectory() )
    throw new ServiceInitExceprion("Service init error: work directory path '"+BackendConfig.getWorkDirectory()+"' should point to directory");
   
   if( ! wd.canWrite() )
    throw new ServiceInitExceprion("Service init error: work directory '"+BackendConfig.getWorkDirectory()+"' is not writable");
  }
  else
  {
   if( ! wd.mkdirs() )
    throw new ServiceInitExceprion("Service init error: can't create work directory '"+BackendConfig.getWorkDirectory()+"'");
  }
  
  File sessDir = new File(wd, ServiceConfig.SessionDir);
  
  if( ! sessDir.exists() && ! sessDir.mkdirs() )
   throw new ServiceInitExceprion("Service init error: can't create session directory '"+BackendConfig.getWorkDirectory()+"/"+BackendConfig.SessionDir+"'");
 
  ServiceManagerImpl svc  = new ServiceManagerImpl();

  
  svc.setUserManager( new JPAUserManager( BackendConfig.getEntityManagerFactory() ) );
  svc.setSessionManager( new SessionManagerImpl(sessDir) );
  svc.setSubmissionManager( new JPASubmissionManager(BackendConfig.getEntityManagerFactory()));
  
  return svc;
  
 }

}
