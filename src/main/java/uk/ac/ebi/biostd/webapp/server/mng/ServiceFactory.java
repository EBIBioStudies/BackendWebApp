package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.impl.FileManagerImpl;
import uk.ac.ebi.biostd.webapp.server.mng.impl.JPAReleaser;
import uk.ac.ebi.biostd.webapp.server.mng.impl.JPASubmissionManager;
import uk.ac.ebi.biostd.webapp.server.mng.impl.JPAUserManager;
import uk.ac.ebi.biostd.webapp.server.mng.impl.SecurityManagerImpl;
import uk.ac.ebi.biostd.webapp.server.mng.impl.ServiceManagerImpl;
import uk.ac.ebi.biostd.webapp.server.mng.impl.SessionManagerImpl;

public class ServiceFactory
{
 
 public static ServiceManager createService() throws ServiceInitExceprion
 {
  
 
  if( BackendConfig.getWorkDirectory() == null )
   throw new ServiceInitExceprion("Service init error: work directory parameter is not defined");
  
  Path wd  = BackendConfig.getWorkDirectory() ;
  
  if( Files.exists(wd) )
  {
   if( ! Files.isDirectory( wd ) )
    throw new ServiceInitExceprion("Service init error: work directory path '"+BackendConfig.getWorkDirectory()+"' should point to directory");
   
   if( ! Files.isWritable(wd) )
    throw new ServiceInitExceprion("Service init error: work directory '"+BackendConfig.getWorkDirectory()+"' is not writable");
  }
  else
  {
   try
   {
    Files.createDirectories(wd);
   }
   catch(IOException e)
   {
    throw new ServiceInitExceprion("Service init error: can't create work directory '"+wd+"'");
   }
  }
  
  Path sessDir = wd.resolve(ServiceConfig.SessionDir);
  
  if( Files.notExists(sessDir) )
  {
   try
   {
    Files.createDirectories(sessDir);
   }
   catch(IOException e)
   {
    throw new ServiceInitExceprion("Service init error: can't create session directory '"+sessDir+"'");
   }
  }
  
 
  ServiceManagerImpl svc  = new ServiceManagerImpl();

  
  svc.setUserManager( new JPAUserManager( ) );
  svc.setSessionManager( new SessionManagerImpl(sessDir.toFile()) );
  svc.setSubmissionManager( new JPASubmissionManager(BackendConfig.getEntityManagerFactory()));
  svc.setSecurityManager(new SecurityManagerImpl() );
  svc.setFileManager( new FileManagerImpl() );
  svc.setReleaseManager( new JPAReleaser() );
  
  return svc;
  
 }

}
