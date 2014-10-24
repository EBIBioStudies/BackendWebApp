package uk.ac.ebi.biostd.webapp.server.webdav;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet Filter implementation class UserFilter
 */
//@WebFilter(dispatcherTypes = { DispatcherType.FORWARD }, urlPatterns = { "/UserFilter", "/files" })
public class UserFilter implements Filter
{

 /**
  * Default constructor.
  */
 public UserFilter()
 {
  // TODO Auto-generated constructor stub
 }

 /**
  * @see Filter#destroy()
  */
 @Override
 public void destroy()
 {
  // TODO Auto-generated method stub
 }

 /**
  * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
  */
 @Override
 public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
 {
  HttpServletRequest req = (HttpServletRequest)request;
  
  req.getPathInfo();
  req.
  
  chain.doFilter(request, response);
 }

 /**
  * @see Filter#init(FilterConfig)
  */
 @Override
 public void init(FilterConfig fConfig) throws ServletException
 {
  // TODO Auto-generated method stub
 }

}
