package uk.ac.ebi.biostd.webapp.server.config;

import java.io.File;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;


public class BackendConfig
{
 public static final String SessionCookie = "BIOSTDSESS";
 
 public static final String SessionDir = "sessions";
 public static final String UsersDir = "Users";
 public static final String GroupsDir = "Groups";
 
 public static final String WorkdirParameter       = "workdir";
 public static final String DataDirParameter       = "datadir";
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;
 
 private static String dataDirectory;
 private static String dataMountPath;
 private static String workDirectory;
 private static String recapchaPrivateKey;


 private static ServiceManager defaultServiceManager;
 private static EntityManagerFactory emf;

 private static File usersDir;
 private static File groupsDir;

 
 public static boolean readParameter(String param, String val) throws ServiceConfigException
 {
  
  if( WorkdirParameter.equals(param) )
  {
   workDirectory=val;
   return true;
  }
  
  if( DataDirParameter.equals(param) )
  {
   dataDirectory=val;
   
   usersDir = new File( dataDirectory, UsersDir );
   groupsDir = new File( dataDirectory, GroupsDir );
   
   return true;
  }

  if( DataMountPathParameter.equals(param) )
  {
   dataMountPath=val;
   return true;
  }
  
  if( RecapchaPrivateKeyParameter.equals(param) )
  {
   recapchaPrivateKey=val;
   return true;
  }

  
  return false;

 }

 
 public static String getWorkDirectory()
 {
  return workDirectory;
 }

 public static ServiceManager getServiceManager()
 {
  return defaultServiceManager;
 }
 
 public static void setServiceManager(ServiceManager serviceManager)
 {
  defaultServiceManager = serviceManager;
 }


 public static void setEntityManagerFactory(EntityManagerFactory e)
 {
  emf=e;
 }
 
 public static EntityManagerFactory getEntityManagerFactory()
 {
  return emf;
 }


 public static String getDataDirectory()
 {
  return dataDirectory;
 }


 public static String getDataMountPath()
 {
  return dataMountPath;
 }


 public static File getUserDir(User user)
 {
  return new File(usersDir,String.valueOf( user.getId() ));
 }

 public static File getGroupDir(UserGroup grp)
 {
  return new File(groupsDir,String.valueOf( grp.getId() ));
 }


 public static String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }

}
