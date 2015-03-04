package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SessionListener;
import uk.ac.ebi.biostd.webapp.server.mng.SessionManager;



public class SessionManagerImpl implements SessionManager, Runnable
{
 private static final int CHECK_INTERVAL = 30000;
 private static final int MAX_SESSION_IDLE_TIME = 3000000;
 
 private final Thread controlThread = new Thread( this );
 private boolean shutdown = false;

 private final Map<String, Session> sessionMap = new TreeMap<String, Session>();
 private final Map<Thread, Session> threadMap = new HashMap<Thread, Session>();
 private Collection<SessionListener> sessLstnrs = new ArrayList<SessionListener>();
 
 private final Lock lock = new ReentrantLock(); 
 
 private final File sessDirRoot;
 
 private Session anonSess;
 
 public SessionManagerImpl( File sdr )
 {
  sessDirRoot=sdr;
  
  controlThread.setName("Session GC");
  controlThread.setDaemon(true);
  
  controlThread.start();
 }
 
 @Override
 public Session createSession(User user)
 {
  String key = generateSessionKey(user.getLogin());

  File sessDir = new File(sessDirRoot,key);
  
  Session sess = new Session( sessDir, BackendConfig.getEntityManagerFactory() );
    
  sess.setSessionKey(key);
  sess.setUser(user);

  try
  {
   lock.lock();

   sessionMap.put(key,sess);
   
   for( SessionListener sl : sessLstnrs )
    sl.sessionOpened(user);
  }
  finally
  {
   lock.unlock();
  }
  
  return sess;
 }

 @Override
 public Session getSession(String sessID)
 {
  try
  {
   lock.lock();

   Session sess = sessionMap.get(sessID);
   
   if( sess == null )
    return null;
   
   sess.setLastAccessTime( System.currentTimeMillis() );
   
   return sessionMap.get(sessID);
  }
  finally
  {
   lock.unlock();
  }
 }
 
 @Override
 public boolean closeSession(String sessID)
 {
  try
  {
   lock.lock();

   Session sess = sessionMap.remove(sessID);
   
   if( sess == null )
    return false;
   
   User u = sess.getUser();
   
   sess.destroy();
   
   for( SessionListener sl : sessLstnrs )
    sl.sessionClosed(u);
   
   return true;
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public void shutdown()
 {
  shutdown = true;
  controlThread.interrupt();
 }
 
 @Override
 public boolean hasActiveSessions()
 {
  return sessionMap.size() > 0;
 }

 @Override
 public void run()
 {
  Thread.currentThread().setName("Session manager watchdog");
  
  while( ! shutdown )
  {

   
   try
   {
    lock.lock();
    
    long time = System.currentTimeMillis();

    Iterator<Session> sitr = sessionMap.values().iterator();
    
    while( sitr.hasNext() )
    {
     Session sess = sitr.next();
     
     if( ( ( time - sess.getLastAccessTime() ) > MAX_SESSION_IDLE_TIME && ! sess.isCheckedIn() ) || shutdown )
     {
      sitr.remove();

      User u = sess.getUser();
      
      sess.destroy();
      
      for( SessionListener sl : sessLstnrs )
       sl.sessionClosed(u);
      
      Iterator<Map.Entry<Thread, Session>> thIter = threadMap.entrySet().iterator();
      
      while( thIter.hasNext() )
      {
       Map.Entry<Thread, Session> me = thIter.next();
       
       if( me.getValue() == sess )
        thIter.remove();
      }
       
     }
      
    }
    
    if( anonSess != null )
    {
     if( ( ( time - anonSess.getLastAccessTime() ) > MAX_SESSION_IDLE_TIME ) || shutdown )
     {
      anonSess.destroy();
      
      Iterator<Map.Entry<Thread, Session>> thIter = threadMap.entrySet().iterator();
      
      while( thIter.hasNext() )
      {
       Map.Entry<Thread, Session> me = thIter.next();
       
       if( me.getValue() == anonSess )
        thIter.remove();
      }
      
      anonSess=null;
     }
    }
   }
   finally
   {
    lock.unlock();
   }
   
   try
   {
    Thread.sleep(CHECK_INTERVAL);
   }
   catch(InterruptedException e)
   {
   }

  }
 }

 final static String algorithm="SHA1";
 
 private String generateSessionKey( String strs )
 {

  StringBuffer message = new StringBuffer(100);

  message.append( Math.random() );
  
  message.append(strs);

  message.append(System.currentTimeMillis());

  try
  {
   MessageDigest md5d = MessageDigest.getInstance(algorithm);

   byte[] digest = md5d.digest(message.toString().getBytes());

   message.setLength(0);
   message.append("K");

   for(int i = 0; i < digest.length; i++)
   {
    String byteHex = Integer.toHexString(digest[i]);
    
    if( byteHex .length() < 2 )
     message.append('0').append(byteHex.charAt(0));
    else
     message.append(byteHex.substring(byteHex.length()-2));
   }
   
   return message.toString();
  }
  catch(NoSuchAlgorithmException ex)
  {
   ex.printStackTrace();
   return String.valueOf(System.currentTimeMillis());
  }

 }


 @Override
 public Session getSession()
 {
  try
  {
   lock.lock();
   
   Session sess = threadMap.get(Thread.currentThread());
   
   if( sess == null )
   {
    if( anonSess != null )
     sess = anonSess;
    
    String key = generateSessionKey("__");

    File sessDir = new File(sessDirRoot,key);
    
    sess =  anonSess = new Session(sessDir, BackendConfig.getEntityManagerFactory()) ;  
    
   }
   
   return sess;
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public Session checkin(String sessId)
 {
  try
  {
   lock.lock();
   
   Session sess = sessionMap.get( sessId );
   
   if( sess != null )
   {
    threadMap.put(Thread.currentThread(), sess);
    
    sess.setCheckedIn( true );
   }
   
   return sess;
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public Session checkout()
 {
  try
  {
   lock.lock();
   
   Session sess = threadMap.remove(Thread.currentThread());
   
   if( sess != null )
    sess.setCheckedIn( false );
   
   return sess;
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public User getEffectiveUser()
 {
  Session sess = getSession();
  
  if( sess == null )
   return null;
  
  return sess.getUser();
 }

 @Override
 public Session getSessionByUser(String uid)
 {
  try
  {
   lock.lock();
   
   Iterator<Session> sessItr = sessionMap.values().iterator();
   
   while( sessItr.hasNext() )
   {
    Session s = sessItr.next();
    if(s.getUser().getLogin().equals(uid) )
     return s;
   } 
   
   return null;
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public void addSessionListener(SessionListener sl)
 {
  try
  {
   lock.lock();

   sessLstnrs.add(sl);
  }
  finally
  {
   lock.unlock();
  }
  
 }

 @Override
 public void removeSessionListener(SessionListener sl)
 {
  try
  {
   lock.lock();

   sessLstnrs.remove(sl);
  }
  finally
  {
   lock.unlock();
  }
 }

 @Override
 public Session getAnonymousSession()
 {
  try
  {
   lock.lock();

  if( anonSess != null )
   return anonSess;
  
  String key = generateSessionKey("__");

  File sessDir = new File(sessDirRoot,key);
  
  return anonSess = new Session(sessDir, BackendConfig.getEntityManagerFactory()) ;
  }
  finally
  {
   lock.unlock();
  }
 }

}
