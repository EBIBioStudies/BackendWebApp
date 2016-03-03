package uk.ac.ebi.biostd.webapp.server.export.formatting;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.out.TextStreamFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.export.ExporterStat;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;
import uk.ac.ebi.biostd.webapp.server.util.MapParamPool;

public class ChunkedFormattingOutputModule implements OutputModule
{
 static final boolean DefaultShowNS = false;
 static final boolean DefaultShowAC = true;

 static final boolean DefaultPublicOnly = false;
 
 private final String name;
 
 private TextStreamFormatter formatter;
 
 private Path outDir;
 private Path tmpDir;

 private StreamPartitioner tmpStream;
 
 private static Logger log = null;
 
 public ChunkedFormattingOutputModule(String name, Map<String, String> cfgMap) throws TaskConfigException
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  
  this.name = name;
  
  FmtModuleConfig cfg = new FmtModuleConfig();
  
  cfg.loadParameters(new MapParamPool(cfgMap), "");
  
  
  String fmtr = cfg.getFormat(null);
  
  if( fmtr == null )
   throw new TaskConfigException("Output module '"+name+"': '"+FmtModuleConfig.FormatParameter+"' parameter not defined. It should point to formatter class");
  
  Class<?> fmtCls = null;
  
  try
  {
   fmtCls = Class.forName(fmtr);
  }
  catch( ClassNotFoundException e )
  {
   throw new TaskConfigException("Output module '"+name+"': Formatter class '"+fmtCls+"' not found");
  }
  
  if( ! TextStreamFormatter.class.isAssignableFrom(fmtCls) )
   throw new TaskConfigException("Output module '"+name+"': Class '"+fmtCls+"' doesn't implement Formatter interface");
  
  Constructor<?> ctor = null;
  

  
  try
  {
   try
   {
    ctor = fmtCls.getConstructor(Map.class);
    formatter = (TextStreamFormatter) ctor.newInstance(cfg.getFormatterParams());
   }
   catch(NoSuchMethodException e)
   {
    try
    {
     ctor = fmtCls.getConstructor();
     formatter = (TextStreamFormatter) ctor.newInstance();
    }
    catch(NoSuchMethodException e1)
    {
     throw new TaskConfigException("Output module '" + name + "': Can't fine appropriate constructor of class '" + fmtCls + "'");
    }
   }
   catch(SecurityException e)
   {
    throw new TaskConfigException("Output module '" + name + "': Can't get constructor of class '" + fmtCls + "' " + e.getMessage());
   }
  }
  catch(Exception ex)
  {
   throw new TaskConfigException("Output module '" + name + "': Can't create instance of class '" + fmtCls + "'");
  }
  
  String tmpDirName = cfg.getTmpDir(null);
  
  if( tmpDirName == null )
   throw new TaskConfigException("Output module '"+name+"': Temp directory is not defined");
  
  tmpDir = FileSystems.getDefault().getPath(tmpDirName);
  
  if( ! tmpDir.isAbsolute() )
  {
   if( BackendConfig.getBaseDirectory() == null )
   {
    log.error("Output module '"+name+"': Temporary directory path should be absolute or relative to path defined by '"+BackendConfig.BaseDirParameter+"' parameter");
    throw new TaskConfigException("Output module '"+name+"': Temporary directory path should be absolute or relative to path defined by '"+BackendConfig.BaseDirParameter+"' parameter");
   }
   
   tmpDir = BackendConfig.getBaseDirectory().resolve(tmpDir);
  }
  

  if( ! Files.exists(tmpDir) && BackendConfig.isCreateFileStructure() )
  {
   try
   {
    Files.createDirectories(tmpDir);
   }
   catch(IOException e)
   {
    log.error("Output module '"+name+"': Can't create temporary directory: " + tmpDir);
    throw new TaskConfigException("Output module '"+name+"': Can't create temporary directory: " + tmpDir);
   }
  }
  
  if( ! Files.isWritable( tmpDir ) )
  {
   log.error("Output module '"+name+"': Temporary directory is not writable: " + tmpDir);
   throw new TaskConfigException("Output module '"+name+"': Temporary directory is not writable: " + tmpDir);
  }
  
  
  String outFileName = cfg.getOutputFile(null);
  
  if( outFileName == null )
   throw new TaskConfigException("Output module '"+name+"': Output file is not defined");
  
  outDir = FileSystems.getDefault().getPath(outFileName);

  if( ! outDir.isAbsolute() )
  {
   if( BackendConfig.getBaseDirectory() == null )
   {
    log.error("Output module '"+name+"': Output file path should be absolute or relative to path defined by '"+BackendConfig.BaseDirParameter+"' parameter");
    throw new TaskConfigException("Output module '"+name+"': TOutput file path should be absolute or relative to path defined by '"+BackendConfig.BaseDirParameter+"' parameter");
   }
   
   outDir = BackendConfig.getBaseDirectory().resolve(outDir);
  }

  outFileName = outDir.getFileName().toString();
  
  outDir = outDir.getParent();

  if( ! Files.exists(outDir) && BackendConfig.isCreateFileStructure() )
  {
   try
   {
    Files.createDirectories(outDir);
   }
   catch(IOException e)
   {
    log.error("Output module '"+name+"': Can't create output directory: " + outDir);
    throw new TaskConfigException("Output module '"+name+"': Can't create output directory: " + outDir);
   }
  }
  
  if(! Files.isDirectory(outDir) || ! Files.isWritable(outDir) )
  {
   log.error("Output module '"+name+"': Output file directory is not writable: " + outDir);
   throw new TaskConfigException("Output module '"+name+"': Output file directory is not writable: " + outDir);
  }
 
  String sfx,pfx;
  
  int pos = outFileName.lastIndexOf('.');
  
  if( pos >= 0 )
  {
   pfx = outFileName.substring(0,pos);
   sfx = outFileName.substring(pos+1);
  }
  else
  {
   pfx = outFileName;
   sfx = "";
  }
  
  
  
  tmpStream = new StreamPartitioner(formatter, tmpDir, pfx, sfx, cfg.isChunkOutput()?cfg.getChunkSize():Long.MAX_VALUE, cfg.isChunkSizeInUnits());
 }
 
 
 
 @Override
 public TextStreamFormatter getFormatter()
 {
  return formatter;
 }

 @Override
 public Appendable getOut()
 {
  return tmpStream;
 }


 @Override
 public void start() throws IOException
 {
  log.debug("Starting export module for task '" + name + "'");
  tmpStream.reset();
 }
 
 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  if(tmpStream != null )
   tmpStream.close();
  
  Path outParent = outDir.getParent();
 
  Path tmpOutDir =outParent.resolve( "biostdMovingChunkedDir."+System.currentTimeMillis() );
  
  FileManager fmgr = BackendConfig.getServiceManager().getFileManager();
  
  try
  {
   Files.move(tmpDir, tmpOutDir);
   
   Files.createDirectories(tmpDir);
  }
  catch(Exception e)
  {
   fmgr.copyDirectory(tmpDir, tmpOutDir);
   
   fmgr.deleteDirectoryContents(tmpDir);
  }
  
  Path tmpOutDir2 = outParent.resolve( "biostdMovingDirBackup."+System.currentTimeMillis() );
  
  
  try
  {
   Files.move(outDir, tmpOutDir2);
   Files.move(tmpOutDir, outDir);

   fmgr.deleteDirectory(tmpOutDir2);
  }
  catch(Exception e)
  {
   e.printStackTrace();
   log.error("Task '"+name+"': Can't rename files in directiry: " + outParent);
  }

 }
 
 @Override
 public void cancel()
 {
   try
   {
    if(tmpStream != null )
     tmpStream.close();

    BackendConfig.getServiceManager().getFileManager().deleteDirectoryContents(tmpDir);
   }
   catch(IOException e)
   {
    e.printStackTrace();
    
    log.error("Task '"+name+"': Can't clean up task");
   }
  
 }

 
 @Override
 public String getName()
 {
  return name;
 }
}
