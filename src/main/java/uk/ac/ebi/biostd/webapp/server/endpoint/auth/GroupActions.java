package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.Response;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityManager;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

public class GroupActions
{
 private static Logger log = LoggerFactory.getLogger(GroupActions.class);
 
 static void remUserFromGroup(ParameterPool params, HttpServletRequest request, Response resp, Session sess) throws IOException
 {
  changeGroup(params, request, resp, sess, false);
 }

 static void addUserToGroup(ParameterPool params, HttpServletRequest request, Response resp, Session sess) throws IOException
 {
  changeGroup(params, request, resp, sess, true);
 }
 
 static void changeGroup (ParameterPool params, HttpServletRequest request, Response resp, Session sess, boolean add) throws IOException
 {
  if(sess == null)
  {
   resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
   return;
  }

  String grName = params.getParameter(AuthServlet.GroupParameter);
  
  if( grName == null || (grName=grName.trim()).length() == 0 )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+AuthServlet.GroupParameter+"' parameter is not defined");
   return;
  }

  String uName = params.getParameter(AuthServlet.UserParameter);
  
  if( uName == null || (uName=uName.trim()).length() == 0 )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+AuthServlet.UserParameter+"' parameter is not defined");
   return;
  }

  SecurityManager scMgr = BackendConfig.getServiceManager().getSecurityManager();
  
  UserGroup grp = scMgr.getGroup(grName);
  
  if( grp == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid group");
   return;
  }
  
  
  User usr = sess.getUser();

  if(!usr.isSuperuser() && !scMgr.mayUserChangeGroup(usr,grp))
  {
   resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "Permission denied");
   return;
  }
  
  User uToAdd = scMgr.getUserByEmail(uName);
  
  if( uToAdd == null )
   uToAdd = scMgr.getUserByLogin(uName);
  
  if( uToAdd == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid user");
   return;
  }

  boolean res;
  
  try
  {
   if( add )
    res = scMgr.addUserToGroup(uToAdd,grp);
   else
    res = scMgr.removeUserFromGroup(uToAdd,grp);
  }
  catch(Exception e)
  {
   log.error("Change group operation failed: "+e.getMessage());
   
   resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "Internal server error");
   return;
  }
  
  if( ! res )
  {
   if( add )
    resp.respond(HttpServletResponse.SC_OK, "FAIL", "Already in group");
   else
    resp.respond(HttpServletResponse.SC_OK, "FAIL", "Not in group");
  }
  else
   resp.respond(HttpServletResponse.SC_OK, "OK", "Operation successful");
   
 }

 static void createGroup(ParameterPool prms, HttpServletRequest request, Response resp, Session sess) throws IOException
 {
  if(sess == null)
  {
   resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");
   return;
  }

  User usr = sess.getUser();

  if(!usr.isSuperuser() && !BackendConfig.getServiceManager().getSecurityManager().mayUserCreateGroup(usr))
  {
   resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "Permission denied");
   return;
  }

  String grName = prms.getParameter(AuthServlet.ProjectParameter);
  boolean isProject = "1".equals(grName) || "true".equalsIgnoreCase(grName) || "yes".equalsIgnoreCase(grName);
  
  grName = prms.getParameter(AuthServlet.NameParameter);
  String grDesc = prms.getParameter(AuthServlet.DescriptionParameter);
  
  if( grName == null || (grName=grName.trim()).length() == 0 )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+AuthServlet.NameParameter+"' parameter is not defined");
   return;
  }
  
  if( BackendConfig.getServiceManager().getUserManager().getGroup(grName) != null )
  {
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Group exits");
   return;
  }
  
  UserGroup ug = new UserGroup();
  
  ug.setName(grName);
  ug.setDescription(grDesc);
  ug.setProject(isProject);
  ug.setOwner(usr);
  
  
  try
  {
   BackendConfig.getServiceManager().getUserManager().addGroup(ug);
  }
  catch( Throwable t )
  {
   resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Add group error: "+t.getMessage());

   if( t instanceof NullPointerException )
    t.printStackTrace();
   
   return;
  } 
  
  resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(AuthServlet.NameParameter,grName));
  
 }

}
