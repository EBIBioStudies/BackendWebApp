package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.HttpReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.JSONReqParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityException;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.KeyExpiredException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.SystemUserMngException;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

/**
 * Servlet implementation class AuthServlet
 */
public class AuthServlet extends ServiceServlet 
{
 private static Logger log;
 
 private static final long serialVersionUID = 1L;
 
 public static final String ActionParameter="action"; 	
 public static final String SessionIdParameter="sessid";   
 public static final String UserLoginParameter="login";   
 public static final String UserEmailParameter="email";   
 public static final String PasswordParameter="password";   
 public static final String UsernameParameter="username";   
 public static final String FormatParameter="format";   
 public static final String ReCaptchaChallengeParameter="recaptcha_challenge";   
 public static final String ReCaptchaResponseParameter="recaptcha_response";   
 public static final String ReCaptcha2ResponseParameter=BackendConfig.googleClientResponseParameter;
 public static final String ActivationURLParameter = "activationURL";   
 public static final String PassResetURLParameter = "resetURL";   
 public static final String SuccessURLParameter = "successURL";   
 public static final String FailURLParameter = "failURL";   
 public static final String ResetKeyParameter = "key";   
  
	
 /**
  * @see HttpServlet#HttpServlet()
  */
 public AuthServlet()
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());
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
 
 
 private void process( Action act, ParameterPool prms, HttpServletRequest request, HttpServletResponse response, Response resp) throws IOException
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
    User u = sess.getUser();
    
    KV[] vars = null;
    if( u.getLogin() != null )
     vars = new KV[]{new KV(UserLoginParameter,u.getLogin()), new KV(UserEmailParameter,u.getEmail()), new KV(UsernameParameter,u.getFullName())};
    else
     vars = new KV[]{new KV(UserEmailParameter,u.getEmail()), new KV(UsernameParameter,u.getFullName())};
    
    resp.respond(HttpServletResponse.SC_OK, "OK", null, vars );
    return;
   }
   
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not logged in");

   return;
  }
  else if( act == Action.signin )
   signin(prms, resp);
  else if( act == Action.signout )
   signout(prms, request, resp);
  else if( act == Action.signup )
   signup(prms, request, resp);
  else if( act == Action.activate )
   activate(prms, request, response, resp);
  else if( act == Action.retryact )
   retryActivation(prms, request, resp);
  else if( act == Action.passrstreq )
   passwordResetRequest(prms, request, resp);
  else if( act == Action.passreset )
   passwordReset(prms, request, resp);
 }


 /**
  * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void service(HttpServletRequest request, HttpServletResponse response, Session sess) throws ServletException, IOException
 {
  Action act = Action.check;
  

  String pi = request.getPathInfo();

  
  if( pi != null && pi.length() > 1 )
  {
   int lastSlsh = pi.lastIndexOf('/');
   
   if( lastSlsh <= 0 )
    lastSlsh=pi.length();
   
   String actstr = pi.substring(1,lastSlsh);

   try
   {
    act = Action.valueOf(actstr);
   }
   catch( Throwable e )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Invalid path: "+pi);
    return;
   }
  }

  boolean jsonReq = false;
  
  String cType = request.getContentType();
  
  if( cType != null )
  {
   int pos = cType.indexOf(';');
   
   if( pos > 0 )
    cType = cType.substring(0,pos).trim();

   jsonReq = cType.equalsIgnoreCase("application/json");
  }

  
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
   
   String reqBody = null;
   
  
   reqBody = StringUtils.readFully(request.getInputStream(), cs);
  
   if( reqBody == null || reqBody.length() == 0 )
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().print("FAIL Empty JSON request body");
    return;
   }
   
   try
   {
    params = new JSONReqParameterPool(reqBody, request.getRemoteAddr());
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
  
  process(act, params, request, response, resp);  

 }
 
 
 private void signin( ParameterPool prms, Response resp) throws IOException
 {
  
  Session sess;
  try
  {
   sess = BackendConfig.getServiceManager().getUserManager().login( prms.getParameter(UserLoginParameter), prms.getParameter(PasswordParameter));
  }
  catch(SecurityException e)
  {
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "FAIL "+e.getMessage());
   return;
  }
  
  
  String skey = sess.getSessionKey();
  
  Cookie cke =  new Cookie(BackendConfig.SessionCookie, skey);
  cke.setPath(getServletContext().getContextPath());
  
  resp.addCookie( cke );
  
  resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(SessionIdParameter, skey), new KV(UsernameParameter,sess.getUser().getFullName()));
 }
 
 
 private void signout( ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  String prm = prms.getParameter(SessionIdParameter);
  
  if( prm == null )
  {
   prm  = getCookieSessId(request);
   
   if( prm == null )
   {
    resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Can't find session id");

    return;
   }
  }
  
  if(  BackendConfig.getServiceManager().getSessionManager().closeSession(prm) )
  {
   resp.respond(HttpServletResponse.SC_OK, "OK", "User logged out");
   return;
  }
  
  resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not logged in");
 }
 
 
 private void signup( ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  User usr = null;

  String login = prms.getParameter(UserLoginParameter);
  
  if( login != null )
  {
   login = login.trim();
   
   if( login.length() == 0 )
    login = null;
   else
   {
    if( login.indexOf('@') >= 0 )
    {
     resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Character @ in allowed in login");

     return;
    }
     
    
    usr = BackendConfig.getServiceManager().getUserManager().getUserByLogin(login);
    
    if( usr != null )
    {
     resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Login is taken by another user");

     return;
    }

   }
  }
  
  String email = prms.getParameter(UserEmailParameter);
  
  if( email == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+UserEmailParameter+"' parameter is not defined");

   return;
  }
 
  email = email.trim();
  
  if( ! checkRecaptchas(prms, request, resp, null) )
   return;

  
  if( ! EmailValidator.getInstance(false).isValid(email) )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Email address in not valid");

   return;
  }
  
  usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);
  
  if( usr != null )
  {
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Email is taken by another user");

   return;
  }
  
  String pass = prms.getParameter(PasswordParameter);
  
  if( pass == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+PasswordParameter+"' parameter is not defined");

   return;
  }
  
  if( pass.length() < 6 )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+PasswordParameter+"' passowrd must be 6 symbols at least");

   return;
  }

  
  String actvURL = null;
  
  if( BackendConfig.isEnableUnsafeRequests() )
   actvURL =  prms.getParameter(ActivationURLParameter);
  
  
  User u = new User();
  
  u.setLogin(login);
  u.setEmail(email);
  u.setPassword(pass);
  u.setFullName(prms.getParameter(UsernameParameter));
  
  try
  {
   BackendConfig.getServiceManager().getUserManager().addUser(u, BackendConfig.isMandatoryAccountActivation(), actvURL);
  }
  catch( Throwable t )
  {
   resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Add user error: "+t.getMessage());

   return;
  } 
  
  resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(UsernameParameter,u.getFullName()));
//  resp.respond(HttpServletResponse.SC_OK, "OK");

 }
 
 
 private void activate( ParameterPool prms, HttpServletRequest request, HttpServletResponse response, Response resp) throws IOException
 {
  String actKey = request.getPathInfo();
  

  String succURL = null;
  String failURL = null;

  if( BackendConfig.isEnableUnsafeRequests() )
  {
   succURL = request.getParameter(SuccessURLParameter);
   failURL = request.getParameter(FailURLParameter);
  }
  
  actKey = actKey.substring(actKey.lastIndexOf('/')+1);
  
  if( actKey == null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);
   
   return;
  }

  ActivationInfo ainf = AccountActivation.decodeActivationKey(actKey);
  
  if( ainf == null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request",failURL);

   return;
  }
  
  String msg = null;
  
  try
  {
   BackendConfig.getServiceManager().getUserManager().activateUser( ainf );
  }
  catch( KeyExpiredException e)
  {
   msg = "Activation key has expired";
  }
  catch( SystemUserMngException e)
  {
   msg = "System error";
   
   log.error("System error : "+e.getCause().getMessage(),e);
  }
  catch( Exception e)
  {
   msg = "Invalid request";
  }
  
  if( msg != null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);
   
   return;
  }
  
  
  resp.respondRedir(HttpServletResponse.SC_OK, "OK", "User successfully activated. You can log in now",succURL);

 }
 
 private void retryActivation( ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  String email = prms.getParameter(UserEmailParameter);
  
  if( email == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+UserEmailParameter+"' parameter is not defined");

   return;
  }
 
  email = email.trim();
  
  
  if( ! checkRecaptchas(prms, request, resp, null) )
   return;


  User usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);
  
  if( usr == null )
  {
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account doesn't exist");

   return;
  }
  
  if( usr.isActive() || usr.getActivationKey() == null )
  {
   resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account is active");
   
   return;
  }

  String actvURL = null;
  
  if( BackendConfig.isEnableUnsafeRequests() )
   actvURL =  prms.getParameter(ActivationURLParameter);

  if( !  AccountActivation.sendActivationRequest(usr, UUID.fromString(usr.getActivationKey()) , actvURL) )
  {
   resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Can't send activation email");
   
   return;
  }
  else
   resp.respond(HttpServletResponse.SC_OK, "OK", "Activation request email has been sent");

 }

 private void passwordResetRequest(ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  String email = prms.getParameter(UserEmailParameter);
  
  String succURL = null;
  String failURL = null;

  if( BackendConfig.isEnableUnsafeRequests() )
  {
   succURL = prms.getParameter(SuccessURLParameter);
   failURL = prms.getParameter(FailURLParameter);
  }
  
  if( email == null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+UserEmailParameter+"' parameter is not defined",failURL);

   return;
  }
 
  email = email.trim();
  
  if( ! checkRecaptchas(prms, request, resp, failURL) )
   return;
  

  User usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);
  
  if( usr == null )
  {
   resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account doesn't exist", failURL);

   return;
  }
  
  if( ! usr.isActive() )
  {
   resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account is not active", failURL);
   
   return;
  }
  
  String actvURL = null;
  
  if( BackendConfig.isEnableUnsafeRequests() )
   actvURL =  prms.getParameter(PassResetURLParameter);
  
  String msg = null;
  
  try
  {
   BackendConfig.getServiceManager().getUserManager().passwordResetRequest( usr, actvURL );
   
   resp.respondRedir(HttpServletResponse.SC_OK, "OK", "Password reset request email has been sent",succURL);

  }
  catch( SystemUserMngException e)
  {
   msg = "System error";
   
   log.error("System error : "+e.getCause().getMessage(),e);
  }
  catch( Exception e)
  {
   msg = "Invalid request";
  }
  
  if( msg != null )
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);

 }
 
 private boolean checkRecaptchas(ParameterPool prms, HttpServletRequest request, Response resp, String failURL) throws IOException
 {
  String cptResp = prms.getParameter(ReCaptcha2ResponseParameter);
  
  if( cptResp != null )
  {
   
   if ( !checkRecaptcha2(cptResp, request.getRemoteAddr()) ) 
   {
    resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL CAPTCHA", "Captcha response is not valid", failURL);

    return false;
   }
   
  }
  else
  {
   cptResp = prms.getParameter(ReCaptchaResponseParameter);
   
   if( cptResp == null )
   {
    resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+ReCaptchaResponseParameter+"' parameter is not defined", failURL);
    
    return false;
   }
   
   String cptChal = prms.getParameter(ReCaptchaChallengeParameter);
   
   if( cptChal == null )
   {
    resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+ReCaptchaChallengeParameter+"' parameter is not defined", failURL);
    
    return false;
   }
   
   
   
   ReCaptcha reCaptcha = ReCaptchaFactory.newReCaptcha("", BackendConfig.getRecapchaPrivateKey(), false);
   
   ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(prms.getClientAddress(), cptChal, cptResp);
   
   if ( !reCaptchaResponse.isValid() ) 
   {
    resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL CAPTCHA", "Captcha response is not valid",failURL);
    
    return false;
   }
  }
  
  return true;
 }

 private void passwordReset(ParameterPool prms, HttpServletRequest request, Response resp) throws IOException
 {
  String actKey = prms.getParameter(ResetKeyParameter);
  

  String succURL = null;
  String failURL = null;

  if( BackendConfig.isEnableUnsafeRequests() )
  {
   succURL = prms.getParameter(SuccessURLParameter);
   failURL = prms.getParameter(FailURLParameter);
  }
  
//  actKey = actKey.substring(actKey.lastIndexOf('/')+1);
  
  if( ! checkRecaptchas(prms, request, resp, failURL) )
   return;

  
  if( actKey == null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);
   
   return;
  }

  ActivationInfo ainf = AccountActivation.decodeActivationKey(actKey);
  
  if( ainf == null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request",failURL);

   return;
  }
  
  String pass = prms.getParameter(PasswordParameter);
  
  if( pass == null )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+PasswordParameter+"' parameter is not defined");

   return;
  }
  
  if( pass.length() < 6 )
  {
   resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "'"+PasswordParameter+"' passowrd must be 6 symbols at least");

   return;
  }
  
  String msg = null;
  
  try
  {
   BackendConfig.getServiceManager().getUserManager().resetPassword( ainf, pass );
  }
  catch( KeyExpiredException e)
  {
   msg = "Reset key has expired";
  }
  catch( SystemUserMngException e)
  {
   msg = "System error";
   
   log.error("System error : "+e.getCause().getMessage(),e);
  }
  catch( Exception e)
  {
   msg = "Invalid request";
  }
  
  if( msg != null )
  {
   resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);
   
   return;
  }
  
  resp.respondRedir(HttpServletResponse.SC_OK, "OK", "User password has been reset.",succURL);
 }
 
 private boolean checkRecaptcha2( String resp, String cliIP)
 {
  try
  {
   URL url = new URL(BackendConfig.googleVerifyURL);

   StringBuilder postData = new StringBuilder();

   postData.append(URLEncoder.encode(BackendConfig.googleSecretParam, "UTF-8"));
   postData.append('=');
   postData.append(URLEncoder.encode(BackendConfig.getRecapchaPrivateKey(), "UTF-8"));

   postData.append('&');
   postData.append(URLEncoder.encode(BackendConfig.googleResponseParam, "UTF-8"));
   postData.append('=');
   postData.append(URLEncoder.encode(resp, "UTF-8"));

   postData.append('&');
   postData.append(URLEncoder.encode(BackendConfig.googleRemoteipParam, "UTF-8"));
   postData.append('=');
   postData.append(URLEncoder.encode(cliIP, "UTF-8"));

   byte[] postDataBytes = postData.toString().getBytes("UTF-8");

   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
   conn.setRequestMethod("POST");
   conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
   conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
   conn.setDoOutput(true);
   conn.getOutputStream().write(postDataBytes);

   JSONObject js = new JSONObject(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8) );
   
   conn.disconnect();
   
   Object succ = js.opt(BackendConfig.googleSuccessField);
   
   if( succ != null && succ instanceof Boolean )
    return ((Boolean)succ).booleanValue();
   else
    log.error("Google returned invalid JSON while checking recaptcha: invalid 'success' field");
  }
  catch(IOException e)
  {
   log.error("IO error while checking recaptcha: "+e.getMessage());
  }
  catch(JSONException e)
  {
   log.error("Google returned invalid JSON while checking recaptcha: "+e.getMessage());
  }
  
  return false;
 }
}
