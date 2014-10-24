package uk.ac.ebi.biostd.webapp.server.webdav;

import uk.ac.ebi.biostd.authz.User;

public class ThreadUser
{
 private static ThreadLocal<User> user = new ThreadLocal<User>();
 
 public static void setUser( User u )
 {
  user.set(u);
 }
 
 public static User getUser()
 {
  return user.get();
 }
 
}
