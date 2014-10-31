package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

public interface ParameterPool
{
 String getParameter(String pName);
 
 Action getDefaultAction();
}
