package uk.ac.ebi.biostd.webapp.server.endpoint.perm;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.security.ObjectClass;
import uk.ac.ebi.biostd.webapp.server.mng.security.PermissionClass;
import uk.ac.ebi.biostd.webapp.server.mng.security.SecurityException;
import uk.ac.ebi.biostd.webapp.server.mng.security.SubjectClass;

public class PermissionsServlet extends ServiceServlet {

    public static final String OperationParameter = "op";
    public static final String SubjectClassParameter = "subjectClass";
    public static final String SubjectIDParameter = "subjectID";
    public static final String ObjectClassParameter = "objectClass";
    public static final String ObjectIDParameter = "objectID";
    public static final String PermissionClassParameter = "permissionClass";
    public static final String PermissionIDParameter = "permissionID";
    public static final String PermissionActionParameter = "permissionAction";
    private static final long serialVersionUID = 1L;
    private static Logger log;

    public PermissionsServlet() {
        if (log == null) {
            log = LoggerFactory.getLogger(this.getClass());
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse response, Session sess) throws ServletException,
            IOException {

        if (sess == null || sess.isAnonymouns()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain");
            response.getWriter().print("FAIL User not logged in");
            return;
        }

        String val = req.getParameter(OperationParameter);

        if (val == null) {
            badReq(response, "Parameter '" + OperationParameter + "' is not set");
            return;
        }

        Operation op = null;

        for (Operation cop : Operation.values()) {
            if (cop.name().equalsIgnoreCase(val)) {
                op = cop;
                break;
            }
        }

        if (op == null) {
            badReq(response, "Invalid value of '" + OperationParameter + "'");
            return;
        }

        SubjectClass sClass = null;

        val = req.getParameter(SubjectClassParameter);

        if (val == null) {
            badReq(response, "Parameter '" + SubjectClassParameter + "' is not set");
            return;
        }

        for (SubjectClass cop : SubjectClass.values()) {
            if (cop.name().equalsIgnoreCase(val)) {
                sClass = cop;
                break;
            }
        }

        if (sClass == null) {
            badReq(response, "Invalid value of '" + SubjectClassParameter + "'");
            return;
        }

        String sID = null;

        sID = req.getParameter(SubjectIDParameter);

        if (sID == null) {
            badReq(response, "Parameter '" + SubjectIDParameter + "' is not set");
            return;
        }

        ObjectClass oClass = null;

        val = req.getParameter(ObjectClassParameter);

        if (val == null) {
            badReq(response, "Parameter '" + ObjectClassParameter + "' is not set");
            return;
        }

        for (ObjectClass cop : ObjectClass.values()) {
            if (cop.name().equalsIgnoreCase(val)) {
                oClass = cop;
                break;
            }
        }

        if (oClass == null) {
            badReq(response, "Invalid value of '" + ObjectClassParameter + "'");
            return;
        }

        String oID = null;

        oID = req.getParameter(ObjectIDParameter);

        if (oID == null) {
            badReq(response, "Parameter '" + ObjectIDParameter + "' is not set");
            return;
        }

        PermissionClass pClass = null;

        val = req.getParameter(PermissionClassParameter);

        if (val == null) {
            badReq(response, "Parameter '" + PermissionClassParameter + "' is not set");
            return;
        }

        for (PermissionClass cop : PermissionClass.values()) {
            if (cop.name().equalsIgnoreCase(val)) {
                pClass = cop;
                break;
            }
        }

        if (pClass == null) {
            badReq(response, "Invalid value of '" + PermissionClassParameter + "'");
            return;
        }

        String pID = null;

        pID = req.getParameter(PermissionIDParameter);

        if (pID == null) {
            badReq(response, "Parameter '" + PermissionIDParameter + "' is not set");
            return;
        }

        boolean pAction = false;

        if (pClass == PermissionClass.Permission) {
            val = req.getParameter(PermissionActionParameter);

            if (val == null) {
                badReq(response, "Parameter '" + PermissionActionParameter + "' is not set");
                return;
            }

            if ("deny".equalsIgnoreCase(val)) {
                pAction = false;
            } else if ("allow".equalsIgnoreCase(val)) {
                pAction = true;
            } else {
                badReq(response, "Invalid parameter '" + PermissionActionParameter + "' value");
                return;
            }
        }

        try {
            if (op == Operation.SET) {
                BackendConfig.getServiceManager().getSecurityManager()
                        .setPermission(pClass, pID, pAction, sClass, sID, oClass, oID, sess.getUser());
            } else if (op == Operation.CLEAR) {
                BackendConfig.getServiceManager().getSecurityManager()
                        .clearPermission(pClass, pID, pAction, sClass, sID, oClass, oID, sess.getUser());
            }
        } catch (SecurityException e) {
            badReq(response, "Operation failed: " + e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.getWriter().print("OK success");

    }

    private void badReq(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.getWriter().print("FAIL " + msg);
    }

    public enum Operation {
        SET,
        CLEAR
    }
}
