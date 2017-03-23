package uk.ac.ebi.biostd.webapp.server.export.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.out.TextStreamFormatter;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.export.ExporterStat;
import uk.ac.ebi.biostd.webapp.server.export.OutputModule;
import uk.ac.ebi.biostd.webapp.server.export.TaskConfigException;


public class JSONSourceFixOutputModule implements OutputModule, TextStreamFormatter
{
 private static Logger log = null;
 private String name;
 private JSONFormatter formatter;

 public JSONSourceFixOutputModule(String name, Map<String, String> cfgMap) throws TaskConfigException
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  
  this.name = name;
  
  formatter = new JSONFormatter(null, true);
 }

  @Override
 public TextStreamFormatter getFormatter()
 {
  return this;
 }

 @Override
 public Appendable getOut()
 {
  return null;
 }

 @Override
 public void start() throws IOException
 {
 }

 @Override
 public void finish(ExporterStat stat) throws IOException
 {
 }

 @Override
 public void cancel() throws IOException
 {
 }

 @Override
 public String getName()
 {
  return name;
 }

 @Override
 public void comment(String arg0, Appendable arg1) throws IOException
 {
 }

 @Override
 public void footer(Appendable arg0) throws IOException
 {
 }

 @Override
 public void format(Submission sbm, Appendable arg1) throws IOException
 {
  Path filesP = BackendConfig.getSubmissionFilesPath(sbm);
  
  try( PrintStream out = new PrintStream( filesP.resolve(sbm.getAccNo()+".json").toFile() ) )
  {
   formatter.format(sbm, out);
  }
  catch (Exception e) 
  {
   log.error("Can't generate JSON source file: "+e.getMessage());
   e.printStackTrace();
  }
  
 }

 @Override
 public void header(Map<String, List<String>> arg0, Appendable arg1) throws IOException
 {
 }

 @Override
 public void separator(Appendable arg0) throws IOException
 {
 }

}
