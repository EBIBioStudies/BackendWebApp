package uk.ac.ebi.biostd.webapp.server.endpoint;

public interface ParameterPool
{
 String getParameter(String pName);
 
 String getClientAddress();

 String[] getParameters(String pName);
}
