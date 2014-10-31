package uk.ac.ebi.biostd.webapp.server.util;

import java.util.Enumeration;

import javax.servlet.ServletContext;

public class ServletContextParamPool implements ParamPool
{
 private final ServletContext servletContext;
 
 public ServletContextParamPool( ServletContext ctx )
 {
  servletContext = ctx;
 }
 
 @Override
 public Enumeration<String> getNames()
 {
  return servletContext.getInitParameterNames();
 }

 @Override
 public String getParameter(String name)
 {
  return servletContext.getInitParameter(name);
 }


}
