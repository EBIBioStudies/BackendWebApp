package uk.ac.ebi.biostd.webapp.server.mng;

public interface SessionManager
{
 String getEffectiveUser();
 
 Session createSession( String uname );
 Session getSession( String sKey );
 Session getSession();
 Session getSessionByUser(String id);

 Session checkin( String sessId );
 Session checkout( );

 void shutdown();

}
