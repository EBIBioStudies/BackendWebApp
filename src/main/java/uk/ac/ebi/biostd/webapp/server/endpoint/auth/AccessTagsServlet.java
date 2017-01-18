package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.authz.ACR.Permit;
import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.BuiltInUsers;
import uk.ac.ebi.biostd.authz.SystemAction;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

import com.pri.util.StringUtils;

public class AccessTagsServlet extends HttpServlet
{
 public static final String loginParameter = "login"; 
 public static final String passwordParameter = "password"; 
 public static final String hashParameter = "hash"; 

 private static final long serialVersionUID = 1L;

 @SuppressWarnings("unchecked")
 @Override
 protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
  String login = null;
  String pass = null;
  String hash = null;
  
  if( "text/plain".equalsIgnoreCase(req.getContentType()) )
  {
   BufferedReader reader = req.getReader();
   
   String line = null;
   
   while( (line=reader.readLine()) != null )
   {
    int pos = line.indexOf(":");
    
    if( pos < 0 )
     continue;
    
    String pname = line.substring(0,pos).trim();
    String pval = line.substring(pos+1).trim();
    
    if( loginParameter.equals(pname) )
     login = pval;
    else if( passwordParameter.equals(pname) )
     pass = pval;
    else if( hashParameter.equals(pname) )
     hash = pval;
   }
  }
  else
  {
   login = req.getParameter(loginParameter);
   pass = req.getParameter(passwordParameter);
   hash = req.getParameter(hashParameter);
  }
  
  if( pass != null && pass.length() == 0 )
   pass=null;
  
  if( hash != null && hash.length() == 0 )
   hash=null;
  
  if( login == null || (pass == null && hash == null && ! BuiltInUsers.Guest.getUserName().equals(login)) )
  {
   resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   resp.getWriter().println("Status: INVALID REQUEST");
   return;
  }
  
  
  EntityManager em = null;
  
  try
  {
   em = BackendConfig.getEntityManagerFactory().createEntityManager();
   
   Query q = em.createNamedQuery(User.GetByEMailQuery);
   
   q.setParameter("email", login);
   
   User u = null;
   
   try
   {
    u = (User)q.getSingleResult();
   }
   catch(Exception e)
   {
   }
   
   if( u == null )
   {
    q= em.createNamedQuery(User.GetByLoginQuery);
    
    q.setParameter("login", login);
    
    try
    {
     u = (User)q.getSingleResult();
    }
    catch(Exception e)
    {
     resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
     resp.getWriter().println("Status: FAILED");
     return;
    }

   }
   
   if( pass != null )
   {
    if( ! u.checkPassword(pass) )
    {
     resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
     resp.getWriter().println("Status: FAILED");
     return;
    }
   }
   else if( ! BuiltInUsers.Guest.getUserName().equals(login) && ( u.getPasswordDigest() == null || ! hash.equalsIgnoreCase( StringUtils.toHexStr(u.getPasswordDigest() ) ) ) )
   {
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    resp.getWriter().println("Status: FAILED");
    return;
   }
  
   StringBuilder allow = new StringBuilder();
   StringBuilder deny = new StringBuilder();
   
   allow.append('~').append(u.getEmail()).append(';');
   
   allow.append('#').append(u.getId()).append(';');
   
   q = em.createQuery("SELECT t FROM AccessTag t");
   
   for( AccessTag t : (List<AccessTag>)q.getResultList() )
   {
    Permit p = t.checkDelegatePermission(SystemAction.READ, u);
    
    if( p == Permit.ALLOW )
     allow.append(t.getName()).append(';');
    else if( p == Permit.DENY )
     deny.append(t.getName()).append(';');
   }
   
   allow.setLength( allow.length()-1);

   if( deny.length() > 0 )
    deny.setLength( deny.length()-1);
   
   resp.getWriter().println("Status: OK");
   resp.getWriter().println("Allow: "+allow.toString());
   resp.getWriter().println("Deny: "+deny.toString());
   resp.getWriter().println("Superuser: "+(u.isSuperuser()?"true":"false"));
   
  }
  catch(Exception e)
  {
   resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
   resp.getWriter().println("Status: SERVER ERROR");
  }
  finally
  {
   if( em != null )
    em.close();
  }
  
 }
 

 
}
