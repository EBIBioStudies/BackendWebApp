package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.HttpReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

/**
 * Servlet implementation class AuthServlet
 */
public class AuthServlet extends HttpServlet 
{
 
 private static final long serialVersionUID = 1L;
 
 public static final String ActionParameter="action"; 	
 public static final String SessionIdParameter="sessid";   
 public static final String UserLoginParameter="login";   
 public static final String PasswordParameter="password";   
 public static final String EmailParameter="email";   
 public static final String UsernameParameter="username";   
 public static final String FormatParameter="format";   
 public static final String ReCaptchaChallengeParameter="recaptcha_challenge";   
 public static final String ReCaptchaResponseParameter="recaptcha_response";   
	
 /**
  * @see HttpServlet#HttpServlet()
  */
 public AuthServlet()
 {
  super();
  // TODO Auto-generated constructor stub
 }

 @Override
 public void init(ServletConfig config) throws ServletException
 {
  super.init(config);
  
//  System.out.println( "Contex path: " + config.getServletContext().getContextPath() );
 }
 
 public static String getCookieSessId( HttpServletRequest req )
 {
  Cookie[] cuks = req.getCookies();
  
  if( cuks!= null && cuks.length != 0)
  {
   for (int i = cuks.length - 1; i >= 0; i--)
   {
    if (cuks[i].getName().equals(BackendConfig.SessionCookie) )
     return cuks[i].getValue();
   }
  }

  return null;
 }
 
 
 private void process( Action act, ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  String prm = prms.getParameter(ActionParameter);
  
  if( prm != null )
  {
   try
   {
    act = Action.valueOf(prm);
   }
   catch( Throwable e )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid '"+prm+"' parameter value: "+prm);
    
    return;
   }
  }
  
  SessionManager sessMngr = BackendConfig.getServiceManager().getSessionManager();
  
  if( act == Action.check )
  {
   prm = prms.getParameter(SessionIdParameter);
   
   if( prm == null )
   {
    prm  = getCookieSessId(request);
    
    if( prm == null )
    {
     resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Can't find session id");

     return;
    }
   }
   
   Session sess = sessMngr.getSession(prm);
   
   if( sess != null )
   {
    resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(UserLoginParameter,sess.getUser().getLogin()), new KV(UsernameParameter,sess.getUser().getFullName()) );
    return;
   }
   
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not logged in");

   return;
  }
  else if( act == Action.signin )
  {
   prm = prms.getParameter(UserLoginParameter);
   
   if( prm == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+UserLoginParameter+"' parameter is not defined");

    return;
   }

   
   User usr = BackendConfig.getServiceManager().getUserManager().getUserByName(prm);
   
   if( usr == null )
    usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(prm);
   
   if( usr == null )
   {
    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL login failed");

    return;
   }

   prm = prms.getParameter(PasswordParameter);
   
   if( prm == null )
    prm = "";
    
   if( ! usr.checkPassword(prm) )
   {
    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL login failed");

    return;
   }

   Session sess = sessMngr.getSessionByUser(usr.getLogin());
   
   if( sess == null )
    sess = sessMngr.createSession(usr);    
   
   String skey = sess.getSessionKey();
   
   Cookie cke =  new Cookie(BackendConfig.SessionCookie, skey);
   cke.setPath(getServletContext().getContextPath());
   
   resp.addCookie( cke );
   
   resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(SessionIdParameter, skey), new KV(UsernameParameter,usr.getFullName()));
   
   return;
  }
  else if( act == Action.signout )
  {
   prm = prms.getParameter(SessionIdParameter);
   
   if( prm == null )
   {
    prm  = getCookieSessId(request);
    
    if( prm == null )
    {
     resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Can't find session id");

     return;
    }
   }
   
   if( sessMngr.closeSession(prm) )
   {
    resp.respond(HttpServletResponse.SC_OK, "OK", "User logged out");
    return;
   }
   
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not logged in");

   return;
  }
  else if( act == Action.signup )
  {
   String login = prms.getParameter(UserLoginParameter);
   
   if( login == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+UserLoginParameter+"' parameter is not defined");

    return;
   }
  
   User usr = null;
   
   usr = BackendConfig.getServiceManager().getUserManager().getUserByName(login);
   
   if( usr != null )
   {
    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User exists");

    return;
   }
   
   String email = prms.getParameter(EmailParameter);
  
   if( email == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+EmailParameter+"' parameter is not defined");

    return;
   }
   
   usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);
   
   if( usr != null )
   {
    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User with email address '"+email+"' exists");

    return;
   }



   String pass = prms.getParameter(PasswordParameter);
   
   if( pass == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+PasswordParameter+"' parameter is not defined");

    return;
   }
   
   String cptChal = prms.getParameter(ReCaptchaChallengeParameter);
   
   if( cptChal == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+ReCaptchaChallengeParameter+"' parameter is not defined");

    return;
   }

   String cptResp = prms.getParameter(ReCaptchaResponseParameter);
   
   if( cptResp == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+ReCaptchaResponseParameter+"' parameter is not defined");

    return;
   }

   
   ReCaptcha reCaptcha = ReCaptchaFactory.newReCaptcha("", BackendConfig.getRecapchaPrivateKey(), false);
   
   ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(prms.getClientAddress(), cptChal, cptResp);

   if ( !reCaptchaResponse.isValid() ) 
   {
    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL CAPTCHA", "Captcha response is not valid");

    return;
   }

   
   
   User u = new User();
   
   u.setLogin(login);
   u.setEmail(email);
   u.setPassword(pass);
   u.setFullName(prms.getParameter(UsernameParameter));
   
   try
   {
    BackendConfig.getServiceManager().getUserManager().addUser(u);
   }
   catch( Throwable t )
   {
    resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Add user error: "+t.getMessage());

    return;
   } 
   
   resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(UsernameParameter,u.getFullName()));
//   resp.respond(HttpServletResponse.SC_OK, "OK");
   
  }
   
 }
 
 /**
  * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  Action act = Action.check;
  

  String pi = request.getPathInfo();

  if( pi != null && pi.length() > 1 )
  {
   try
   {
    act = Action.valueOf(pi.substring(1));
   }
   catch( Throwable e )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid path: "+pi);
    return;
   }
  }


  boolean jsonReq = request.getContentType()!=null && request.getContentType().startsWith("application/json");
  
  Response resp = null;
  
  String prm = request.getParameter(FormatParameter);
  
  if( "json".equals(prm) )
   resp = new JSONHttpResponse(response);
  else if( "text".equals(prm) )
   resp = new TextHttpResponse(response);
  else if( jsonReq )
   resp = new JSONHttpResponse(response);
  else
   resp = new TextHttpResponse(response);

  ParameterPool params = null;
  
  if( jsonReq )
  {
   Charset cs = Charset.defaultCharset();
   
   String enc = request.getCharacterEncoding();
   
   if( enc != null )
   {
    try
    {
     cs = Charset.forName(enc);
    }
    catch( Exception e )
    {}
   }
   
   String json = StringUtils.readFully(request.getInputStream(), cs);
   
   if( json.length() == 0 )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Empty JSON request body");
    return;
   }
   
   try
   {
    params = new JSONReqParameterPool(json, request.getRemoteAddr());
   }
   catch( Exception e )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid JSON request body");
    return;
   }
  }
  else
   params = new HttpReqParameterPool(request);
  
  process(act, params, request, resp);  

 }

}
