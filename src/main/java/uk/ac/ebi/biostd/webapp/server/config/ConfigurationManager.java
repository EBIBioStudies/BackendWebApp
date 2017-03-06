package uk.ac.ebi.biostd.webapp.server.config;

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
 
 private Logger log = null;

 
 public ConfigurationManager()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
 }
 
 public void setConfiguration()
 {
  
 }
 
 private boolean readConfig( ParamPool config ) throws ServiceInitExceprion
 {
  Map<String, Object> dbConfig = new HashMap<String, Object>();
  Map<String, Object> emailConfig = new HashMap<String, Object>();
  TaskConfig taskConfig = null;
  
  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+?)\\s*\\])?\\.(\\S+)$").matcher("");

  boolean confOk = true;
  
  Enumeration<String> pNames = config.getNames();

  String baseDir = config.getParameter(ServiceParamPrefix+BackendConfig.BaseDirParameter);
  
  if( baseDir != null )
  {
   try
   {
    if(!BackendConfig.readParameter(BackendConfig.BaseDirParameter, baseDir))
     log.warn("Unknown configuration parameter: " + BackendConfig.BaseDirParameter + " will be ignored");
   }
   catch(ServiceConfigException e)
   {
    log.error("Invalid parameter value: " + BackendConfig.BaseDirParameter + "=" + baseDir+" "+e.getMessage());
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
     if(!BackendConfig.readParameter(param, val))
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

  
  BackendConfig.setEmailConfig(emailConfig);
  BackendConfig.setTaskConfig(taskConfig);
  
  return confOk;
  
 }
}
