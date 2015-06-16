package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.idgen.Counter;
import uk.ac.ebi.biostd.idgen.IdGen;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.in.json.JSONReader;
import uk.ac.ebi.biostd.in.pagetab.CSVTSVSpreadsheetReader;
import uk.ac.ebi.biostd.in.pagetab.FileOccurrence;
import uk.ac.ebi.biostd.in.pagetab.ODSpreadsheetReader;
import uk.ac.ebi.biostd.in.pagetab.PageTabSyntaxParser;
import uk.ac.ebi.biostd.in.pagetab.SectionOccurrence;
import uk.ac.ebi.biostd.in.pagetab.SpreadsheetReader;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.in.pagetab.XLSpreadsheetReader;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.model.SubmissionAttribute;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;

public class JPASubmissionManager implements SubmissionManager
{
 private enum SubmissionDirState
 {
  ABSENT,
  LINKED,
  COPIED,
  HOME
 }
 
 private static class FileTransactionUnit
 {
  Path submissionPath;
  Path historyPath;
  Path submissionPathTmp;
  Path historyPathTmp;

  SubmissionDirState state;
 }
 
 private static Logger log;
 
 static enum SourceType
 {
  XML,
  JSON,
  PageTab
 }

 private static Set<PosixFilePermission> rwxrwx___ = PosixFilePermissions.fromString("rwxrwx---");
 private static Set<PosixFilePermission> rwxrwxr_x = PosixFilePermissions.fromString("rwxrwxr-x");

 
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

  try
  {
   trn.begin();

   Query q = em.createNamedQuery("Submission.getByOwner");

   q.setParameter("uid", u.getId());

   if(offset > 0)
    q.setFirstResult(offset);

   if(limit > 0)
    q.setMaxResults(limit);

   @SuppressWarnings("unchecked")
   List<Submission> res = q.getResultList();

   return res;
  }
  finally
  {
   trn.commit();
  }
 }

 @Override
 public LogNode deleteSubmissionByAccession( String acc, User usr )
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Deleting submission '"+acc+"'", ec);

  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  
  Path origDir = null;
  Path histDir = null;
  Path histDirTmp = null;
  
  int dirOp = 0; // 0 - not changed, 1 - moved, 2 - copied, 3 = error
  
  try
  {
   em.getTransaction().begin();

   Query q = em.createNamedQuery("Submission.getByAcc");

   q.setParameter("accNo", acc);

   Submission sbm = null;

   try
   {
    sbm = (Submission) q.getSingleResult();
   }
   catch(NoResultException e)
   {
   }

   if(sbm == null)
   {
    gln.log(Level.ERROR, "Submission not found");
    return gln;
   }

   if(!BackendConfig.getServiceManager().getSecurityManager().mayUserDeleteSubmission(sbm, usr))
   {
    gln.log(Level.ERROR, "User has no permission to delete this submission");
    return gln;
   }

   origDir = BackendConfig.getSubmissionPath(sbm);

   if(Files.exists(origDir))
   {
    histDir = BackendConfig.getSubmissionHistoryPath(sbm);
    histDirTmp = histDir.resolveSibling(histDir.getFileName() + "#tmp");

    try
    {
     fileMngr.moveDirectory(origDir, histDirTmp); // trying to submission directory to the history dir
     dirOp = 1;
    }
    catch(Exception e)
    {
     // If we can't move the directory we have to make a copy of it

     try
     {
      fileMngr.copyDirectory(origDir, histDirTmp);
      dirOp=2;
     }
     catch(Exception ex1)
     {
      log.error("Can't copy directory " + origDir + " to " + histDirTmp + " : " + ex1.getMessage());
      gln.log(Level.ERROR, "File operation error. Contact system administrator");
      
      dirOp = 3;
      
      return gln; // Bad. We have to break the operation
     }

    }
   }
   
   sbm.setMTime(System.currentTimeMillis() / 1000);
   sbm.setVersion(-sbm.getVersion());
  }
  finally
  {
   boolean trnOk=true;
   
   try
   {
    if( dirOp != 3 )
     em.getTransaction().commit();
    else
    {
     em.getTransaction().rollback();
     trnOk = false;
    }
   }
   catch(Throwable t)
   {
    trnOk = false;
    
    String err = "Database transaction failed: " + t.getMessage();

    gln.log(Level.ERROR, err);

    if(em.getTransaction().isActive())
     em.getTransaction().rollback();
   }
   
   if( trnOk )
    gln.log(Level.INFO, "Transaction successful");
   else
   {
    if( dirOp == 1 )
    {
     try
     {
      fileMngr.moveDirectory(histDirTmp, origDir );
     }
     catch(IOException e)
     {
      log.error("Delete opration rollback (move dir) failed: "+e);
      e.printStackTrace();
      
      gln.log(Level.ERROR, "Severe server problem. Please inform system administrator. The database may be inconsistent");
     }

    }
    else if( dirOp == 2 )
    {
     try
     {
      fileMngr.deleteDirectory(histDirTmp);
     }
     catch(IOException e)
     {
      log.error("Delete opration rollback (del dir) failed: "+e);
      e.printStackTrace();
     }
    }
   }

  }
  
  return gln;
 }
 
 

 @Override
 public Submission getSubmissionsByAccession(String acc)
 {
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  EntityTransaction trn = em.getTransaction();

  try
  {
   trn.begin();

   Query q = em.createNamedQuery("Submission.getByAcc");


   @SuppressWarnings("unchecked")
   List<Submission> res = q.getResultList();

   if( res.size() > 0 )
    return res.get(0);
   
   return null;
  }
  finally
  {
   trn.commit();
  }
  
 }

 
 @Override
 public synchronized LogNode createSubmission( byte[] data, DataFormat type, String charset, Operation op, User usr, boolean validateOnly )
 {
  ErrorCounter ec = new ErrorCounterImpl();

  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, op.name() + " submission(s) from " + type.name() + " source", ec);

  if( op == Operation.CREATE && !BackendConfig.getServiceManager().getSecurityManager().mayUserCreateSubmission(usr) )
  {
   gln.log(Level.ERROR, "User has no permission to create submissions");
   return gln;
  }

  gln.log(Level.INFO, "Processing '" + type.name() + "' data. Body size: " + data.length);

  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  boolean submOk = true;
  boolean submComplete = false;

  PMDoc doc = null;
  SpreadsheetReader reader = null;

  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  Path trnPath = BackendConfig.getSubmissionsTransactionPath().resolve(BackendConfig.getInstanceId()+"#"+BackendConfig.getSeqNumber());

  List<FileTransactionUnit> trans = null;

  try
  {

   em.getTransaction().begin();

   switch(type)
   {
    case xml:

     gln.log(Level.ERROR, "XML submission are not supported yet");

     return gln;

    case json:

     String txt = convertToText(data, charset, gln);

     if(txt != null)
      doc = new JSONReader(new TagResolverImpl(em), parserCfg).parse(txt, gln);

     break;

    case xlsx:
    case xls:

     try
     {
      Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data));
      reader = new XLSpreadsheetReader(wb);
     }
     catch(Exception e)
     {
      gln.log(Level.ERROR, "Can't read Excel file: " + e.getMessage());
     }

     break;

    case ods:

     try
     {
      SpreadsheetDocument odsdoc = SpreadsheetDocument.loadDocument(new ByteArrayInputStream(data));
      reader = new ODSpreadsheetReader(odsdoc);
     }
     catch(Exception e)
     {
      gln.log(Level.ERROR, "Can't read ODS file: " + e.getMessage());
     }

     break;

    case csv:
    case tsv:
    case csvtsv:

     txt = convertToText(data, charset, gln);

     if(txt != null)
      reader = new CSVTSVSpreadsheetReader(txt, type == DataFormat.csv ? ',' : (type == DataFormat.tsv ? '\t' : '\0'));

     break;

    default:
     
     gln.log(Level.ERROR, "Unsupported file type: " + type.name());
     SimpleLogNode.setLevels(gln);
     return gln;

   }

   SimpleLogNode.setLevels(gln);

   if(gln.getLevel() == Level.ERROR)
   {
    submOk = false;
    return gln;
   }

   if(reader != null)
   {
    PageTabSyntaxParser prs = new PageTabSyntaxParser(new TagResolverImpl(em), parserCfg);

    try
    {
     doc = prs.parse(reader, gln);
    }
    catch(ParserException e)
    {
     gln.log(Level.ERROR, "Parser exception: " + e.getMessage());
     SimpleLogNode.setLevels(gln);
     submOk = false;
     return gln;
    }
   }
   
   Path usrPath = BackendConfig.getUserDirPath(usr);

   
   for(SubmissionInfo si : doc.getSubmissions())
   {
    si.getSubmission().setOwner(usr);

    Set<String> globSecId = null;

    long ts = System.currentTimeMillis() / 1000;

    si.getSubmission().setMTime(ts);

    Submission oldSbm = null;
//      getSubmissionByAcc(si.getSubmission().getAccNo(), em);
//
//
//    if( oldSbm != null && op == Operation.NEW )
//    {
//     
//    }
    
    if( si.getAccNoPrefix() != null )
    {
     if(  ! AccNoUtil.checkAccNoStr(si.getAccNoPrefix()) )
     {
      si.getLogNode().log(Level.ERROR, "Submission accssesion number prefix contains invalid characters");
      submOk = false;
     }
    }
    
    if( si.getAccNoSuffix() != null )
    {
     if(  ! AccNoUtil.checkAccNoStr(si.getAccNoSuffix()) )
     {
      si.getLogNode().log(Level.ERROR, "Submission accssesion number suffix contains invalid characters");
      submOk = false;
     }
    }
    
    if( si.getAccNoPrefix() == null && si.getAccNoSuffix() == null )
    {
     if(  ! AccNoUtil.checkAccNoStr(si.getSubmission().getAccNo() ) )
     {
      si.getLogNode().log(Level.ERROR, "Submission accssesion number contains invalid characters");
      submOk = false;
     }
    }
    
    if( op == Operation.UPDATE || op == Operation.REPLACE )
    {
     if(si.getAccNoPrefix() != null || si.getAccNoSuffix() != null || si.getSubmission().getAccNo() == null)
     {
      si.getLogNode().log(Level.ERROR, "Submission must have accession number for "+op.name()+" operation");
      submOk = false;
      continue;
     }

     oldSbm = getSubmissionByAcc(si.getSubmission().getAccNo(), em);

     if( oldSbm == null )
     {
      if( op == Operation.REPLACE )
      {
       si.getLogNode().log(Level.ERROR, "Submission '" + si.getSubmission().getAccNo() + "' doesn't exist and can't be replced");
       submOk = false;
       continue;
      }
      else
      {
       if( !BackendConfig.getServiceManager().getSecurityManager().mayUserCreateSubmission(usr) )
       {
        si.getLogNode().log(Level.ERROR, "User has no permission to create submissions");
        submOk = false;
        continue;
       }
       
       si.getSubmission().setCTime(ts);
       si.getSubmission().setVersion(1);
      }
     }
     else
     {
      si.getSubmission().setCTime(oldSbm.getCTime());
      si.setOriginalSubmission(oldSbm);
      si.getSubmission().setVersion(oldSbm.getVersion() + 1);
      
      if(!BackendConfig.getServiceManager().getSecurityManager().mayUserUpdateSubmission(oldSbm, usr))
      {
       si.getLogNode().log(Level.ERROR, "Submission update is not permitted for this user");
       submOk = false;
       continue;
      }
      
      globSecId = new HashSet<String>();
      
      collectGlobalSecIds(oldSbm.getRootSection(), globSecId);
     }
     
    }
    else
    {
     si.getSubmission().setCTime(ts);
     si.getSubmission().setVersion(1);
    }

    Path relPath = null;
    
    boolean rTimeFound = false;
    boolean rootPathFound = false;
    
    String rootPathAttr = null; 
    
    Iterator<SubmissionAttribute> saitr = si.getSubmission().getAttributes().iterator();
    
    while(saitr.hasNext() )
    {
     SubmissionAttribute sa = saitr.next(); 
     
     if(Submission.releaseDateAttribute.equals(sa.getName()))
     {
      saitr.remove();
      
      if(rTimeFound)
      {
       si.getLogNode().log(Level.ERROR, "Multiple '" + Submission.releaseDateAttribute + "' attributes are not allowed");
       submOk = false;
       break;
      }

      rTimeFound = true;

      String val = sa.getValue();

      if(val != null)
      {
       val = val.trim();

       if(val.length() > 0)
       {
        Matcher mtch = rTimePattern.matcher(val);

        if(!mtch.matches())
         si.getLogNode().log(Level.ERROR,
           "Invalid '" + Submission.releaseDateAttribute + "' attribute value. Expected date in format: YYYY-MM-DD[Thh:mm[:ss[.mmm]]]");
        else
        {
         Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

         cal.set(Calendar.YEAR, Integer.parseInt(mtch.group("year")));
         cal.set(Calendar.MONTH, Integer.parseInt(mtch.group("month")) - 1);
         cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mtch.group("day")));

         String str = mtch.group("hour");

         if(str != null)
          cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str));

         str = mtch.group("min");

         if(str != null)
          cal.set(Calendar.MINUTE, Integer.parseInt(str));

         str = mtch.group("sec");

         if(str != null)
          cal.set(Calendar.SECOND, Integer.parseInt(str));

         si.getSubmission().setRTime(cal.getTimeInMillis() / 1000);
        }
       }
      }

     }
     else if( Submission.rootPathAttribute.equals(sa.getName()) )
     {
      saitr.remove();

      if(rootPathFound)
      {
       si.getLogNode().log(Level.ERROR, "Multiple '" + Submission.rootPathAttribute + "' attributes are not allowed");
       submOk = false;
       break;
      }

      rootPathFound = true;
      
      rootPathAttr = sa.getValue();
      si.getSubmission().setRootPath(rootPathAttr);
     }
     else if( Submission.titleAttribute.equals(sa.getName()) )
     {
      saitr.remove();

      si.getSubmission().setTitle( sa.getValue() );
     }

    }

    if( rootPathAttr != null )
    {
     String path = StringUtils.stripLeadingSlashes(rootPathAttr);

     if( path.length() > 0 )
      relPath  = FileSystems.getDefault().getPath(path).normalize();
    }
    
    Path basePath = usrPath;
    
    if( relPath != null )
    {
     basePath = usrPath.resolve(relPath).normalize();
     
     if( ! basePath.startsWith(usrPath) )
     {
      si.getLogNode().log(Level.ERROR, "Invalid submission root path '" + rootPathAttr + "'");
      submOk = false;
      break;
     }

    }
    
    
    for(FileOccurrence foc : si.getFileOccurrences())
    {
     FilePointer fp = fileMngr.checkFileExist(foc.getFileRef().getName(), basePath);

     if(fp != null || (oldSbm != null && (fp = fileMngr.checkFileExist(foc.getFileRef().getName(), oldSbm)) != null))
      foc.setFilePointer(fp);
     else
     {
      foc.getLogNode().log(Level.ERROR, "File reference '" + foc.getFileRef().getName() + "' can't be resolved. Check files in the user directory");
      submOk = false;
     }
    }

    if(si.getAccNoPrefix() == null && si.getAccNoSuffix() == null && oldSbm == null )
    {
     if(!checkSubmissionIdUniq(si.getSubmission().getAccNo(), em))
     {
      si.getLogNode().log(Level.ERROR, "Submission accession number '" + si.getSubmission().getAccNo() + "' is already taken by another submission");
      submOk = false;
     }
    }

    for(SectionOccurrence seco : si.getGlobalSections())
    {
     if(seco.getPrefix() == null && seco.getSuffix() == null && (globSecId == null || !globSecId.contains(seco.getSection().getAccNo())))
     {
      if(!checkSectionIdUniq(seco.getSection().getAccNo(), em))
      {
       seco.getSecLogNode().log(Level.ERROR, "Section accession number '" + seco.getSection().getAccNo() + "' is taken by another section");
       submOk = false;
      }
     }
    }

   }

   if(!submOk || validateOnly )
   {
    SimpleLogNode.setLevels(gln);
    
    if( validateOnly )
     submComplete=true;
    
    return gln;
   }

  
   for(SubmissionInfo si : doc.getSubmissions())
   {
    Submission subm =  si.getSubmission();   
    
    if(!validateOnly && (si.getAccNoPrefix() != null || si.getAccNoSuffix() != null))
    {
     while(true)
     {
      String newAcc = getNextAccNo(si.getAccNoPrefix(), si.getAccNoSuffix(), em);

      if(checkSubmissionIdUniq(newAcc, em))
      {
       subm.setAccNo(newAcc);
       break;
      }
     }
    }
    
    if( subm.getTitle() == null )
     subm.setTitle( subm.getAccNo()+" "+SimpleDateFormat.getDateTimeInstance().format( new Date(subm.getCTime()) ) );

    for(SectionOccurrence seco : si.getGlobalSections())
    {
     if(seco.getPrefix() != null || seco.getSuffix() != null)
     {

      while(true)
      {
       String newAcc = getNextAccNo(seco.getPrefix(), seco.getSuffix(), em);

       if(checkSectionIdUniqTotal(newAcc, em))
       {
        seco.getSection().setAccNo(newAcc);
        break;
       }
      }
     }
    }

    if(si.getOriginalSubmission() != null)
     si.getOriginalSubmission().setVersion(-si.getOriginalSubmission().getVersion());

    em.persist(subm);

   }

   submComplete=true;
   
   trans = new ArrayList<JPASubmissionManager.FileTransactionUnit>( doc.getSubmissions().size() );
   
   if( ! prepareFileTransaction(fileMngr, trans, doc.getSubmissions(), trnPath) )
   {
    gln.log(Level.ERROR, "File operation failed. Contact system administrator");
    submOk = false;
   }
   
  }
  catch( Throwable t )
  {
   gln.log(Level.ERROR, "Exception: "+t.getMessage());

   t.printStackTrace();
  }
  finally
  {
   if(!submOk || !submComplete)
   {
    if(em.getTransaction().isActive())
     em.getTransaction().rollback();
    
    gln.log(Level.ERROR, "Submit/Update operation failed. Rolling transaction back");
    
    if( trans != null )
     rollbackFileTransaction(fileMngr, trans, trnPath);
    
    return gln;
   }
   else
   {
    try
    {
     em.getTransaction().commit();
     
     try
     {
      commitFileTransaction(fileMngr, trans, trnPath);
     }
     catch( IOException ioe )
     {
      String err = "File transaction commit failed: " + ioe.getMessage();

      gln.log(Level.ERROR, err);
      log.error(err);
      
      ioe.printStackTrace();
      
      return gln;
     }
    }
    catch(Throwable t)
    {
     String err = "Database transaction commit failed: " + t.getMessage();

     gln.log(Level.ERROR, err);
     log.error(err);

     t.printStackTrace();
     
     if(em.getTransaction().isActive())
      em.getTransaction().rollback();

     if( trans != null )
      rollbackFileTransaction(fileMngr, trans, trnPath);

     return gln;
    }
    
    gln.log(Level.INFO, "Database transaction successful");
   }
  }
  
  
  return gln;
 }
 
 private void rollbackFileTransaction( FileManager fileMngr, List<FileTransactionUnit> trans,  Path trnPath )
 {
  for(FileTransactionUnit ftu : trans)
  {
   try
   {
    if(ftu.historyPathTmp != null)
    {
     if(Files.isSymbolicLink(ftu.submissionPath))
     {
      Files.delete(ftu.submissionPath);
      Files.move(ftu.historyPathTmp, ftu.submissionPath);
     }
     else
     {
      fileMngr.deleteDirectory(ftu.historyPathTmp);
     }
    }

    if(ftu.submissionPathTmp != null)
    {
     fileMngr.deleteDirectory(ftu.submissionPathTmp);
    }
   }
   catch(Exception e)
   {
    log.error("File operation error: " + e.getMessage());
    e.printStackTrace();
   }
  }
  
  try
  {
   fileMngr.deleteDirectory(trnPath);
  }
  catch(Exception e)
  {
   log.error("Can't delete transaction directory: '"+trnPath+"' "+ e.getMessage());
   e.printStackTrace();
  }
  
 }
 
 private void commitFileTransaction( FileManager fileMngr, List<FileTransactionUnit> trans,  Path trnPath ) throws IOException
 {
  for(FileTransactionUnit ftu : trans)
  {

   Path dirToDel = null;
   
   if( ftu.state == SubmissionDirState.COPIED )
   {
    Files.move(ftu.historyPathTmp, ftu.historyPath);

    dirToDel = ftu.submissionPathTmp.getParent().resolve(ftu.submissionPath.getFileName() + "~");
    Files.move(ftu.submissionPath, dirToDel);
   }
   else if( ftu.state == SubmissionDirState.HOME )
    Files.move(ftu.submissionPath, ftu.historyPath);
   else if( ftu.state == SubmissionDirState.LINKED )
   {
    Files.move(ftu.historyPathTmp, ftu.historyPath);
    Files.delete(ftu.submissionPath);
   }
   

   if( ftu.submissionPathTmp != null )
    Files.move(ftu.submissionPathTmp, ftu.submissionPath);

   if(dirToDel != null)
   {
    try
    {
     fileMngr.deleteDirectory(dirToDel);
    }
    catch(Exception ex3)
    {
     log.error("Can't delete directory of dirsymlink: " + dirToDel + " " + ex3.getMessage());
    }
   }

  }

  try
  {
   Files.delete(trnPath);
  }
  catch(Exception ex4)
  {
   log.error("Can't delete directory : " + trnPath + " " + ex4.getMessage());
  }
  
 }
 
 private boolean prepareFileTransaction( FileManager fileMngr, List<FileTransactionUnit> trans, Collection<SubmissionInfo> subs, Path trnPath )
 {

  for( SubmissionInfo si : subs )
  {
   
   FileTransactionUnit ftu = new FileTransactionUnit();
   trans.add(ftu);

   
   Path origDir = BackendConfig.getSubmissionPath( si.getSubmission() );
   ftu.submissionPath = origDir;

   ftu.state =SubmissionDirState.ABSENT;
   
   if(si.getOriginalSubmission() != null)
   {
    Path histDir = BackendConfig.getSubmissionHistoryPath(si.getOriginalSubmission());


    if(Files.exists(origDir))
    {
     ftu.historyPath = histDir;

     Path histDirTmp = histDir.resolveSibling(histDir.getFileName() + "#tmp");

     try
     {
      fileMngr.moveDirectory(origDir, histDirTmp); // trying to submission directory to the history dir
      ftu.historyPathTmp = histDirTmp;

      try
      {
       Files.createSymbolicLink(origDir, histDirTmp); //to provide access to the submission before the commit
       ftu.state =SubmissionDirState.LINKED;
      }
      catch(Exception ex2)
      {
       fileMngr.moveDirectory(histDirTmp, origDir); //if we can't make a symbolic link (FAT?) let's return the directory back
       ftu.historyPathTmp = null; // Signaling that the directory was not neither moved nor copied
       ftu.state =SubmissionDirState.HOME;
      }
     }
     catch(Exception e)
     {
      // If we can't move the directory we have to make a copy of it

      try
      {
       fileMngr.copyDirectory(origDir, histDirTmp);
       ftu.historyPathTmp = histDirTmp;
       ftu.state =SubmissionDirState.COPIED;
      }
      catch(Exception ex1)
      {
       log.error("Can't copy directory " + origDir + " to " + histDirTmp + " : " + ex1.getMessage());

       return false; // Bad. We have to break the operation
      }

     }
    }

   }

   if( si.getFileOccurrences().size() == 0 )
    continue;

   
   Path trnSbmPath = trnPath.resolve(si.getSubmission().getAccNo());

   try
   {
    Files.createDirectories(trnSbmPath);
    ftu.submissionPathTmp = trnSbmPath;
   }
   catch(IOException e1)
   {
    log.error("Create submission transaction dir (" + trnSbmPath + ") error. " + e1.getMessage());
    e1.printStackTrace();

    return false;
   }

   try
   {
    if(BackendConfig.getServiceManager().getSecurityManager().mayEveryoneReadSubmission(si.getSubmission()))
     Files.setPosixFilePermissions(trnSbmPath, rwxrwxr_x);
    else
     Files.setPosixFilePermissions(trnSbmPath, rwxrwx___);
   }
   catch(UnsupportedOperationException ex)
   {
   }
   catch(IOException e1)
   {
    log.error("Submission dir (" + trnSbmPath + ") set permissions error. " + e1.getMessage());
    e1.printStackTrace();

    return false;
   }

   Path sbmFilesPath = trnSbmPath.resolve(BackendConfig.SubmissionFilesDir);

   if(si.getFileOccurrences() != null)
   {
    for(FileOccurrence fo : si.getFileOccurrences())
    {
     try
     {
      fileMngr.linkOrCopy(sbmFilesPath, fo.getFilePointer());
      si.getLogNode().log(Level.INFO, "File '" + fo.getFileRef().getName() + "' transfer success");
     }
     catch(IOException e)
     {
      log.error("File " + fo.getFilePointer() + " transfer error: " + e.getMessage());
      return false;
     }
    }
   }
  }
  
  return true;
 }

 private String convertToText( byte[] data, String charset, LogNode ln )
 {
  Charset cs = null;
  
  if( charset == null )
  {
   cs = Charset.forName("utf-8");
   ln.log(Level.WARN, "Charset isn't specified. Assuming default 'utf-8'");
  }
  else
  {
   try
   {
    cs = Charset.forName(charset);
   }
   catch(Exception e)
   {
    ln.log(Level.ERROR, "System doen't support charser: '"+charset+"'");
    return null;
   }
  }
  
  return new String(data,cs);
 }

 
/*
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
  
  boolean submOk=true;
  
  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  PMDoc doc=null;
  
  try
  {

   em.getTransaction().begin();

   try
   {
    doc = parser.parse(txt, gln);
   }
   catch(ParserException e)
   {
    gln.log(Level.ERROR, "Parser exception: " + e.getMessage());
    SimpleLogNode.setLevels(gln);
    submOk=false;
    return gln;
   }

   SimpleLogNode.setLevels(gln);

   if(gln.getLevel() == Level.ERROR)
   {
    submOk=false;
    return gln;
   }
   
   for(SubmissionInfo si : doc.getSubmissions())
   {
    si.getSubmission().setOwner(usr);

    Set<String> globSecId = null;
    Submission oldSbm = null;

    long ts = System.currentTimeMillis() / 1000;

    si.getSubmission().setMTime(ts);

    if(update)
    {
     if(si.getAccNoPrefix() != null || si.getAccNoSuffix() != null || si.getSubmission().getAccNo() == null)
     {
      si.getLogNode().log(Level.ERROR, "Submission must have accession number for update operation");
      submOk = false;
      continue;
     }

     oldSbm = getSubmissionByAcc(si.getSubmission().getAccNo(), em);

     if(oldSbm == null)
     {
      si.getLogNode().log(Level.ERROR, "Submission '" + si.getSubmission().getAccNo() + "' doesn't exist and can't be updated");
      submOk = false;
      continue;
     }

     si.getSubmission().setCTime(oldSbm.getCTime());
     si.setOriginalSubmission(oldSbm);
     si.getSubmission().setVersion(oldSbm.getVersion() + 1);

     if(!BackendConfig.getServiceManager().getSecurityManager().mayUserUpdateSubmission(oldSbm, usr))
     {
      si.getLogNode().log(Level.ERROR, "Submission update is not permitted for this user");
      submOk = false;
      continue;
     }

     globSecId = new HashSet<String>();

     collectGlobalSecIds(oldSbm.getRootSection(), globSecId);
    }
    else
    {
     si.getSubmission().setCTime(ts);
     si.getSubmission().setVersion(1);
    }

    boolean rTimeFound = false;
    for(SubmissionAttribute sa : si.getSubmission().getAttributes())
    {
     if(Submission.releaseDateAttribute.equals(sa.getName()))
     {
      if(rTimeFound)
      {
       si.getLogNode().log(Level.ERROR, "Multiple '" + Submission.releaseDateAttribute + "' attributes are not allowed");
       break;
      }

      rTimeFound = true;

      String val = sa.getValue();

      if(val != null)
      {
       val = val.trim();

       if(val.length() > 0)
       {
        Matcher mtch = rTimePattern.matcher(val);

        if(!mtch.matches())
         si.getLogNode().log(Level.ERROR,
           "Invalid '" + Submission.releaseDateAttribute + "' attribute value. Expected date in format: YYYY-MM-DD[Thh:mm[:ss[.mmm]]]");
        else
        {
         Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

         cal.set(Calendar.YEAR, Integer.parseInt(mtch.group("year")));
         cal.set(Calendar.MONTH, Integer.parseInt(mtch.group("month")) - 1);
         cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mtch.group("day")));

         String str = mtch.group("hour");

         if(str != null)
          cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str));

         str = mtch.group("min");

         if(str != null)
          cal.set(Calendar.MINUTE, Integer.parseInt(str));

         str = mtch.group("sec");

         if(str != null)
          cal.set(Calendar.SECOND, Integer.parseInt(str));

         si.getSubmission().setRTime(cal.getTimeInMillis() / 1000);
        }
       }
      }

     }
    }

    for(FileOccurrence foc : si.getFileOccurrences())
    {
     FilePointer fp = fileMngr.checkFileExist(foc.getFileRef().getName(), usr);

     if(fp != null || (update && (fp = fileMngr.checkFileExist(foc.getFileRef().getName(), oldSbm)) != null))
      foc.setFilePointer(fp);
     else
     {
      foc.getLogNode().log(Level.ERROR, "File reference '" + foc.getFileRef().getName() + "' can't be resolved. Check files in the user directory");
      submOk = false;
     }
    }

    if(si.getAccNoPrefix() == null && si.getAccNoSuffix() == null && !update)
    {
     if(!checkSubmissionIdUniq(si.getSubmission().getAccNo(), em))
     {
      si.getLogNode().log(Level.ERROR, "Submission accession number '" + si.getSubmission().getAccNo() + "' is already taken by another submission");
      submOk = false;
     }
    }

    for(SectionOccurrence seco : si.getGlobalSections())
    {
     if(seco.getPrefix() == null && seco.getSuffix() == null && (globSecId == null || !globSecId.contains(seco.getSection().getAccNo())))
     {
      if(!checkSectionIdUniq(seco.getSection().getAccNo(), em))
      {
       seco.getSecLogNode().log(Level.ERROR, "Section accession number '" + seco.getSection().getAccNo() + "' is taken by another section");
       submOk = false;
      }
     }
    }

   }

   if(!submOk)
   {
    SimpleLogNode.setLevels(gln);
    return gln;
   }

   for(SubmissionInfo si : doc.getSubmissions())
   {

    if(si.getAccNoPrefix() != null || si.getAccNoSuffix() != null)
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

    if(si.getOriginalSubmission() != null)
     si.getOriginalSubmission().setVersion(-si.getOriginalSubmission().getVersion());

    em.persist(si.getSubmission());

   }
  }
  finally
  {
   if( ! submOk )
   {
    if(em.getTransaction().isActive())
     em.getTransaction().rollback();
   }
   else
   {
    try
    {
     em.getTransaction().commit();
    }
    catch(Throwable t)
    {
     String err = "Database transaction commit failed: " + t.getMessage();

     gln.log(Level.ERROR, err);

     if(em.getTransaction().isActive())
      em.getTransaction().rollback();

     return gln;
    }
   }
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
*/

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
  
  List<?> res = q.getResultList();
  
  if( res.size() == 0 )
   return null;
  
  return (Submission)res.get(0);
 }


 private boolean checkSubmissionIdUniq(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Submission.countByAcc");
  q.setParameter("accNo", accNo);
  
  return ((Number)q.getSingleResult()).intValue() == 0;
 }

 private boolean checkSectionIdUniq(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Section.countByAccActive");
  q.setParameter("accNo", accNo);
  
  return ((Number)q.getSingleResult()).intValue() == 0;
 }

 private boolean checkSectionIdUniqTotal(String accNo, EntityManager em)
 {
  Query q =  em.createNamedQuery("Section.countByAcc");
  q.setParameter("accNo", accNo);
  
  return ((Number)q.getSingleResult()).intValue() == 0;
 }


/*
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
*/
}
