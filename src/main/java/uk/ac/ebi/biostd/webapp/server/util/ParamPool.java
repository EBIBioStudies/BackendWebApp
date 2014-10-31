package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Enumeration;



public interface ParamPool
{
 Enumeration<String> getNames();
 
 String getParameter( String name );
}