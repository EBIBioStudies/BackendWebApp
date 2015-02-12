package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;

public interface SessionManager
{
 User getEffectiveUser();
 
 Session createSession(User user);
 Session getSession( String sKey );
 Session getSession();
 Session getSessionByUser(String id);
 boolean closeSession( String sKey );

 Session checkin( String sessId );
 Session checkout( );

 boolean hasActiveSessions();
 
 void addSessionListener( SessionListener sl );
 void removeSessionListener( SessionListener sl );
 
 void shutdown();

}
