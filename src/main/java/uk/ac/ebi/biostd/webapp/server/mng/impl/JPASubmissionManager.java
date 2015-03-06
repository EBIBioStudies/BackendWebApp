package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.db.TagResolver;
import uk.ac.ebi.biostd.idgen.Counter;
import uk.ac.ebi.biostd.idgen.IdGen;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.Parser;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.in.json.JSONReader;
import uk.ac.ebi.biostd.in.pagetab.FileOccurrence;
import uk.ac.ebi.biostd.in.pagetab.PageTabSyntaxParser;
import uk.ac.ebi.biostd.in.pagetab.SectionOccurrence;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.model.SubmissionAttribute;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager;

public class JPASubmissionManager implements SubmissionManager
{
 private static Logger log;
 
 static enum SourceType
 {
  XML,
  JSON,
  PageTab
 }

 
 private ParserConfig parserCfg;
 
 private Pattern rTimePattern;
 
 public JPASubmissionManager(EntityManagerFactory emf)
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  

  parserCfg = new ParserConfig();
  
  parserCfg.setMultipleSubmissions(true);
  
  rTimePattern = Pattern.compile(Submission.releaseDateFormat);
 }

 
 @Override
 public Collection<Submission> getSubmissionsByOwner(User u, int offset, int limit)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();
  
  trn.begin();
  
  Query q = em.createNamedQuery("Submission.getByOwner");

  q.setParameter("uid", u.getId());

  if( offset > 0 )
   q.setFirstResult(offset);
  
  if( limit > 0 )
   q.setMaxResults(limit);
  
  @SuppressWarnings("unchecked")
  List<Submission> res = q.getResultList();

  trn.commit();
  
  return res;
 }

 @Override
 public LogNode deleteSubmissionByAccession( String acc, User usr )
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Deleting submission '"+acc+"'", ec);

  
  em.getTransaction().begin();
  
  Query q = em.createNamedQuery("Submission.getByAcc");
  
  q.setParameter("accNo", acc);
  
  Submission sbm = (Submission)q.getSingleResult();
  
  if( sbm == null )
  {
   gln.log(Level.ERROR, "Submission not found");
   return gln;
  }
  
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserDeleteSubmission(sbm, usr) )
  {
   gln.log(Level.ERROR, "User has no permission to delete this submission");
   return gln;
  }
  
  sbm.setMTime( System.currentTimeMillis() / 1000 );
  sbm.setVersion( - sbm.getVersion() );
  
  try
  {
   em.getTransaction().commit();
  }
  catch( Throwable t )
  {
   String err = "Database transaction failed: "+t.getMessage();
   
   gln.log(Level.ERROR, err);
   
   if( em.getTransaction().isActive() )
    em.getTransaction().rollback();
   
   return gln;
  }
  
  return gln;
 }
 
 private LogNode createSubmission( String txt, SourceType type, boolean update, User usr )
 {
  ErrorCounter ec = new ErrorCounterImpl();

  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, (update?"Updating":"Creating")+" submission(s) from "+type.name()+" source", ec);
  
  if( ! update && ! BackendConfig.getServiceManager().getSecurityManager().mayUserCreateSubmission(usr)  )
  {
   gln.log(Level.ERROR, "User has no permission to create submissions");
   return gln;
  }
  
  gln.log(Level.INFO, "Body size: " + txt.length());
    
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  TagResolver tgRslv = new TagResolverImpl(em);
  
  Parser parser = null;
  
  switch(type)
  {
   case XML:
    
    gln.log(Level.ERROR, "XML submission are not supported yet");
    
    return gln;

   case PageTab:
    
    parser = new PageTabSyntaxParser(tgRslv, parserCfg);

    break;

   case JSON:
    
    parser = new JSONReader(tgRslv, parserCfg);
    
    break;
 
    
   default:
    break;
  }
  
  
  em.getTransaction().begin();
  
  PMDoc doc=null;
  try
  {
   doc = parser.parse(txt, gln);
  }
  catch(ParserException e)
  {
   gln.log(Level.ERROR, "Parser exception: "+e.getMessage());
   SimpleLogNode.setLevels(gln);
   
   return gln;
  }

 
  SimpleLogNode.setLevels(gln);
  
  if( gln.getLevel() == Level.ERROR )
   return gln;
  
  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();

  boolean submOk=true;

  for( SubmissionInfo si : doc.getSubmissions() )
  {
   si.getSubmission().setOwner(usr);
   
   Set<String> globSecId = null;
   Submission oldSbm = null; 
   
   long ts = System.currentTimeMillis()/1000;
   
   si.getSubmission().setMTime(ts);
   
   if( update )
   {
    if( si.getAccNoPrefix() !=null || si.getAccNoSuffix() !=null || si.getSubmission().getAccNo() == null )
    {
     si.getLogNode().log(Level.ERROR, "Submission must have accession number for update operation");
     submOk = false;
     continue;
    }
    
    oldSbm = getSubmissionByAcc(si.getSubmission().getAccNo(),em);
    
    
    if( oldSbm == null )
    {
     si.getLogNode().log(Level.ERROR, "Submission '"+si.getSubmission().getAccNo()+"' doesn't exist and can't be updated");
     submOk = false;
     continue;
    }
    
    si.getSubmission().setCTime(oldSbm.getCTime());
    si.setOriginalSubmission(oldSbm);
    si.getSubmission().setVersion( oldSbm.getVersion() + 1 );
    
    if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserUpdateSubmission(oldSbm, usr) )
    {
     si.getLogNode().log(Level.ERROR, "Submission update is not permitted for this user");
     submOk = false;
     continue;
    }
    
    globSecId = new HashSet<String>();
    
    collectGlobalSecIds(oldSbm.getRootSection(),globSecId);
   }
   else
   {
    si.getSubmission().setCTime(ts);
    si.getSubmission().setVersion(1);
   }

   boolean rTimeFound=false;
   for( SubmissionAttribute sa: si.getSubmission().getAttributes() )
   {
    if( Submission.releaseDateAttribute.equals( sa.getName() ) )
    {
     if( rTimeFound )
     {
      si.getLogNode().log(Level.ERROR, "Multiple '"+Submission.releaseDateAttribute+"' attributes are not allowed");
      break;
     }
     
     rTimeFound = true;
     
     String val = sa.getValue();
     
     if( val != null )
     {
      val=val.trim();
      
      if( val.length() > 0 )
      {
       Matcher mtch = rTimePattern.matcher(val);
       
       if( ! mtch.matches() )
        si.getLogNode().log(Level.ERROR, "Invalid '"+Submission.releaseDateAttribute+"' attribute value. Expected date in format: YYYY-MM-DD[Thh:mm[:ss[.mmm]]]");
       else
       {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        
        cal.set(Calendar.YEAR, Integer.parseInt(mtch.group("year")));
        cal.set(Calendar.MONTH, Integer.parseInt(mtch.group("month"))-1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mtch.group("day")));
        
        String str = mtch.group("hour");
        
        if( str != null )
         cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str));
 
        str = mtch.group("min");
        
        if( str != null )
         cal.set(Calendar.MINUTE, Integer.parseInt(str));

        
        str = mtch.group("sec");
        
        if( str != null )
         cal.set(Calendar.SECOND, Integer.parseInt(str));
        
        si.getSubmission().setRTime( cal.getTimeInMillis()/1000 );
       }
      }
     }
     
    }
   }
    
   for( FileOccurrence foc : si.getFileOccurrences())
   {
    FilePointer fp = fileMngr.checkFileExist(foc.getFileRef().getName(), usr);
    
    if( fp != null || ( update && ( fp = fileMngr.checkFileExist(foc.getFileRef().getName(), oldSbm) ) != null )  )
     foc.setFilePointer( fp );
    else
    {
     foc.getLogNode().log(Level.ERROR, "File reference '"+foc.getFileRef().getName()+"' can't be resolved. Check files in the user directory");
     submOk = false;
    }
   }
   
   if( si.getAccNoPrefix() == null && si.getAccNoSuffix() == null && ! update )
   {
    if( ! checkSubmissionIdUniq(si.getSubmission().getAccNo(), em) )
    {
     si.getLogNode().log(Level.ERROR, "Submission accession number '"+si.getSubmission().getAccNo()+"' is already taken by another submission");
     submOk = false;
    }
   }
   
   for( SectionOccurrence seco : si.getGlobalSections() )
   {
    if( seco.getPrefix() == null && seco.getSuffix() == null && ( globSecId == null || ! globSecId.contains(seco.getSection().getAccNo()) ) )
    {
     if( ! checkSectionIdUniq(seco.getSection().getAccNo(), em) )
     {
      seco.getSecLogNode().log(Level.ERROR, "Section accession number '"+seco.getSection().getAccNo()+"' is taken by another section");
      submOk = false;
     }
    }
   }
   
  }
  
  if( ! submOk )
  {
   SimpleLogNode.setLevels(gln);
   return gln;
  }
  
  
  for( SubmissionInfo si : doc.getSubmissions() )
  {
   
   if( si.getAccNoPrefix() != null || si.getAccNoSuffix() != null )
   {
    while(true)
    {
     String newAcc = getNextAccNo(si.getAccNoPrefix(), si.getAccNoSuffix(), em);

     if(checkSubmissionIdUniq(newAcc, em))
     {
      si.getSubmission().setAccNo(newAcc);
      break;
     }
    }
   }
   
   for(SectionOccurrence seco : si.getGlobalSections())
   {
    if(seco.getPrefix() != null || seco.getSuffix() != null)
    {

     while(true)
     {
      String newAcc = getNextAccNo(seco.getPrefix(), seco.getSuffix(), em);

      if(checkSectionIdUniq(newAcc, em))
      {
       seco.getSection().setAccNo(newAcc);
       break;
      }
     }
    }
   }
   
   if( si.getOriginalSubmission() != null )
    si.getOriginalSubmission().setVersion(- si.getOriginalSubmission().getVersion() );

   em.persist( si.getSubmission() );
   
  }
  
  try
  {
   em.getTransaction().commit();
  }
  catch( Throwable t )
  {
   String err = "Database transaction failed: "+t.getMessage();
   
   gln.log(Level.ERROR, err);
   
   if( em.getTransaction().isActive() )
    em.getTransaction().rollback();
   
   return gln;
  }
  
   
  for( SubmissionInfo si : doc.getSubmissions() )
  {
  
   fileMngr.createSubmissionDir( si.getSubmission() );
   
   if( si.getFileOccurrences() != null  )
   {
    for( FileOccurrence fo : si.getFileOccurrences() )
    {
     try
     {
      fileMngr.copyToSubmissionFilesDir( si.getSubmission(), fo.getFilePointer() );
     }
     catch( IOException e )
     {
      si.getLogNode().log(Level.ERROR, "File transfer error: "+fo.getFilePointer());
     }
    }
   }
   
   String srcFileName = "source";
   
   switch( type )
   {
    case JSON:
     srcFileName += ".json.txt";
     break;
     
    case PageTab:
     srcFileName += ".pagetab.txt";
     break;

    case XML:
     srcFileName += ".xml";
     break;

    default:
     break;
   }
   
   File srcFile = fileMngr.createSubmissionDirFile( si.getSubmission(), srcFileName );
   
   try
   {
    PrintWriter srcOut = new PrintWriter(srcFile);
    
    srcOut.append(txt);
    
    srcOut.close();
    
   }
   catch(IOException e)
   {
    si.getLogNode().log(Level.ERROR, "File write error: "+srcFileName);
   }
   
  }
  
  SimpleLogNode.setLevels(gln);
  
  return gln;

 }


 private String getNextAccNo(String prefix, String suffix, EntityManager em)
 {
  Query q = em.createNamedQuery("IdGen.getByPfxSfx");
  
  q.setParameter("prefix", prefix);
  q.setParameter("suffix", suffix);
  
  @SuppressWarnings("unchecked")
  List<IdGen> genList = q.getResultList();
  
  IdGen gen = null;
  
  if( genList.size() == 0 )
  {
   gen = new IdGen();
   
   gen.setPrefix(prefix);
   gen.setSuffix(suffix);
   
   em.persist(gen);
  }
  else if( genList.size() == 1 )
   gen = genList.get(0);
  else
   log.error("Query returned multiple ("+genList.size()+") IdGen objects");

  Counter cnt = gen.getCounter();
  
  if( cnt == null )
  {
   cnt = new Counter();
   cnt.setMaxCount(0);
   
   em.persist(cnt);
   
   gen.setCounter(cnt);
  }
  
  StringBuilder sb = new StringBuilder();
  
  if( prefix != null )
   sb.append(prefix);
  
  sb.append( cnt.getNextNumber() );
  
  if( suffix != null )
   sb.append(suffix);

  return sb.toString();
 }


 private void collectGlobalSecIds(Section sec, Set<String> globSecId)
 {
  if( sec.isGlobal() )
   globSecId.add(sec.getAccNo());
  
  if( sec.getSections() != null )
  {
   for( Section sbs : sec.getSections() )
    collectGlobalSecIds(sbs, globSecId);
  }
  
 }


 private Submission getSubmissionByAcc(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Submission.getByAcc");
 
  q.setParameter("accNo", accNo);
  
  return (Submission)q.getSingleResult();
 }


 private boolean checkSubmissionIdUniq(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Submission.countByAcc");
  q.setParameter("accNo", accNo);
  
  return ((Number)q.getSingleResult()).intValue() == 0;
 }

 private boolean checkSectionIdUniq(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Section.countByAcc");
  q.setParameter("accNo", accNo);
  
  return ((Number)q.getSingleResult()).intValue() == 0;
 }


 @Override
 public LogNode createJSONSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.JSON, false, usr);
 }


 @Override
 public LogNode createXMLSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.XML, false, usr);
 }


 @Override
 public LogNode createPageTabSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.PageTab, false, usr);
 }


 @Override
 public LogNode updateJSONSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.JSON, true, usr);
 }


 @Override
 public LogNode updateXMLSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.XML, true, usr);
 }


 @Override
 public LogNode updatePageTabSubmission(String txt, User usr)
 {
  return createSubmission(txt, SourceType.PageTab, true, usr);
 }

}
