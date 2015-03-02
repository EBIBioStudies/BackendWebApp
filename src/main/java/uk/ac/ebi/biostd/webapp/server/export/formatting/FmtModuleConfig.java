package uk.ac.ebi.biostd.webapp.server.export.formatting;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.biostd.webapp.server.util.ParamPool;

public class FmtModuleConfig
{



 public static final String PublicOnlyParameter        = "publicOnly";

 public static final String OutputFileParameter        = "outfile";
 public static final String FormatParameter            = "format";
 public static final String TmpDirParameter            = "tmpdir";

 private String      format;
 private Boolean     publicOnly;
 private String      outputFile;
 private String      tmpDir;
 private Map<String, String> formatterParams=new HashMap<String, String>();

 public void loadParameters(ParamPool params, String pfx)
 {
  if( pfx == null )
   pfx="";
  
  format = params.getParameter(pfx+FormatParameter);
  
  outputFile = params.getParameter(pfx+OutputFileParameter);
  
  tmpDir = params.getParameter(pfx+TmpDirParameter);
  
  
  
  String pv = params.getParameter(pfx+PublicOnlyParameter);
  
  if( pv != null  )
  {
   publicOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }
  
  Enumeration<String> pnames = params.getNames();
  
  while( pnames.hasMoreElements() )
  {
   String nm = pnames.nextElement();
   
   if( nm.startsWith(FormatParameter+".") )
    formatterParams.put(nm.substring(FormatParameter.length()+1), params.getParameter(nm));
  }

 }

 
 public Boolean getPublicOnly(boolean def)
 {
  return publicOnly!=null?publicOnly:def;
 }


 public String getFormat(String def)
 {
  return format!=null?format:def;
 }


 public String getOutputFile(String def)
 {
  return outputFile!=null?outputFile:def;
 }
 

 public String getTmpDir(String def)
 {
  return tmpDir!=null?tmpDir:def;
 }
 
 public Map<String, String> getFormatterParams()
 {
  return formatterParams;
 }

}
