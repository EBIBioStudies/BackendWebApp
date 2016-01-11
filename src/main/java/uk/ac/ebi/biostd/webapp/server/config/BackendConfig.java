package uk.ac.ebi.biostd.webapp.server.config;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.export.TaskInfo;
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

 public static Set<PosixFilePermission> rwxrwx___ = PosixFilePermissions.fromString("rwxrwx---");
 public static Set<PosixFilePermission> rwxrwxr_x = PosixFilePermissions.fromString("rwxrwxr-x");

 public static final String googleVerifyURL = "https://www.google.com/recaptcha/api/siteverify";
 public static final String googleSecretParam = "secret";
 public static final String googleResponseParam = "response";
 public static final String googleRemoteipParam = "remoteip";
 public static final String googleClientResponseParameter="recaptcha2-response";
 public static final String googleSuccessField = "success";
 
 
 public static final String GuestsGroup = "@Guests";
 public static final String EveryoneGroup = "@Everyone";
 public static final String AuthenticatedGroup = "@Authenticated";

 public static final String             PublicTag                           = "Public";

 public static final String             SubmissionHistoryPostfix            = "#ver";
 public static final String             SubmissionFilesDir                  = "Files";
 public static final String             UsersDir                            = "Users";
 public static final String             GroupsDir                           = "Groups";

 public static final String             CreateFileStructureParameter        = "createFileStructure";

 public static final String             BaseDirParameter                    = "baseDir";
 
 public static final String             WorkdirParameter                    = "workDir";

 public static final String             UserGroupDirParameter               = "userGroupDir";
 public static final String             SubmissionDirParameter              = "submissionDir";
 public static final String             SubmissionTransactionDirParameter   = "submissionTransactionDir";
 public static final String             SubmissionHistoryDirParameter       = "submissionHistoryDir";
 public static final String             AllowFileLinksParameter             = "allowFileLinks";
 public static final String             PublicFTPDirParameter               = "publicFTPDir";
 public static final String             SubmissionUpdateParameter           = "updateDir";
 public static final String             UpdateURLParameter                  = "updateListenerURL";
 public static final String             UpdateURLFilePlaceholder            = "{file}";
 public static final String             UpdateWaitPeriodParameter           = "updateWaitPeriod";
 public static final String             MaxUpdatesPerFileParameter          = "maxUpdatesPerFile";
 public static final String             FTPRootPathParameter                = "FTPRootPath";
 public static final String             DropBoxPathParameter                = "dropboxPath";
 
 public static final String             DefaultSubmissionAccPrefixParameter = "defaultSubmissionAccNoPrefix";
 public static final String             DefaultSubmissionAccSuffixParameter = "defaultSubmissionAccNoSuffix";

 
 public static final String DataMountPathParameter = "dataMountPath";
 public static final String RecapchaPrivateKeyParameter = "recapcha_private_key";
 
 public static final int maxPageTabSize=5000000;

 
 private static String dataMountPath;
 private static String recapchaPrivateKey;

 private static long instanceId;
 private static AtomicInteger sequence;

 private static ServiceManager defaultServiceManager;
 private static EntityManagerFactory emf;
 private static TaskInfo expTaskInfo;

 private static boolean createFileStructure=false;

 private static Path baseDirectory;
 private static Path workDirectory;
 private static Path userGroupPath;
 private static Path usersPath;
 private static Path groupsPath;
 private static Path submissionsPath;
 private static Path submissionsHistoryPath;
 private static Path submissionsTransactionPath;
 private static Path publicFTPPath;
 private static Path submissionUpdatePath;
 private static Path ftpRootPath;
 private static Path dropboxPath;

 private static String updateListenerURLPfx;
 private static String updateListenerURLSfx;
 
 private static String defaultSubmissionAccPrefix = null;
 private static String defaultSubmissionAccSuffix = null;
 
 private static int updateWaitPeriod = 5;
 private static int maxUpdatesPerFile = 50;
 
 private static boolean fileLinkAllowed=true;

 private static Map<String, Object> databaseConfig;
 
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
  val = val.trim();
  param = param.trim();
  
  if( DefaultSubmissionAccPrefixParameter.equals(param) )
  {
   defaultSubmissionAccPrefix=val;
   
   return true;
  }

  if( DefaultSubmissionAccSuffixParameter.equals(param) )
  {
   defaultSubmissionAccSuffix=val;
   
   return true;
  }

  if( BaseDirParameter.equals(param) )
  {
   baseDirectory=FileSystems.getDefault().getPath(val);
   
   if( ! baseDirectory.isAbsolute() )
    throw new ServiceConfigException(BaseDirParameter+": path should be absolute");
   
   return true;
  }

  if( WorkdirParameter.equals(param) )
  {
   workDirectory=createPath(WorkdirParameter,val);
   
   return true;
  }
  
  if( FTPRootPathParameter.equals(param) )
  {
   ftpRootPath=FileSystems.getDefault().getPath(val);
   
   return true;
  }

  if( DropBoxPathParameter.equals(param) )
  {
   dropboxPath=FileSystems.getDefault().getPath(val);
   
   return true;
  }

  
  if( SubmissionDirParameter.equals(param) )
  {
   submissionsPath = createPath(SubmissionDirParameter,val);

   return true;
  }
  
  if( SubmissionUpdateParameter.equals(param) )
  {
   submissionUpdatePath = createPath(SubmissionUpdateParameter,val);

   return true;
  }
  
  if( UpdateURLParameter.equals(param) )
  {
   int pos = val.indexOf(UpdateURLFilePlaceholder);
   
   if( pos < 0 )
    throw new ServiceConfigException(UpdateURLParameter+" should contain "+UpdateURLFilePlaceholder+" placeholder");
   
   updateListenerURLPfx = val.substring(0,pos);
   updateListenerURLSfx = val.substring(pos+UpdateURLFilePlaceholder.length());

   try
   {
    new URL(updateListenerURLPfx+"aaa.txt"+updateListenerURLSfx);
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(UpdateURLParameter+": invalid URL '"+val+"'");
   }
   
   return true;
  }

  if( UpdateWaitPeriodParameter.equals(param) )
  {
   try
   {
    updateWaitPeriod = Integer.parseInt(val);
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(UpdateWaitPeriodParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  if( MaxUpdatesPerFileParameter.equals(param) )
  {
   try
   {
    maxUpdatesPerFile = Integer.parseInt(val);
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(MaxUpdatesPerFileParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }

  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   submissionsHistoryPath = createPath(SubmissionHistoryDirParameter,val);

 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   submissionsTransactionPath = createPath(SubmissionTransactionDirParameter,val);
 
   return true;
  }

  if( PublicFTPDirParameter.equals(param) )
  {
   publicFTPPath = createPath(PublicFTPDirParameter,val);
 
   return true;
  }


  if( UserGroupDirParameter.equals(param) )
  {
   userGroupPath = createPath(UserGroupDirParameter,val);

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

  if( AllowFileLinksParameter.equals(param) )
  {
   fileLinkAllowed = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
   return true;
  }

  if( CreateFileStructureParameter.equals(param) )
  {
   createFileStructure = val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1");
  }

  
  return false;

 }

 private static Path createPath( String prm, String p ) throws ServiceConfigException
 {
  Path np = FileSystems.getDefault().getPath(p);
  
  if( np.isAbsolute() )
   return np;
  
  if( baseDirectory != null )
   return baseDirectory.resolve(np);

  throw new ServiceConfigException(prm+": path should be either absolute or "+BaseDirParameter+" parameter should be defined before");
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

 public static String getSubmissionRelativePath( Submission sbm )
 {
  return AccNoUtil.getPartitionedPath( sbm.getAccNo() );
 }
 
 public static Path getSubmissionPath(Submission sbm)
 {
  return submissionsPath.resolve( getSubmissionRelativePath(sbm) );
 }


 public static Path getSubmissionFilesPath(Submission sbm)
 {
  return getSubmissionPath(sbm).resolve( SubmissionFilesDir );
 }

 public static Path getSubmissionPublicFTPPath(Submission sbm)
 {
  return getPublicFTPPath().resolve( getSubmissionRelativePath(sbm) );
 }

 public static Path getSubmissionsHistoryPath()
 {
  return submissionsHistoryPath;
 }
 
 public static Path getSubmissionHistoryPath(Submission sbm)
 {
  return submissionsHistoryPath.resolve( getSubmissionRelativePath(sbm) + SubmissionHistoryPostfix + Math.abs(sbm.getVersion()) );
 }


 public static String getRecapchaPrivateKey()
 {
  return recapchaPrivateKey;
 }


 public static boolean isLinkingAllowed()
 {
  return fileLinkAllowed;
 }

 public static Path getPublicFTPPath()
 {
  return publicFTPPath;
 }
 
 public static Path getSubmissionUpdatePath()
 {
  return submissionUpdatePath;
 }

 public static String getUpdateListenerURLPrefix()
 {
  return updateListenerURLPfx;
 }
 
 public static String getUpdateListenerURLPostfix()
 {
  return updateListenerURLSfx;
 }

 public static void setPublicFTPPath(Path publicFTPPath)
 {
  BackendConfig.publicFTPPath = publicFTPPath;
 }

 public static String getDefaultSubmissionAccPrefix()
 {
  return defaultSubmissionAccPrefix;
 }

 public static String getDefaultSubmissionAccSuffix()
 {
  return defaultSubmissionAccSuffix;
 }

 public static int getUpdateWaitPeriod()
 {
  return updateWaitPeriod;
 }

 public static int getMaxUpdatesPerFile()
 {
  return maxUpdatesPerFile;
 }

 public static boolean isCreateFileStructure()
 {
  return createFileStructure;
 }

 public static Path getBaseDirectory()
 {
  return baseDirectory;
 }

 public static Path getFtpRootPath()
 {
  return ftpRootPath;
 }

 public static Path getDropboxPath()
 {
  return dropboxPath;
 }


 public static TaskInfo getExportTask()
 {
  return expTaskInfo;
 }
 
 public static void setExportTask( TaskInfo ti )
 {
  expTaskInfo = ti;
 }

 public static void setDatabaseConfig(Map<String, Object> dbConfig)
 {
  databaseConfig = new HashMap<String, Object>(dbConfig);
  
 }
 
 public static Map<String, Object> getDatabaseConfig()
 {
  return databaseConfig;
  
 }

}
