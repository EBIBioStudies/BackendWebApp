package uk.ac.ebi.biostd.webapp.server.endpoint.submit;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.JSON4Log;
import uk.ac.ebi.biostd.treelog.JSON4Report;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager.Operation;
import uk.ac.ebi.biostd.webapp.shared.tags.TagRef;
import uk.ac.ebi.biostd.webapp.shared.tags.TagRefParser;

public class SubmitServlet extends ServiceServlet
{

 private static final long serialVersionUID = 1L;

 public static final String validateOnlyParameter = "validateOnly";
 public static final String idParameter = "id";
 public static final String accnoParameter = "accno";
 public static final String accnoPatternParameter = "accnoPattern";
 public static final String requestIdParameter = "requestId";
 public static final String tagsParameter = "tags";
 public static final String accessParameter = "access";
 public static final String releaseDateParameter = "releaseDate";

 
 
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response, Session sess) throws ServletException, IOException
 {
  String reqId = request.getParameter(requestIdParameter);
  
  if( reqId != null && BackendConfig.isEnableUnsafeRequests() )
  {
   String tname = Thread.currentThread().getName();

   try
   {
    Thread.currentThread().setName(reqId);
    
    serviceContinue(request, response, sess);
   }
   finally
   {
    Thread.currentThread().setName(tname);
   }
  }
  else
   serviceContinue(request, response, sess);
  
 }
 
 protected void serviceContinue(HttpServletRequest request, HttpServletResponse response, Session sess) throws ServletException, IOException
 {
  if( sess == null || sess.isAnonymouns() )
  {
   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
   response.getWriter().print("FAIL User not logged in");
   return;
  }

  Operation act = null;

  String pi = request.getPathInfo();

  if(pi != null && pi.length() > 1)
  {
   pi=pi.substring(1);
   
   for( Operation op : Operation.values() )
   {
    if( op.name().equalsIgnoreCase(pi) )
    {
     act = op;
     break;
    }
   }
   
  }
  
  if( act == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Invalid path: " + pi);
   return;
  }

  if( act == Operation.DELETE )
  {
   processDelete( request, response, sess );
   return;
  }
  
  if( act == Operation.TRANKLUCATE )
  {
   processTranklucate( request, response, sess );
   return;
  }

  if( act == Operation.SETMETA )
  {
   processSetMeta( request, response, sess );
   return;
  }

  
  String cType = request.getContentType();
  
  if( cType == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL 'Content-type' header missing");
   return;
  }
  
  int pos = cType.indexOf(';');
  
  if( pos > 0 )
   cType = cType.substring(0,pos).trim();
  
  DataFormat fmt = null;
  
  for( DataFormat f : DataFormat.values() )
  {
   if( f.getContentType().equalsIgnoreCase(cType) )
   {
    fmt = f;
    break;
   }
  }
  
  if( fmt == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Content type '"+cType+"' is not supported");
   return;
  }

  byte[] data = IOUtils.toByteArray(request.getInputStream());


  if( data.length == 0 )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Empty request body");
   return;
  }
  
  String vldPrm = request.getParameter(validateOnlyParameter);
  
  boolean validateOnly = vldPrm != null && ("true".equalsIgnoreCase(vldPrm) || "yes".equalsIgnoreCase(vldPrm) || "1".equals(vldPrm) );

  SubmissionReport res = BackendConfig.getServiceManager().getSubmissionManager().createSubmission(data, fmt, request.getCharacterEncoding(), act, sess.getUser(), validateOnly);
  
  LogNode topLn = res.getLog();
  
  SimpleLogNode.setLevels(topLn);

//  if( topLn.getLevel().getPriority() >= Level.ERROR.getPriority() )
//   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
  
  response.setContentType("application/json");
  
  JSON4Report.convert(res, response.getWriter());
  
 }
 
 private void processSetMeta(HttpServletRequest request, HttpServletResponse response, Session sess) throws IOException
 {
  String sbmAcc = request.getParameter(accnoParameter);
  
  if(sbmAcc == null )
   sbmAcc = request.getParameter(idParameter);
  
  if(sbmAcc == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL '"+accnoParameter+"' parameter is not specified");
   return;
  }
  
  String val =  request.getParameter(tagsParameter);
  
  List<TagRef> tags = null;
  
  if( val != null )
  {
   try
   {
    tags = TagRefParser.parseTags(val);
   }
   catch(ParserException e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL invalid '"+tagsParameter+"' parameter value");
    return;
   }
  }
  
  val =  request.getParameter(accessParameter);
  
  Set<String> access = null;
  
  if( val != null )
  {
   val = val.trim();
   
   if( val.length() == 0 )
    access = Collections.emptySet();
   else
   {
    access = new HashSet<String>();
    
    for( String s : val.split(",") )
     access.add( s.trim() );
   }
   
  }
  
  
  val = request.getParameter(releaseDateParameter);
  
  long rTime = -1;
  
  if( val != null )
  {
   
   Matcher mtch =  Pattern.compile(Submission.releaseDateFormat).matcher(val);

   if(!mtch.matches())
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid '" + releaseDateParameter + "' parameter value. Expected date in format: YYYY-MM-DD[Thh:mm[:ss[.mmm]]]");
    return;
   }
   else
   {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    cal.set(Calendar.YEAR, Integer.parseInt(mtch.group("year")));
    cal.set(Calendar.MONTH, Integer.parseInt(mtch.group("month")) - 1);
    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mtch.group("day")));

    String str = mtch.group("hour");

    if(str != null)
     cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str));

    str = mtch.group("min");

    if(str != null)
     cal.set(Calendar.MINUTE, Integer.parseInt(str));

    str = mtch.group("sec");

    if(str != null)
     cal.set(Calendar.SECOND, Integer.parseInt(str));

    rTime = cal.getTimeInMillis();
   }
  }

  LogNode topLn = BackendConfig.getServiceManager().getSubmissionManager().updateSubmissionMeta(sbmAcc, tags, access, rTime, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  JSON4Log.convert(topLn, response.getWriter());
  
 }

 public void processDelete(HttpServletRequest request, HttpServletResponse response, Session sess) throws IOException
 {
  String sbmAcc = request.getParameter("accno");
  
  if(sbmAcc == null )
   sbmAcc = request.getParameter("id");
  
  if(sbmAcc == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL 'id' parameter is not specified");
   return;
  }
  
  response.setContentType("application/json");
  
  LogNode topLn = BackendConfig.getServiceManager().getSubmissionManager().deleteSubmissionByAccession(sbmAcc, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  JSON4Log.convert(topLn, response.getWriter());

 }

 public void processTranklucate(HttpServletRequest request, HttpServletResponse response, Session sess ) throws IOException
 {
  String sbmID = request.getParameter(idParameter);
  String sbmAcc = request.getParameter(accnoParameter);
  String patAcc = request.getParameter(accnoPatternParameter);
  
  boolean clash=false;
  
  if( patAcc != null )
  {
   if( sbmAcc != null || sbmID != null )
    clash = true;
  }
  else if( sbmAcc != null )
  {
   if( patAcc != null || sbmID != null )
    clash = true;
  }
  else if( sbmID != null )
  {
   if( patAcc != null || sbmAcc != null )
    clash = true;
  }
  else
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL '"+idParameter+"' or '"+accnoParameter+"' or '"+accnoPatternParameter+"' parameter is not specified");
   return;
  }
  
  if( clash )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().print("FAIL Parameters '"+idParameter+"', '"+accnoParameter+"' and '"+accnoPatternParameter+"' can'n be used at the same time");
   return;
  }
  
  int id = -1;
  
  if( sbmID != null  )
  {
   try
   {
    id = Integer.parseInt(sbmID);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid '"+idParameter+"' parameter value. Must be integer");
    return;
   }
  }
  
  if( patAcc != null ) //&& patAcc.length() < 5 && patAcc.startsWith("%") || patAcc.startsWith("") )
  {
   if( patAcc.length() < 5 )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid '"+accnoPatternParameter+"' parameter value. Pattern is too short");
    return;
   }
   
   int pfxLen = 0;
   
   for( int i=0; i < patAcc.length(); i++ )
   {
    char c = patAcc.charAt(i);
    
    if( c == '?' || c== '%' )
     break;
    
    pfxLen++;
   }
   
   if( pfxLen < 5 )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid '"+accnoPatternParameter+"' parameter value. Pattern is too loose. Should have 5 characters prefix");
    return;
   }
  }
  
  response.setContentType("application/json");
  
  LogNode topLn = null;
  
  if( sbmID != null )
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionById(id, sess.getUser());
  else if( sbmAcc!= null )
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionByAccession(sbmAcc, sess.getUser());
  else
   topLn = BackendConfig.getServiceManager().getSubmissionManager().tranklucateSubmissionByAccessionPattern(patAcc, sess.getUser());
  
  SimpleLogNode.setLevels(topLn);
  JSON4Log.convert(topLn, response.getWriter());

 }
 
}
