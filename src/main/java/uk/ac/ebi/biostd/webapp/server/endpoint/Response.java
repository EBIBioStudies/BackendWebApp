package uk.ac.ebi.biostd.webapp.server.endpoint;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biostd.webapp.shared.util.KV;

public interface Response
{


 void respondRedir(int code, String sts, String msg, String url) throws IOException;
 void respond(int code, String sts) throws IOException;
 void respond(int code, String sts, String msg, KV ... kvs) throws IOException;
 void addCookie(Cookie cookie);
 
 ReqResp.Format getFormat();
 HttpServletResponse getHttpServletResponse();
 
}
