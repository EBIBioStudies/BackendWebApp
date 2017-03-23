package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.db.AdHocTagResolver;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.out.pageml.PageMLFormatter;
import uk.ac.ebi.biostd.treelog.JSON4Log;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.server.mng.impl.PTDocumentParser;

public class Converter
{

 public static void convert(byte[] data, DataFormat fmt, String outFmt, HttpServletResponse response) throws IOException
 {
  ParserConfig pc = new ParserConfig();
  
  pc.setMultipleSubmissions(true);
  pc.setPreserveId(false);
  
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Converting "+fmt.name()+" document", null);

  
  PMDoc doc = new PTDocumentParser(pc).parseDocument(data, fmt, "UTF-8", new AdHocTagResolver(), gln);
  
  Writer out  = response.getWriter();
  
  if( "xml".equalsIgnoreCase(outFmt))
  {
   if( doc == null )
    out.append("FAIL Invalid document");
   else
   {
    response.setContentType("text/xml");
    new PageMLFormatter(out,false).format(doc);
   }
  }
  else
  {
   response.setContentType("application/json");

   SimpleLogNode.setLevels(gln);

   out.append("{\nlog: ");
   JSON4Log.convert(gln, out);
  
   out.append(",\ndocument: ");

   new JSONFormatter(response.getWriter(), true).format(doc);
   
   out.append("\n}");

  }
 }

}
