package uk.ac.ebi.biostd.webapp.server.config;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.webapp.server.export.TaskConfig;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.ServiceConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceInitExceprion;
import uk.ac.ebi.biostd.webapp.server.util.ParamPool;

public class ConfigurationManager
{
 static final String DBParamPrefix = "db.";
 static final String ServiceParamPrefix = "biostd.";
 static final String TaskParamPrefix = "export.";
 static final String EmailParamPrefix = "email.";
 static final String OutputParamPrefix = "output";

 static final String OutputClassParameter = "class";
 
 public static final String             CreateFileStructureParameter        = "createFileStructure";

 public static final String             BaseDirParameter                    = "baseDir";
 
 public static final String             WorkdirParameter                    = "workDir";

 public static final String             EnableUnsafeRequestsParameter       = "enableUnsafeRequests";
 public static final String             UserGroupDirParameter               = "userGroupDir";
 public static final String             UserGroupIndexDirParameter          = "userGroupIndexDir";
 public static final String             PublicDropboxesParameter            = "publicDropboxes";
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
 
 public static final String             MandatoryAccountActivationParameter = "mandatoryAccountActivation";
 public static final String             ActivationEmailSubjectParameter     = "activationEmailSubject";
 public static final String             ActivationEmailPlainTextParameter   = "activationEmailPlainTextFile";
 public static final String             ActivationEmailHtmlParameter        = "activationEmailHtmlFile";
 public static final String             ActivationTimeoutParameter          = "activationTimeout";
 public static final String             ActivationTimeoutParameterHours     = "activationTimeoutHours";

 public static final String             SubscriptionEmailSubjectParameter   = "subscriptionEmailSubject";
 public static final String             SubscriptionEmailPlainTextParameter = "subscriptionEmailPlainTextFile";
 public static final String             SubscriptionEmailHtmlParameter      = "subscriptionEmailHtmlFile";
 
 public static final String             PassResetTimeoutParameter           = "passwordResetTimeout";
 public static final String             PassResetEmailSubjectParameter      = "passwordResetEmailSubject";
 public static final String             PassResetEmailPlainTextParameter    = "passwordResetEmailPlainTextFile";
 public static final String             PassResetEmailHtmlParameter         = "passwordResetEmailHtmlFile";
 
 public static final String             DefaultSubmissionAccPrefixParameter = "defaultSubmissionAccNoPrefix";
 public static final String             DefaultSubmissionAccSuffixParameter = "defaultSubmissionAccNoSuffix";
 
 public static final String             DataMountPathParameter              = "dataMountPath";
 public static final String             RecapchaPrivateKeyParameter         = "recapcha_private_key";
 
 private ParamPool contextParamPool;
 
 private static Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

 
 public ConfigurationManager()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }
 

 public static boolean readConfiguration( ParamPool config ) throws ServiceInitExceprion
 {
  ConfigBean cfgBean = BackendConfig.createConfig(); 
  
  Map<String, Object> dbConfig = new HashMap<String, Object>();
  Map<String, Object> emailConfig = new HashMap<String, Object>();
  TaskConfig taskConfig = null;
  
  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;
  
  Enumeration<String> pNames = config.getNames();

  String baseDir = config.getParameter(ServiceParamPrefix+BaseDirParameter);
  
  if( baseDir != null )
  {
   try
   {
    if(!readParameter(BaseDirParameter, baseDir,cfgBean))
     log.warn("Unknown configuration parameter: " + BaseDirParameter + " will be ignored");
   }
   catch(ServiceConfigException e)
   {
    log.error("Invalid parameter value: " + BaseDirParameter + "=" + baseDir+" "+e.getMessage());
    confOk = false;
   }
  }
  
  while(pNames.hasMoreElements())
  {
   String key = pNames.nextElement();
   String val = config.getParameter(key);

   if(key.startsWith(DBParamPrefix))
    dbConfig.put(key.substring(DBParamPrefix.length()), val);
   else if(key.startsWith(ServiceParamPrefix))
   {
    String param = key.substring(ServiceParamPrefix.length());

    try
    {
     if(!readParameter(param, val, cfgBean))
      log.warn("Unknown configuration parameter: " + key + " will be ignored");
    }
    catch(ServiceConfigException e)
    {
     log.error("Invalid parameter value: " + key + "=" + val+" "+e.getMessage());
     confOk = false;
    }
   }
   else if(key.startsWith(TaskParamPrefix))
   {
    if(taskConfig == null)
     taskConfig = new TaskConfig("export");

    String param = key.substring(TaskParamPrefix.length());

    outMtch.reset(param);

    if(outMtch.matches())
    {
     String outName = outMtch.group(1);
     String outParam = outMtch.group(2);

     if(outName == null)
      outName = "_default_";

     taskConfig.addOutputParameter(outName, outParam, val);
    }
    else
    {
     try
     {
      if(!taskConfig.readParameter(param, val))
       log.warn("Unknown configuration parameter: " + key + " will be ignored");
     }
     catch(TaskConfigException e)
     {
      log.error("Parameter read error: "+e.getMessage());
      confOk = false;
     }
    }

   }
   else if(key.startsWith(EmailParamPrefix))
   {
    if( emailConfig == null )
     emailConfig = new HashMap<String, Object>();
    
    dbConfig.put(key.substring(EmailParamPrefix.length()), val);
   }
   else
    log.warn("Invalid parameter {} will be ignored.", key);
   
   
  }

  
  cfgBean.setEmailConfig(emailConfig);
  cfgBean.setTaskConfig(taskConfig);
  
  return confOk;
  
 }
 
 
 public static void validateConfiguration( ConfigBean cfg )
 {
  if( cfg.isCreateFileStructure() && cfg.getBaseDirectory() != null )
  {
   try
   {
    Files.createDirectories(cfg.getBaseDirectory());
    Files.setPosixFilePermissions(cfg.getBaseDirectory(), BackendConfig.rwxrwx___);
   }
   catch( UnsupportedOperationException e )
   {
    log.warn("Filesystem doesn't support POSIX file permissions. Please check base directory permissions manually");
   }
   catch(IOException e)
   {
    throw new RuntimeException("Directory access error: "+cfg.getBaseDirectory());
   }
  }
  
  Path dir = cfg.getWorkDirectory();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+WorkdirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);
  
  dir = cfg.getUserGroupDropboxPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+UserGroupDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  
  dir = cfg.getUserGroupIndexPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+UserGroupIndexDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( ! checkDirectory( cfg.getUsersIndexPath() ) )
   throw new RuntimeException("Directory access error: "+cfg.getUsersIndexPath() );

  if( ! checkDirectory( cfg.getGroupsIndexPath() ) )
   throw new RuntimeException("Directory access error: "+cfg.getGroupsIndexPath() );

  
  if( ! checkDirectory( cfg.getSubmissionUpdatePath() ) )
   throw new RuntimeException("Directory access error: "+cfg.getSubmissionUpdatePath() );
 

  dir = cfg.getSubmissionsPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  
  dir = cfg.getSubmissionsHistoryPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionHistoryDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }
  
  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

  
  dir = cfg.getSubmissionsTransactionPath();
  
  if( dir == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+SubmissionTransactionDirParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( ! checkDirectory(dir) )
   throw new RuntimeException("Directory access error: "+dir);

//  if( BackendConfig.getServiceManager().getEmailService() == null )
//  {
//   log.error("Email service is not configured");
//   throw new RuntimeException("Invalid configuration");
//  }
  
  if( cfg.isMandatoryAccountActivation() && cfg.getActivationEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+ActivationEmailSubjectParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( cfg.isMandatoryAccountActivation() && cfg.getActivationEmailPlainTextFile() == null && cfg.getActivationEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+ActivationEmailPlainTextParameter+" "+
     ServiceParamPrefix+ActivationEmailHtmlParameter+" parameters must be set");
   throw new RuntimeException("Invalid configuration");
  }

  Path emailFile = cfg.getActivationEmailPlainTextFile();
  
  if( cfg.isMandatoryAccountActivation() && emailFile != null && ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+ActivationEmailPlainTextParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }
  
  emailFile = cfg.getActivationEmailHtmlFile();
  
  if( cfg.isMandatoryAccountActivation() && emailFile != null && ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+ActivationEmailHtmlParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }
  
  
  if( cfg.getPassResetEmailSubject() == null )
  {
   log.error("Mandatory "+ServiceParamPrefix+PassResetEmailSubjectParameter+" parameter is not set");
   throw new RuntimeException("Invalid configuration");
  }

  if( cfg.getPassResetEmailPlainTextFile() == null && cfg.getPassResetEmailHtmlFile() == null )
  {
   log.error("At least one of "+ServiceParamPrefix+PassResetEmailPlainTextParameter+" "+
     ServiceParamPrefix+PassResetEmailHtmlParameter+" parameters must be set");
   throw new RuntimeException("Invalid configuration");
  }  
  
  emailFile = cfg.getPassResetEmailPlainTextFile();
  
  if( emailFile == null || ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+PassResetEmailPlainTextParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }

  emailFile = cfg.getPassResetEmailHtmlFile();
  
  if( emailFile == null || ( ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) ) )
  {
   log.error(ServiceParamPrefix+PassResetEmailHtmlParameter+" should point to a regular readable file");
   throw new RuntimeException("Invalid configuration");
  }

  
  if( cfg.getSubscriptionEmailSubject() != null || cfg.getSubscriptionEmailPlainTextFile() != null || cfg.getSubscriptionEmailHtmlFile() != null )
  {
   if( cfg.getSubscriptionEmailSubject() == null || cfg.getSubscriptionEmailPlainTextFile() == null || cfg.getSubscriptionEmailHtmlFile() == null )
   {
    log.error("To activate tag subscriptions service the following parameters should be set: "
     +ServiceParamPrefix+SubscriptionEmailSubjectParameter
     +", "+ServiceParamPrefix+SubscriptionEmailPlainTextParameter
     +", "+ServiceParamPrefix+SubscriptionEmailHtmlParameter
      );
    throw new RuntimeException("Invalid configuration");
   }
   
   emailFile = cfg.getSubscriptionEmailPlainTextFile();

   if(  ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile) )
   {
    log.error(ServiceParamPrefix+SubscriptionEmailPlainTextParameter+" should point to a regular readable file");
    throw new RuntimeException("Invalid configuration");
   }

   emailFile = cfg.getSubscriptionEmailHtmlFile();
   
   if(  ! Files.isReadable(emailFile) || ! Files.isRegularFile(emailFile)  )
   {
    log.error(ServiceParamPrefix+SubscriptionEmailHtmlParameter+" should point to a regular readable file");
    throw new RuntimeException("Invalid configuration");
   }
  }
  
  
  Path sbmTestDir = cfg.getSubmissionsPath().resolve("~tmp");
  try
  {
   Path trnTestDir = cfg.getSubmissionsTransactionPath().resolve("~tmp");
   
   Files.createDirectory(sbmTestDir);
   Files.deleteIfExists(trnTestDir);
   Files.move(sbmTestDir, trnTestDir);
   Files.delete(trnTestDir);
  }
  catch(IOException e1)
  {
   try
   {
    Files.deleteIfExists(sbmTestDir);
   }
   catch(IOException e)
   {
   }
  
   log.error("Submission transaction directory: test oparation failed: "+e1.getMessage());
   log.error("Submission transaction directory should be on the same physical drive with submissions directory");

   throw new RuntimeException("Invalid configuration");
  }


 }
 
 
 public static boolean readParameter(String param, String val, ConfigBean cfg) throws ServiceConfigException
 {
  val = val.trim();
  param = param.trim();
  
  if( DefaultSubmissionAccPrefixParameter.equals(param) )
  {
   cfg.setDefaultSubmissionAccPrefix(val);
   
   return true;
  }

  if( DefaultSubmissionAccSuffixParameter.equals(param) )
  {
   cfg.setDefaultSubmissionAccSuffix(val);
   
   return true;
  }

  if( BaseDirParameter.equals(param) )
  {
   cfg.setBaseDirectory(FileSystems.getDefault().getPath(val));
   
   if( ! cfg.getBaseDirectory().isAbsolute() )
    throw new ServiceConfigException(BaseDirParameter+": path should be absolute");
   
   return true;
  }

  if( WorkdirParameter.equals(param) )
  {
   cfg.setWorkDirectory( createPath(WorkdirParameter,val, cfg.getBaseDirectory() ));
   
   return true;
  }
  

  if( FTPRootPathParameter.equals(param) )
  {
   cfg.setFtpRootPath(FileSystems.getDefault().getPath(val));
   
   return true;
  }

  
  if( SubmissionDirParameter.equals(param) )
  {
   cfg.setSubmissionsPath(createPath(SubmissionDirParameter,val, cfg.getBaseDirectory()));

   return true;
  }
  
  if( SubmissionUpdateParameter.equals(param) )
  {
   cfg.setSubmissionUpdatePath(createPath(SubmissionUpdateParameter,val, cfg.getBaseDirectory()));

   return true;
  }
  
  if( UpdateURLParameter.equals(param) )
  {
   int pos = val.indexOf(UpdateURLFilePlaceholder);
   
   if( pos < 0 )
    throw new ServiceConfigException(UpdateURLParameter+" should contain "+UpdateURLFilePlaceholder+" placeholder");
   
   cfg.setUpdateListenerURLPfx(val.substring(0,pos));
   cfg.setUpdateListenerURLSfx(val.substring(pos+UpdateURLFilePlaceholder.length()));

   try
   {
    new URL(cfg.getUpdateListenerURLPfx()+"aaa.txt"+cfg.getUpdateListenerURLSfx());
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
    cfg.setUpdateWaitPeriod(Integer.parseInt(val));
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
    cfg.setMaxUpdatesPerFile( Integer.parseInt(val) );
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(MaxUpdatesPerFileParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }

  
  if( SubmissionHistoryDirParameter.equals(param) )
  {
   cfg.setSubmissionsHistoryPath( createPath(SubmissionHistoryDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }
  
  if( SubmissionTransactionDirParameter.equals(param) )
  {
   cfg.setSubmissionsTransactionPath( createPath(SubmissionTransactionDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }

  if( PublicFTPDirParameter.equals(param) )
  {
   cfg.setPublicFTPPath( createPath(PublicFTPDirParameter,val, cfg.getBaseDirectory()) );
 
   return true;
  }

  if( UserGroupDirParameter.equals(param) )
  {
   cfg.setUserGroupDropboxPath( createPath(UserGroupDirParameter, val, cfg.getBaseDirectory()) );
   
   return true;
  }
  
  if( PublicDropboxesParameter.equals(param) )
  {
   cfg.setPublicDropboxes( "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val) );
   
   return true;
  }

  
  if( UserGroupIndexDirParameter.equals(param) )
  {
   cfg.setUserGroupIndexPath( createPath(UserGroupIndexDirParameter,val, cfg.getBaseDirectory()) );

   cfg.setUsersIndexPath( cfg.getUserGroupIndexPath().resolve(BackendConfig.UsersDir) );
   cfg.setGroupsIndexPath( cfg.getUserGroupIndexPath().resolve(BackendConfig.GroupsDir) );
   
   return true;
  }

  if( DataMountPathParameter.equals(param) )
  {
   cfg.setDataMountPath(val);
   return true;
  }
  
  if( RecapchaPrivateKeyParameter.equals(param) )
  {
   cfg.setRecapchaPrivateKey(val);
   return true;
  }

  if( AllowFileLinksParameter.equals(param) )
  {
   cfg.setFileLinkAllowed( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true;
  }
  
  if( EnableUnsafeRequestsParameter.equals(param) )
  {
   cfg.setEnableUnsafeRequests( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true;
  }


  if( CreateFileStructureParameter.equals(param) )
  {
   cfg.setCreateFileStructure( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true; 
  }
  
  if( MandatoryAccountActivationParameter.equals(param) )
  {
   cfg.setMandatoryAccountActivation( val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equals("1") );
   return true; 
  }


  if( ActivationEmailSubjectParameter.equals(param) )
  {
   cfg.setActivationEmailSubject(val);
   
   return true;
  }
  
  if( PassResetEmailSubjectParameter.equals(param) )
  {
   cfg.setPassResetEmailSubject(val);
   
   return true;
  }


  
  if( ActivationEmailPlainTextParameter.equals(param) )
  {
   cfg.setActivationEmailPlainTextFile(createPath(ActivationEmailPlainTextParameter,val, cfg.getBaseDirectory()));
   
   return true;
  }

  if( ActivationEmailHtmlParameter.equals(param) )
  {
   cfg.setActivationEmailHtmlFile(createPath(ActivationEmailHtmlParameter,val, cfg.getBaseDirectory()) );
   
   return true;
  }
  
  
  if( ActivationTimeoutParameter.equals(param) || ActivationTimeoutParameterHours.equals(param) )
  {
   try
   {
    cfg.setActivationTimeout( (long) ( Double.parseDouble(val) * 60 * 60 * 1000L ) ) ;
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(ActivationTimeoutParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  
  if( SubscriptionEmailSubjectParameter.equals(param) )
  {
   cfg.setSubscriptionEmailSubject(val);
   
   return true;
  }


  if( SubscriptionEmailPlainTextParameter.equals(param) )
  {
   cfg.setSubscriptionEmailPlainTextFile( createPath(SubscriptionEmailPlainTextParameter,val, cfg.getBaseDirectory()) );
   
   return true;
  }

  if( SubscriptionEmailHtmlParameter.equals(param) )
  {
   cfg.setSubscriptionEmailHtmlFile( createPath(SubscriptionEmailHtmlParameter,val, cfg.getBaseDirectory()) );
   
   return true;
  }

  
  if( PassResetEmailPlainTextParameter.equals(param) )
  {
   cfg.setPassResetEmailPlainTextFile( createPath(PassResetEmailPlainTextParameter,val, cfg.getBaseDirectory()) );
   
   return true;
  }

  if( PassResetEmailHtmlParameter.equals(param) )
  {
   cfg.setPassResetEmailHtmlFile( createPath(PassResetEmailHtmlParameter,val, cfg.getBaseDirectory()) );
   
   return true;
  }
  
  
  if( PassResetTimeoutParameter.equals(param) )
  {
   try
   {
    cfg.setPassResetTimeout( Integer.parseInt(val) * 60 * 60 * 1000L );
   }
   catch(Exception e)
   {
    throw new ServiceConfigException(PassResetTimeoutParameter+": integer value expected '"+val+"'");
   }
   
   return true;
  }
  
  return false;

 }
 
 private static Path createPath( String prm, String p, Path baseDir ) throws ServiceConfigException
 {
  Path np = FileSystems.getDefault().getPath(p);
  
  if( np.isAbsolute() )
   return np;
  
  if( baseDir != null )
   return baseDir.resolve(np);

  throw new ServiceConfigException(prm+": path should be either absolute or "+BaseDirParameter+" parameter should be defined before");
 }

 private static boolean checkDirectory(Path file)
 {
  if( Files.exists( file ) )
  {
   if( ! Files.isDirectory( file ) )
   {
    log.error("Path "+file+" is not a directory");
    return false;
   }
   
   if( ! Files.isWritable(file) )
   {
    log.error("Directory "+file+" is not writable");
    return false;
   }
  }
  else if( BackendConfig.isCreateFileStructure() )
  {
   try
   {
    Files.createDirectories( file );
   }
   catch(IOException e)
   {
    log.error("Can't create directory: "+file );
    return false;
   }
  }
  else
   return false;
  
  return true;
 }

}
