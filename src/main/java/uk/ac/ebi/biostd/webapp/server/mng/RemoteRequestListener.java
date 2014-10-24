package uk.ac.ebi.biostd.webapp.server.mng;

import java.io.PrintWriter;


public interface RemoteRequestListener
{

 boolean processRequest(ServiceRequest upReq, PrintWriter printWriter);

}
