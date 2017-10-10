/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute <p> Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at <p> http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ParameterPool;
import uk.ac.ebi.biostd.webapp.server.endpoint.ReqResp;
import uk.ac.ebi.biostd.webapp.server.endpoint.Response;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation;
import uk.ac.ebi.biostd.webapp.server.mng.AccountActivation.ActivationInfo;
import uk.ac.ebi.biostd.webapp.server.mng.UserAuxXMLFormatter;
import uk.ac.ebi.biostd.webapp.server.mng.UserManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.KeyExpiredException;
import uk.ac.ebi.biostd.webapp.server.mng.exception.SystemUserMngException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;
import uk.ac.ebi.biostd.webapp.shared.util.KV;

/**
 * Servlet implementation class AuthServlet
 */
public class AuthServlet extends ServiceServlet {

    public static final String ActionParameter = "action";
    public static final String NameParameter = "name";
    public static final String ProjectParameter = "project";
    public static final String DescriptionParameter = "description";
    public static final String SessionIdParameter = "sessid";
    public static final String UserLoginParameter = "login";
    public static final String UserEmailParameter = "email";
    public static final String SuperuserParameter = "superuser";
    public static final String PasswordParameter = "password";
    public static final String SSOTokenParameter = "ssotoken";
    public static final String PasswordHashParameter = "passhash";
    public static final String UsernameParameter = "username";
    public static final String DropboxParameter = "dropbox";
    public static final String FormatParameter = "format";
    public static final String ReCaptchaChallengeParameter = "recaptcha_challenge";
    public static final String ReCaptchaResponseParameter = "recaptcha_response";
    public static final String ReCaptcha2ResponseParameter = BackendConfig.googleClientResponseParameter;
    public static final String ActivationURLParameter = "activationURL";
    public static final String PassResetURLParameter = "resetURL";
    public static final String SuccessURLParameter = "successURL";
    public static final String FailURLParameter = "failURL";
    public static final String ResetKeyParameter = "key";
    public static final String AuxParameter = "aux";
    public static final String UserParameter = "user";
    public static final String GroupParameter = "group";
    public static final String AuxParameterSeparator = ":";
    private static final long serialVersionUID = 1L;
    private static final String SSOnickname = "nickname";
    private static final String SSOname = "name";
    private static final String SSOemail = "email";
    private static Logger log;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public AuthServlet() {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }
        // TODO Auto-generated constructor stub
    }

    public static String getCookieSessId(HttpServletRequest req) {
        String sessId = req.getHeader(BackendConfig.getSessionTokenHeader());

        if (sessId != null) {
            return sessId;
        }

        Cookie[] cuks = req.getCookies();

        if (cuks != null && cuks.length != 0) {
            for (int i = cuks.length - 1; i >= 0; i--) {
                if (cuks[i].getName().equals(BackendConfig.getSessionCookieName())) {
                    return cuks[i].getValue();
                }
            }
        }

        return null;
    }

    // SSO stuff

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

//  System.out.println( "Contex path: " + config.getServletContext().getContextPath() );
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response, Session sess)
            throws ServletException, IOException {
        Action act = Action.check;

        String pi = request.getPathInfo();

        if (pi != null && pi.length() > 1) {
            int lastSlsh = pi.lastIndexOf('/');

            if (lastSlsh <= 0) {
                lastSlsh = pi.length();
            }

            String actstr = pi.substring(1, lastSlsh);

            try {
                act = Action.valueOf(actstr);
            } catch (Throwable e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain");
                response.getWriter().print("FAIL Invalid path: " + pi);
                return;
            }
        }

        ReqResp rqrs = new ReqResp(request, response);

        if (act == Action.check) {
            checkLoggedIn(rqrs);
        } else if (act == Action.signin) {
            signin(rqrs);
        } else if (act == Action.signout) {
            signout(rqrs);
        } else if (act == Action.signup) {
            signup(rqrs);
        } else if (act == Action.activate) {
            activate(rqrs);
        } else if (act == Action.retryact) {
            retryActivation(rqrs);
        } else if (act == Action.passrstreq) {
            passwordResetRequest(rqrs);
        } else if (act == Action.passreset) {
            passwordReset(rqrs);
        } else if (act == Action.creategroup) {
            GroupActions.createGroup(rqrs, sess);
        } else if (act == Action.removegroup) {
            GroupActions.removeGroup(rqrs, sess);
        } else if (act == Action.listgroups) {
            GroupActions.listGroups(rqrs, sess);
        } else if (act == Action.listgroup) {
            GroupActions.listGroupMembers(rqrs, sess);
        } else if (act == Action.addusertogroup) {
            GroupActions.addUserToGroup(rqrs, sess);
        } else if (act == Action.remuserfromgroup) {
            GroupActions.remUserFromGroup(rqrs, sess);
        }

    }

    private void checkLoggedIn(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String prm = prms.getParameter(SessionIdParameter);

        if (prm == null) {
            prm = getCookieSessId(rqrs.getHttpServletRequest());

            if (prm == null) {
                resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "Can't find session id");

                return;
            }
        }

        Session sess = BackendConfig.getServiceManager().getSessionManager().getSession(prm);

        if (sess == null) {
            resp.respond(HttpServletResponse.SC_UNAUTHORIZED, "FAIL", "User not logged in");

            return;
        }

        int auxLen = 0;

        User u = sess.getUser();
        String auxText = u.getAuxProfileInfo();
        List<String[]> aux = null;

        if (auxText != null && auxText.length() > 0) {
            aux = UserAuxXMLFormatter.readXML(auxText);
            auxLen = aux.size();
        }

        KV[] outInfo = new KV[4 + auxLen + (u.getLogin() != null ? 1 : 0)];

        int n = 0;

        outInfo[n++] = new KV(SessionIdParameter, sess.getSessionKey());
        outInfo[n++] = new KV(UsernameParameter, sess.getUser().getFullName());
        outInfo[n++] = new KV(UserEmailParameter, String.valueOf(sess.getUser().getEmail()));
        outInfo[n++] = new KV(SSOTokenParameter, String.valueOf(sess.getSSOToken()));

        if (u.getLogin() != null) {
            outInfo[n++] = new KV(UserLoginParameter, u.getLogin());
        }

        for (int i = 0; i < auxLen; i++) {
            outInfo[i + n] = new KV(AuxParameter, aux.get(i)[0], aux.get(i)[1]);
        }

        resp.respond(HttpServletResponse.SC_OK, "OK", null, outInfo); // safe for null emails
    }

    private Session signInUsingSSOToken(ReqResp rqrs, String ssoToken) throws Exception {
        Response resp = rqrs.getResponse();
        UserManager um = BackendConfig.getServiceManager().getUserManager();
        Session sess = null;

        DecodedJWT decodedToken = null;
        try {
            decodedToken = SSOSupport.verifyToken(ssoToken);
        } catch (Throwable t) {
            throw new SecurityException("Token verification failed: " + t.getMessage());
        }

        String ssoSubject = decodedToken.getSubject();
        User user = um.getUserBySSOSubject(ssoSubject);

        if (user == null) {
            // sso user not registered - register user
            //
            String ssoEmail = decodedToken.getClaim(SSOemail).asString();
            String ssoLogin = decodedToken.getClaim(SSOnickname).asString();
            String ssoName = decodedToken.getClaim(SSOname).asString();

            user = um.getUserByEmail(ssoEmail);

            if (user == null) {
                // user doesn't exist - create

                user = new User();

                user.setLogin(ssoLogin);
                user.setEmail(ssoEmail);
                user.setPassword(UUID.randomUUID().toString());
                user.setFullName(ssoName);

                try {
                    // create activated user
                    um.addUser(user, null, false, null);
                } catch (Throwable t) {
                    throw new SecurityException("Error when adding SSO user: " + t.getMessage());
                }

            } else {
                // user exists, add link
                //
                try {
                    um.linkSSOSubjectToUser(user, ssoSubject);
                } catch (Throwable t) {
                    throw new SecurityException("Error adding SSO link to existing account: " + t.getMessage());
                }
            }
        }

        // TODO: (pass in User to optimize)
        try {
            sess = um.loginUsingSSOToken(user, ssoToken, ssoSubject);
        } catch (Throwable t) {
            throw new SecurityException("Failed to login using SSO: " + t.getMessage());
        }

        return sess;
    }

    private void signin(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String ssoToken = prms.getParameter(SSOTokenParameter);
        Session sess = null;

        UserManager um = BackendConfig.getServiceManager().getUserManager();

        if (ssoToken != null) {

            try {
                sess = signInUsingSSOToken(rqrs, ssoToken);
            } catch (Exception ex) {
                resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "FAIL " + ex.getMessage());
                return;
            }

        } else {

            String uname = prms.getParameter(UserLoginParameter);

            if (uname == null) {
                resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "FAIL user name is not specified");
                return;
            }

            boolean passHash = false;

            String pass = prms.getParameter(PasswordParameter);

            if (pass == null || pass.length() == 0) {
                passHash = true;
                pass = prms.getParameter(PasswordHashParameter);
            }

            if (pass == null || pass.length() == 0) {
                resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "FAIL password has not been provided");
                return;
            }

            try {
                sess = um.login(uname, pass, passHash);
            } catch (SecurityException e) {

                resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "FAIL " + e.getMessage());
                return;
            }
        }

        String skey = sess.getSessionKey();

        Cookie cke = new Cookie(BackendConfig.getSessionCookieName(), skey);
        cke.setPath(getServletContext().getContextPath()); //Setting path is a right idea but doesn't work with proxies

        resp.addCookie(cke);

        int auxLen = 0;

        String auxText = sess.getUser().getAuxProfileInfo();
        List<String[]> aux = null;

        if (auxText != null && auxText.length() > 0) {
            aux = UserAuxXMLFormatter.readXML(auxText);
            auxLen = aux.size();
        }

        KV[] outInfo = new KV[6 + auxLen];

        outInfo[0] = new KV(SessionIdParameter, skey);
        outInfo[1] = new KV(UsernameParameter, sess.getUser().getFullName());
        outInfo[2] = new KV(UserEmailParameter, String.valueOf(sess.getUser().getEmail()));
        outInfo[3] = new KV(SuperuserParameter, sess.getUser().isSuperuser() ? "true" : "false");
        outInfo[4] = new KV(DropboxParameter, BackendConfig.getUserDropboxRelPath(sess.getUser()));
        outInfo[5] = new KV(SSOTokenParameter, String.valueOf(ssoToken)); // null -> "null" ?

        for (int i = 0; i < auxLen; i++) {
            outInfo[i + 6] = new KV(AuxParameter, aux.get(i)[0], aux.get(i)[1]);
        }

        resp.respond(HttpServletResponse.SC_OK, "OK", null, outInfo); // safe for null emails
    }


    private void signout(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String prm = prms.getParameter(SessionIdParameter);

        if (prm == null) {
            prm = getCookieSessId(rqrs.getHttpServletRequest());

            if (prm == null) {
                resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Can't find session id");

                return;
            }
        }

        if (BackendConfig.getServiceManager().getSessionManager().closeSession(prm)) {
            Cookie cke = new Cookie(BackendConfig.getSessionCookieName(), "");
            cke.setPath(getServletContext().getContextPath());
            cke.setMaxAge(0); //To delete the cookie

            resp.addCookie(cke);

            resp.respond(HttpServletResponse.SC_OK, "OK", "User logged out");
            return;
        }

        resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "User not logged in");
    }


    private void signup(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        User usr = null;

        if (!checkRecaptchas(rqrs, null)) {
            return;
        }

        List<String[]> aux = null;
        String[] auxP = prms.getParameters(AuxParameter);

        if (auxP != null && auxP.length > 0) {
            aux = new ArrayList<>(auxP.length);

            for (String s : auxP) {
                int pos = s.indexOf(AuxParameterSeparator);

                String[] tuple = new String[2];

                if (pos >= 0) {
                    tuple[0] = s.substring(0, pos);
                    tuple[1] = s.substring(pos + AuxParameterSeparator.length());
                } else {
                    tuple[0] = s;
                }

                aux.add(tuple);
            }
        }

        String login = prms.getParameter(UserLoginParameter);

        if (login != null) {
            login = login.trim();

            if (login.length() == 0) {
                login = null;
            } else {
                if (login.indexOf('@') >= 0) {
                    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Character @ in allowed in login");

                    return;
                }

                usr = BackendConfig.getServiceManager().getUserManager().getUserByLogin(login);

                if (usr != null) {
                    resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Login is taken by another user");

                    return;
                }

            }
        }

        String email = prms.getParameter(UserEmailParameter);

        if (email == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + UserEmailParameter + "' parameter is not defined");

            return;
        }

        email = email.trim();

        if (!EmailValidator.getInstance(false).isValid(email)) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Email address in not valid");

            return;
        }

        usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);

        if (usr != null) {
            resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Email is taken by another user");

            return;
        }

        String pass = prms.getParameter(PasswordParameter);

        if (pass == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + PasswordParameter + "' parameter is not defined");

            return;
        }

        if (pass.length() < 6) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + PasswordParameter + "' passowrd must be 6 symbols at least");

            return;
        }

        String actvURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            actvURL = prms.getParameter(ActivationURLParameter);
        }

        User u = new User();

        u.setLogin(login);
        u.setEmail(email);
        u.setPassword(pass);
        u.setFullName(prms.getParameter(UsernameParameter));

        try {
            BackendConfig.getServiceManager().getUserManager()
                    .addUser(u, aux, BackendConfig.isMandatoryAccountActivation(), actvURL);
        } catch (Throwable t) {
            resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Add user error: " + t.getMessage());

            return;
        }

        resp.respond(HttpServletResponse.SC_OK, "OK", null, new KV(UsernameParameter, u.getFullName()));
//  resp.respond(HttpServletResponse.SC_OK, "OK");

    }


    private void activate(ReqResp rqrs) throws IOException {
        Response resp = rqrs.getResponse();
        HttpServletRequest request = rqrs.getHttpServletRequest();

        String actKey = request.getPathInfo();

        String succURL = null;
        String failURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            succURL = request.getParameter(SuccessURLParameter);
            failURL = request.getParameter(FailURLParameter);
        }

        actKey = actKey.substring(actKey.lastIndexOf('/') + 1);

        if (actKey == null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);

            return;
        }

        ActivationInfo ainf = AccountActivation.decodeActivationKey(actKey);

        if (ainf == null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);

            return;
        }

        String msg = null;

        try {
            BackendConfig.getServiceManager().getUserManager().activateUser(ainf);
        } catch (KeyExpiredException e) {
            msg = "Activation key has expired";
        } catch (SystemUserMngException e) {
            msg = "System error";

            log.error("System error : " + e.getCause().getMessage(), e);
        } catch (Exception e) {
            msg = "Invalid request";
        }

        if (msg != null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);

            return;
        }

        resp.respondRedir(HttpServletResponse.SC_OK, "OK", "User successfully activated. You can log in now", succURL);

    }

    private void retryActivation(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String email = prms.getParameter(UserEmailParameter);

        if (email == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + UserEmailParameter + "' parameter is not defined");

            return;
        }

        email = email.trim();

        if (!checkRecaptchas(rqrs, null)) {
            return;
        }

        User usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);

        if (usr == null) {
            resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account doesn't exist");

            return;
        }

        if (usr.isActive() || usr.getActivationKey() == null) {
            resp.respond(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account is active");

            return;
        }

        String actvURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            actvURL = prms.getParameter(ActivationURLParameter);
        }

        if (!AccountActivation.sendActivationRequest(usr, UUID.fromString(usr.getActivationKey()), actvURL)) {
            resp.respond(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "FAIL", "Can't send activation email");

            return;
        } else {
            resp.respond(HttpServletResponse.SC_OK, "OK", "Activation request email has been sent");
        }

    }

    private void passwordResetRequest(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String email = prms.getParameter(UserEmailParameter);

        String succURL = null;
        String failURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            succURL = prms.getParameter(SuccessURLParameter);
            failURL = prms.getParameter(FailURLParameter);
        }

        if (email == null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + UserEmailParameter + "' parameter is not defined", failURL);

            return;
        }

        email = email.trim();

        if (!checkRecaptchas(rqrs, failURL)) {
            return;
        }

        User usr = BackendConfig.getServiceManager().getUserManager().getUserByEmail(email);

        if (usr == null) {
            resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account doesn't exist", failURL);

            return;
        }

        if (!usr.isActive()) {
            resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL", "Account is not active", failURL);

            return;
        }

        String actvURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            actvURL = prms.getParameter(PassResetURLParameter);
        }

        String msg = null;

        try {
            BackendConfig.getServiceManager().getUserManager().passwordResetRequest(usr, actvURL);

            resp.respondRedir(HttpServletResponse.SC_OK, "OK", "Password reset request email has been sent", succURL);

        } catch (SystemUserMngException e) {
            msg = "System error";

            log.error("System error : " + e.getCause().getMessage(), e);
        } catch (Exception e) {
            msg = "Invalid request";
        }

        if (msg != null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);
        }

    }

    private boolean checkRecaptchas(ReqResp rqrs, String failURL) throws IOException {
        String pubK = BackendConfig.getRecaptchaPublicKey();
        String privK = BackendConfig.getRecaptchaPrivateKey();

        if (pubK == null || pubK.length() == 0 || privK == null || privK.length() == 0) {
            return true;
        }

        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String cptResp = prms.getParameter(ReCaptcha2ResponseParameter);

        if (cptResp != null) {

            if (!checkRecaptcha2(cptResp, rqrs.getHttpServletRequest().getRemoteAddr())) {
                resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL CAPTCHA", "Captcha response is not valid",
                        failURL);

                return false;
            }

        } else {
            cptResp = prms.getParameter(ReCaptchaResponseParameter);

            if (cptResp == null) {
                resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                        "'" + ReCaptchaResponseParameter + "' parameter is not defined", failURL);

                return false;
            }

            String cptChal = prms.getParameter(ReCaptchaChallengeParameter);

            if (cptChal == null) {
                resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                        "'" + ReCaptchaChallengeParameter + "' parameter is not defined", failURL);

                return false;
            }

            ReCaptcha reCaptcha = ReCaptchaFactory.newReCaptcha("", privK, false);

            ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(prms.getClientAddress(), cptChal, cptResp);

            if (!reCaptchaResponse.isValid()) {
                resp.respondRedir(HttpServletResponse.SC_FORBIDDEN, "FAIL CAPTCHA", "Captcha response is not valid",
                        failURL);

                return false;
            }
        }

        return true;
    }

    private void passwordReset(ReqResp rqrs) throws IOException {
        ParameterPool prms = rqrs.getParameterPool();
        Response resp = rqrs.getResponse();

        String actKey = prms.getParameter(ResetKeyParameter);

        String succURL = null;
        String failURL = null;

        if (BackendConfig.isEnableUnsafeRequests()) {
            succURL = prms.getParameter(SuccessURLParameter);
            failURL = prms.getParameter(FailURLParameter);
        }

//  actKey = actKey.substring(actKey.lastIndexOf('/')+1);

        if (!checkRecaptchas(rqrs, failURL)) {
            return;
        }

        if (actKey == null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);

            return;
        }

        ActivationInfo ainf = AccountActivation.decodeActivationKey(actKey);

        if (ainf == null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", "Invalid request", failURL);

            return;
        }

        String pass = prms.getParameter(PasswordParameter);

        if (pass == null) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + PasswordParameter + "' parameter is not defined");

            return;
        }

        if (pass.length() < 6) {
            resp.respond(HttpServletResponse.SC_BAD_REQUEST, "FAIL",
                    "'" + PasswordParameter + "' passowrd must be 6 symbols at least");

            return;
        }

        String msg = null;

        try {
            BackendConfig.getServiceManager().getUserManager().resetPassword(ainf, pass);
        } catch (KeyExpiredException e) {
            msg = "Reset key has expired";
        } catch (SystemUserMngException e) {
            msg = "System error";

            log.error("System error : " + e.getCause().getMessage(), e);
        } catch (Exception e) {
            msg = "Invalid request";
        }

        if (msg != null) {
            resp.respondRedir(HttpServletResponse.SC_BAD_REQUEST, "FAIL", msg, failURL);

            return;
        }

        resp.respondRedir(HttpServletResponse.SC_OK, "OK", "User password has been reset.", succURL);
    }

    private boolean checkRecaptcha2(String resp, String cliIP) {
        try {
            URL url = new URL(BackendConfig.googleVerifyURL);

            StringBuilder postData = new StringBuilder();

            postData.append(URLEncoder.encode(BackendConfig.googleSecretParam, "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(BackendConfig.getRecaptchaPrivateKey(), "UTF-8"));

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

            JSONObject js = new JSONObject(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));

            conn.disconnect();

            Object succ = js.opt(BackendConfig.googleSuccessField);

            if (succ != null && succ instanceof Boolean) {
                return ((Boolean) succ).booleanValue();
            } else {
                log.error("Google returned invalid JSON while checking recaptcha: invalid 'success' field");
            }
        } catch (IOException e) {
            log.error("IO error while checking recaptcha: " + e.getMessage());
        } catch (JSONException e) {
            log.error("Google returned invalid JSON while checking recaptcha: " + e.getMessage());
        }

        return false;
    }
}
