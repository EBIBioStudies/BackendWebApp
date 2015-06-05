package uk.ac.ebi.biostd.webapp.server.config;

import java.io.File;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;


public class BackendConfig
{
 public static final String SessionCookie = "BIOSTDSESS";   //think about security issues on system that ignore file name cases
 
 public static final String SessionDir = "sessions";

 public static final String SubmissionFilesDir = "Files";
 
 public static final String WorkdirParameter       = "workDir";

 public static final String UserDirParameter       = "userDir";
 public static final String GroupDirParameter      = "groupDir";
 public static final String SubmissionDirParameter = "submissionDir";
 public static final String SubmissionHistoryDirParameter      = "submissionHistoryDir";

 
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;
 
 private static String dataMountPath;
 private static String workDirectory;
 private static String recapchaPrivateKey;


 private static ServiceManager defaultServiceManager;
 private static EntityManagerFactory emf;

 private static File usersDir;
 private static File groupsDir;
 private static File submissionsDir;
 private static File submissionsHistoryDir;

 
 public static boolean readParameter(String param, String val) throws ServiceConfigException
 {
  
  if( WorkdirParameter.equals(param) )
  {
   workDirectory=val;
   
   return true;
  }
  
  if( SubmissionDirParameter.equals(param) )
  {
   submissionsDir = new File( val );

   return true;
  }
  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   submissionsHistoryDir = new File( val );

   return true;
  }

  if( UserDirParameter.equals(param) )
  {
   usersDir = new File( val );

   return true;
  }

  if( GroupDirParameter.equals(param) )
  {
   groupsDir = new File( val );

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
 
 public static void setWorkDirectory( String dir )
 {
  workDirectory=dir;
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


 public static String getDataMountPath()
 {
  return dataMountPath;
 }

 public static File getUsersDir()
 {
  return usersDir;
 }
 
 public static File getUserDir(User user)
 {
  return new File(usersDir,String.valueOf( user.getId() ));
 }
 

 public static File getGroupsDir()
 {
  return groupsDir;
 }
 
 public static File getGroupDir( UserGroup g )
 {
  return new File(groupsDir,String.valueOf( g.getId() ));
 }

 
 public static File getSubmissionsDir()
 {
  return submissionsDir;
 }
 
 public static File getSubmissionDir(Submission sbm)
 {
  return new File(submissionsDir,AccNoUtil.encode( sbm.getAccNo() ) );
 }
 

 public static File getSubmissionFilesDir(Submission sbm)
 {
  return new File( getSubmissionDir(sbm), SubmissionFilesDir );
 }

 public static File getSubmissionsHistoryDir()
 {
  return submissionsHistoryDir;
 }
 
 public static File getSubmissionHistoryDir(Submission sbm)
 {
  return new File(submissionsDir,AccNoUtil.encode( sbm.getAccNo() )+".ver"+( -sbm.getVersion() ) );
 }
 

 public static File getSubmissionHistoryFilesDir(Submission sbm)
 {
  return new File( getSubmissionDir(sbm), SubmissionFilesDir );
 }


 public static String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }





}
