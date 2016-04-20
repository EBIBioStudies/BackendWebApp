package uk.ac.ebi.biostd.webapp.server.endpoint.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.model.FileRef;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;

/**
 * Servlet implementation class ToolsServlet
 */

public class ToolsServlet extends ServiceServlet
{
 private static final long serialVersionUID = 1L;

 private static enum Operation
 {
  FIX_FILE_TYPE;
 }
 
 @Override
 protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess) throws ServletException, IOException
 {
  if( sess == null || sess.isAnonymouns() )
  {
   resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
   resp.getWriter().print("FAIL User not logged in");
   return;
  }
  
  if( ! sess.getUser().isSuperuser() )
  {
   resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
   resp.getWriter().print("FAIL Only superuser can run it");
   return;
  }

  Operation act = null;

  String pi = req.getPathInfo();

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
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().print("FAIL Invalid path: " + pi);
   return;
  }
  
  switch(act)
  {
   case FIX_FILE_TYPE:
    
    resp.setContentType("text/plain");
    fixFileType(resp.getWriter());
    
    break;

   default:
    break;
  }
  
 }

 private void fixFileType(PrintWriter out)
 {
  EntityManager mngr=null;
  
  int blockSz = 1000;
  
  try
  {
   int offset = 0;
   
   while( true )
   {
    if( mngr != null )
     mngr.close();
    
    mngr = BackendConfig.getEntityManagerFactory().createEntityManager();
    
    EntityTransaction t = mngr.getTransaction();
    
    t.begin();
    
    Query q = mngr.createQuery("select sb from Submission sb");
    
    q.setFirstResult(offset);
    q.setMaxResults(blockSz);
    
    List<Submission> res = q.getResultList();
    
    if( res.size() == 0 )
     break;
    
    for( Submission s : res )
    {
     fixFileType( s, s.getRootSection() );
    }
    
    t.commit();
    
    out.append( "Processed "+offset+" to "+(offset+res.size())+"\n" );
    
    offset += res.size();
   }
   
  }
  catch(Exception e)
  {
   e.printStackTrace();
   e.printStackTrace(out);
  }
  finally
  {
   if( mngr != null && mngr.isOpen() )
    mngr.close();
  }
  
  out.append( "Finished");
  
 }

 private void fixFileType(Submission s, Section sec)
 {
  if( sec.getSections() != null )
  {
   for( Section ss : sec.getSections() )
    fixFileType(s, ss);
  }
  
  if( sec.getFileRefs() != null )
  {
   Path filesPath = BackendConfig.getSubmissionFilesPath(s);
   
   for( FileRef fr : sec.getFileRefs() )
   {
    Path p = filesPath.resolve(fr.getName());
    fr.setDirectory( Files.isDirectory(p));
   }
  }
 }

}
