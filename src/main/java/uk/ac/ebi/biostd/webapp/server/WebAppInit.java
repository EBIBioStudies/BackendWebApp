package uk.ac.ebi.biostd.webapp.server;

import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.webresources.StandardRoot;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.mng.ServiceManager;
import uk.ac.ebi.biostd.mng.UserManager;
import uk.ac.ebi.biostd.webapp.server.config.AppConfig;
import uk.ac.ebi.biostd.webapp.server.webdav.ContextWrapper;
import uk.ac.ebi.biostd.webapp.server.webdav.WebdavServlet;

/**
 * Application Lifecycle Listener implementation class WebAppInit
 *
 */
@WebListener
public class WebAppInit implements ServletContextListener {

 
 /**
  * Default constructor.
  */
 public WebAppInit()
 {
 }

 /**
  * @see ServletContextListener#contextDestroyed(ServletContextEvent)
  */
 @Override
 public void contextDestroyed(ServletContextEvent arg0)
 {
 }

 /**
  * @see ServletContextListener#contextInitialized(ServletContextEvent)
  */
 @Override
 public void contextInitialized(ServletContextEvent ctxEv)
 {
  ServletContext ctx = ctxEv.getServletContext();
  
  WebResourceRoot resRoot = null;
  
  try
  {
   resRoot = (WebResourceRoot)ctx.getAttribute("org.apache.catalina.resources");
  }
  catch(Throwable e)
  {
   throw new RuntimeException("Can't find WebResourceRoot. Not Tomcat 8?");
  }
  
  String dataDir = ctx.getInitParameter(AppConfig.DataDirParameter);
  String dataMount = ctx.getInitParameter(AppConfig.DataMountPathParameter);
  
  if( dataDir == null )
   throw new RuntimeException(AppConfig.DataDirParameter+" parameter is not set");

  if( dataMount == null )
   throw new RuntimeException(AppConfig.DataMountPathParameter+" parameter is not set");
  
  resRoot.createWebResourceSet(ResourceSetType.POST, dataMount, dataDir, null, "/");
  
  StandardRoot davRoot = new StandardRoot( new ContextWrapper(resRoot.getContext(), dataDir) );
  davRoot.setCachingAllowed(false);
  
  try
  {
   davRoot.start();
  }
  catch(LifecycleException e)
  {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
  
  ctx.setAttribute("davRoot", davRoot);
  
  ServletRegistration.Dynamic dn = ctx.addServlet("WebDAV",WebdavServlet.class);

  dn.setAsyncSupported(true);
  dn.setInitParameter(AppConfig.DataMountPathParameter, dataMount);
  dn.setInitParameter("listings", "true");
  dn.setInitParameter("readonly", "false");
  dn.addMapping(dataMount.endsWith("/")?dataMount+'*':dataMount+"/*");
  
  
  
//  Enumeration<String> nms = ctx.getAttributeNames();
//  
//  while( nms.hasMoreElements() )
//  {
//   String nm = nms.nextElement();
//   
//   String val = String.valueOf( ctx.getAttribute(nm) );
//   
//   if( val.length() > 70 )
//    val = val.substring(0,70)+"...";
//   
//   System.out.println(nm+"="+val);
//  }
  
  final User u = new User();
  
  u.setId(1);
  u.setLogin("mike");
  u.setPassword("mike");
  
  final UserGroup g = new UserGroup();
  g.setName("EBI");
  g.setId(1);
  g.setProject(true);
  
  u.setGroups(new ArrayList<UserGroup>(){{add(g);}});
  
  final UserManager um = new UserManager()
  {
   User uMike = u;
   
   @Override
   public User getUserByName(String uName)
   {
    if( uName.equals(uMike.getLogin()))
     return uMike;
    
    return null;
   }
  };
  
  AppConfig.setServiceManager( new ServiceManager()
  {
   
   @Override
   public UserManager getUserManager()
   {
    return um;
   }
  });
  
 }
	
}
