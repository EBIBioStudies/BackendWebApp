package uk.ac.ebi.biostd.webapp.server.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;


public class BackendConfig
{
 public static Path getSubmissionsTransactionPath()
 {
  return submissionsTransactionPath;
 }


 public static final String SessionCookie = "BIOSTDSESS";   //think about security issues on system that ignore file name cases
 
 public static final String SessionDir = "sessions";

 public static final String SubmissionHistoryPostfix = "#ver";
 public static final String SubmissionFilesDir = "Files";
 public static final String UsersDir = "Users";
 public static final String GroupsDir = "Groups";
 
 public static final String WorkdirParameter       = "workDir";

 public static final String UserGroupDirParameter       = "userGroupDir";
 public static final String SubmissionDirParameter = "submissionDir";
 public static final String SubmissionTransactionDirParameter = "submissionTransactionDir";
 public static final String SubmissionHistoryDirParameter      = "submissionHistoryDir";

 
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;
 
 private static String dataMountPath;
 private static String recapchaPrivateKey;

 private static long instanceId;
 private static AtomicInteger sequence;

 private static ServiceManager defaultServiceManager;
 private static EntityManagerFactory emf;


 private static Path workDirectory;
 private static Path userGroupPath;
 private static Path usersPath;
 private static Path groupsPath;
 private static Path submissionsPath;
 private static Path submissionsHistoryPath;
 private static Path submissionsTransactionPath;
 
 public static void init( int contextHash )
 {
  instanceId = System.currentTimeMillis() ^ contextHash;
  sequence = new AtomicInteger();
 }
 
 public static long getInstanceId()
 {
  return instanceId;
 }
 
 public static int getSeqNumber()
 {
  return sequence.getAndIncrement();
 }
 
 public static boolean readParameter(String param, String val) throws ServiceConfigException
 {
  
  if( WorkdirParameter.equals(param) )
  {
   workDirectory=FileSystems.getDefault().getPath(val);
   
   return true;
  }
  
  if( SubmissionDirParameter.equals(param) )
  {
   submissionsPath = FileSystems.getDefault().getPath(val);

   return true;
  }
  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   submissionsHistoryPath = FileSystems.getDefault().getPath(val);
 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   submissionsTransactionPath = FileSystems.getDefault().getPath(val);
 
   return true;
  }


  if( UserGroupDirParameter.equals(param) )
  {
   userGroupPath = FileSystems.getDefault().getPath(val);

   usersPath = userGroupPath.resolve(UsersDir);
   groupsPath = userGroupPath.resolve(GroupsDir);
   
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

 
 public static Path getWorkDirectory()
 {
  return workDirectory;
 }
 
 public static void setWorkDirectory( String dir )
 {
  workDirectory=FileSystems.getDefault().getPath(dir);
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

 public static Path getUserGroupPath()
 {
  return userGroupPath;
 }

 
 public static Path getUsersPath()
 {
  return usersPath;
 }

 public static Path getUserDirPath(User user)
 {
  return usersPath.resolve( String.valueOf( user.getId() ));
 }

 
 public static Path getGroupsPath()
 {
  return groupsPath;
 }

 public static Path getGroupDirPath( UserGroup g )
 {
  return groupsPath.resolve( String.valueOf( g.getId() ) );
 }

 
 public static Path getSubmissionsPath()
 {
  return submissionsPath;
 }

 
 public static Path getSubmissionPath(Submission sbm)
 {
  return submissionsPath.resolve(AccNoUtil.encode( sbm.getAccNo() ));
 }


 public static Path getSubmissionFilesPath(Submission sbm)
 {
  return getSubmissionPath(sbm).resolve( SubmissionFilesDir );
 }


 public static Path getSubmissionsHistoryPath()
 {
  return submissionsHistoryPath;
 }
 
 public static Path getSubmissionHistoryPath(Submission sbm)
 {
  return submissionsHistoryPath.resolve(AccNoUtil.encode( sbm.getAccNo() )+SubmissionHistoryPostfix+( sbm.getVersion() ));
 }


 public static String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }


 public static boolean isLinkingAllowed()
 {
  return true;
 }

}
