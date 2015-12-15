package uk.ac.ebi.biostd.webapp.server.mng.impl;

import static uk.ac.ebi.biostd.in.pageml.PageMLAttributes.ACCNO;
import static uk.ac.ebi.biostd.in.pageml.PageMLAttributes.ID;
import static uk.ac.ebi.biostd.in.pageml.PageMLElements.SUBMISSION;
import static uk.ac.ebi.biostd.util.StringUtils.xmlEscaped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.db.TagResolver;
import uk.ac.ebi.biostd.idgen.Counter;
import uk.ac.ebi.biostd.idgen.IdGen;
import uk.ac.ebi.biostd.in.AccessionMapping;
import uk.ac.ebi.biostd.in.ElementPointer;
import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.in.SubmissionMapping;
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
import uk.ac.ebi.biostd.model.SubmissionAttributeException;
import uk.ac.ebi.biostd.out.cell.CellFormatter;
import uk.ac.ebi.biostd.out.cell.XSVCellStream;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.out.pageml.PageMLFormatter;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.util.FilePointer;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;
import uk.ac.ebi.biostd.webapp.server.mng.SubmissionManager;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;
import uk.ac.ebi.biostd.webapp.server.util.ExceptionUtil;

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

 private Set<String> lockedSmbIds = new HashSet<String>();
 private Set<String> lockedSecIds = new HashSet<String>();
 
 private boolean shutDownManager = false;
 
 private ParserConfig parserCfg;
 private UpdateQueueProcessor queueProc = null;
 private boolean shutdown;
 
 public JPASubmissionManager(EntityManagerFactory emf)
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  shutdown=false;

  parserCfg = new ParserConfig();
  
  parserCfg.setMultipleSubmissions(true);
  
  if( BackendConfig.getSubmissionUpdatePath() != null )
  {
   queueProc = new UpdateQueueProcessor();
   queueProc.start();
  }
  
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
  catch( Throwable t )
  {
   t.printStackTrace();
   log.error("DB error: "+t.getMessage());
  }
  finally
  {
   if( trn.isActive() )
    trn.commit();
  }
  
  return null;
 }

 @Override
 public LogNode deleteSubmissionByAccession( String acc, User usr )
 {
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Deleting submission '"+acc+"'", ec);

  if( shutdown )
  {
   gln.log(Level.ERROR, "Service is shut down");
   return gln;
  } 
  
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  
  Path origDir = null;
  Path histDir = null;
  Path histDirTmp = null;
  
  SubmissionDirState dirOp = SubmissionDirState.ABSENT; // 0 - not changed, 1 - moved, 2 - copied, 3 = error
  
  boolean trnOk=true;

  Submission sbm = null;

  try
  {
   em.getTransaction().begin();

   Query q = em.createNamedQuery("Submission.getByAcc");

   q.setParameter("accNo", acc);


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

   histDir = BackendConfig.getSubmissionHistoryPath(sbm);

   if(Files.exists(origDir))
   {
    histDirTmp = histDir.resolveSibling(histDir.getFileName() + "#tmp");

    try
    {
     fileMngr.moveDirectory(origDir, histDirTmp); // trying to submission directory to the history dir
     dirOp = SubmissionDirState.LINKED;
    }
    catch(Exception e)
    {
     // If we can't move the directory we have to make a copy of it

     try
     {
      fileMngr.copyDirectory(origDir, histDirTmp);
      dirOp=SubmissionDirState.COPIED;
     }
     catch(Exception ex1)
     {
      log.error("Can't copy directory " + origDir + " to " + histDirTmp + " : " + ex1.getMessage());
      gln.log(Level.ERROR, "File operation error. Contact system administrator");
      
      dirOp = null;
      
      return gln; // Bad. We have to break the operation
     }

    }
   }
   
   sbm.setMTime(System.currentTimeMillis() / 1000);
   sbm.setVersion(-sbm.getVersion());
  }
  finally
  {
   
   try
   {
    if( dirOp != null )
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
   {
    if( dirOp != SubmissionDirState.ABSENT )
    {
     try
     {
      fileMngr.moveDirectory( histDirTmp, histDir );
     }
     catch(IOException e)
     {
      log.error("History directory '"+histDirTmp+"' rename failed: "+e);
      e.printStackTrace();
     }
    }
    
    gln.log(Level.INFO, "Transaction successful");
   }
   else
   {
    if( dirOp == SubmissionDirState.LINKED )
    {
     try
     {
      fileMngr.moveDirectory( histDirTmp, origDir );
     }
     catch(IOException e)
     {
      log.error("Delete opration rollback (move dir) failed: "+e);
      e.printStackTrace();
      
      gln.log(Level.ERROR, "Severe server problem. Please inform system administrator. The database may be inconsistent");
     }

    }
    else if( dirOp == SubmissionDirState.COPIED )
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
  
  if( trnOk && BackendConfig.getPublicFTPPath() != null )
  {
   Path ftpPath = BackendConfig.getSubmissionPublicFTPPath(sbm);
   
   if( Files.exists(ftpPath) )
   {
    try
    {
     fileMngr.deleteDirectory(ftpPath);
    }
    catch(Exception e)
    {
     log.error("Can't delete public ftp directory "+ftpPath+" Error: "+e.getMessage());
     e.printStackTrace();
     gln.log(Level.WARN, "Public FTP directory was not deleted");
    }
   }
   
   if( trnOk && queueProc != null )
   {
    StringBuilder out = new StringBuilder();
    
    
    out.append('<').append(SUBMISSION.getElementName()).append(' ').append(ACCNO.getAttrName()).append("=\"");
    
    try
    {
     xmlEscaped(sbm.getAccNo(), out);
    }
    catch(IOException e)
    {
    }

    out.append("\" ").append(ID.getAttrName()).append("=\"").append( String.valueOf(sbm.getId()) );
    out.append("\" delete=\"true\"/>\n");

    String msg = out.toString();
    
    while( ! shutdown )
    {
     try
     {
      queueProc.put(msg);
      break;       
     }
     catch(InterruptedException e)
     {
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

   q.setParameter("accNo", acc);
   
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
 public SubmissionReport createSubmission( byte[] data, DataFormat type, String charset, Operation op, User usr, boolean validateOnly )
 {
  try
  {
   return createSubmissionUnsafe(data, type, charset, op, usr, validateOnly);
  }
  catch(Throwable e)
  {
   ExceptionUtil.unroll(e).printStackTrace();
   throw e;
  }
 }
 
 private PMDoc parseDocument(byte[] data, DataFormat type, String charset, TagResolver tagRslv, LogNode gln)
 {
  PMDoc doc = null;
  SpreadsheetReader reader = null;

  switch(type)
  {
   case xml:

    gln.log(Level.ERROR, "XML submission are not supported yet");

    return null;

   case json:

    String txt = convertToText(data, charset, gln);

    if(txt != null)
     doc = new JSONReader(tagRslv, parserCfg).parse(txt, gln);

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
    return null;

  }

  SimpleLogNode.setLevels(gln);

  if(gln.getLevel() == Level.ERROR)
   return null;

  if(reader != null)
  {
   PageTabSyntaxParser prs = new PageTabSyntaxParser(tagRslv, parserCfg);

   try
   {
    doc = prs.parse(reader, gln);
   }
   catch(ParserException e)
   {
    gln.log(Level.ERROR, "Parser exception: " + e.getMessage());
    SimpleLogNode.setLevels(gln);
    return null;
   }
  }
  
  return doc;

 }
 
 private boolean checkAccNoPfxSfx( SubmissionInfo si )
 {
  boolean submOk = true;
  
  try
  {
   si.setAccNoPrefix( checkAccNoPart(si.getAccNoPrefix()) );
  }
  catch(Exception e)
  {
   si.getLogNode().log(Level.ERROR, "Submission accession number prefix contains invalid characters");
   submOk = false;
  }
  
  try
  {
   si.setAccNoSuffix( checkAccNoPart(si.getAccNoSuffix()) );
  }
  catch(Exception e)
  {
   si.getLogNode().log(Level.ERROR, "Submission accession number prefix contains invalid characters");
   submOk = false;
  }
  
  Submission submission = si.getSubmission();

  
  if( si.getAccNoPrefix() == null && si.getAccNoSuffix() == null )
  {
   try
   {
    submission.setAccNo( checkAccNoPart(submission.getAccNo()) );
   }
   catch(Exception e)
   {
    si.getLogNode().log(Level.ERROR, "Submission accession number contains invalid characters");
    submOk = false;
   }
  
   if( submission.getAccNo() == null )
   {
    si.setAccNoPrefix( BackendConfig.getDefaultSubmissionAccPrefix() );
    si.setAccNoSuffix( BackendConfig.getDefaultSubmissionAccSuffix() );
   }
  }
  
  return submOk;
 }
 
 private SubmissionReport createSubmissionUnsafe( byte[] data, DataFormat type, String charset, Operation op, User usr, boolean validateOnly )
 {
  ErrorCounter ec = new ErrorCounterImpl();

  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, op.name() + " submission(s) from " + type.name() + " source", ec);


  
  SubmissionReport res = new SubmissionReport();
  
  res.setLog(gln);
  
  if( shutdown )
  {
   gln.log(Level.ERROR, "Service is shut down");
   return res;
  } 
  
  if( op == Operation.CREATE && !BackendConfig.getServiceManager().getSecurityManager().mayUserCreateSubmission(usr) )
  {
   gln.log(Level.ERROR, "User has no permission to create submissions");
   return res;
  }

  gln.log(Level.INFO, "Processing '" + type.name() + "' data. Body size: " + data.length);

  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  boolean submOk = true;
  boolean submComplete = false;

  PMDoc doc = null;

  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  Path trnPath = BackendConfig.getSubmissionsTransactionPath().resolve(BackendConfig.getInstanceId()+"#"+BackendConfig.getSeqNumber());

  List<FileTransactionUnit> trans = null;
  LockedIdSet locked = null;
  
  try
  {
   doc = parseDocument(data, type, charset, new TagResolverImpl(em), gln);
   

   if( doc == null )
   {
    submOk = false;
    return res;
   }
   

   
   if( doc.getSubmissions() == null || doc.getSubmissions().size() == 0 )
   {
    gln.log(Level.ERROR, "There are no submissions in the document");    
    SimpleLogNode.setLevels(gln);
    submOk = false;
    return res;
   }

   Path usrPath = BackendConfig.getUserDirPath(usr);
   
   Map<String, ElementPointer> smbIdMap = checkSubmissionAccNoUniq(doc);
   Map<String, ElementPointer> secIdMap = checkSectionAccNoUniq(doc);
   
   if( smbIdMap != null && secIdMap != null )
   {
    locked = waitForIdUnlocked(smbIdMap,secIdMap);
   }
   else
    submOk = false;
   
   
   em.getTransaction().begin();
   

   
   for(SubmissionInfo si : doc.getSubmissions())
   {
    Submission submission = si.getSubmission();
    
    submission.setOwner(usr);

    submOk = submOk && checkAccNoPfxSfx(si);

     

    Set<String> goingGlobSecId = null;

    long ts = System.currentTimeMillis() / 1000;

    submission.setMTime(ts);

    Submission oldSbm = null;

    
    
    if( op == Operation.UPDATE || op == Operation.REPLACE )
    {
     if( (si.getAccNoPrefix() != null || si.getAccNoSuffix() != null || submission.getAccNo() == null ) && op == Operation.REPLACE )
     {
      si.getLogNode().log(Level.ERROR, "Submission must have accession number for "+op.name()+" operation");
      submOk = false;
      continue;
     }

     oldSbm = getSubmissionByAcc(submission.getAccNo(), em);

     if( oldSbm == null )
     {
      if( op == Operation.REPLACE )
      {
       si.getLogNode().log(Level.ERROR, "Submission '" + submission.getAccNo() + "' doesn't exist and can't be replced");
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
       
       submission.setCTime(ts);
       submission.setVersion(1);
      }
     }
     else
     {
      submission.setCTime(oldSbm.getCTime());
      si.setOriginalSubmission(oldSbm);
      submission.setVersion(oldSbm.getVersion() + 1);
      
      if(!BackendConfig.getServiceManager().getSecurityManager().mayUserUpdateSubmission(oldSbm, usr))
      {
       si.getLogNode().log(Level.ERROR, "Submission update is not permitted for this user");
       submOk = false;
       continue;
      }
      
      goingGlobSecId = new HashSet<String>();
      
      collectGlobalSecIds(oldSbm.getRootSection(), goingGlobSecId);
     }
     
    }
    else
    {
     submission.setCTime(ts);
     submission.setVersion(1);
    }

   
    
    try
    {
     submission.normalizeAttributes();
    }
    catch(SubmissionAttributeException e)
    {
     si.getLogNode().log(Level.ERROR, e.getMessage());
     submOk = false;
    }
    
    if( submission.isRTimeSet() && submission.getRTime()*1000 < System.currentTimeMillis() )
    {
     boolean pub=false;
     
     if( submission.getAccessTags() != null )
     {
      for( AccessTag t : submission.getAccessTags() )
      {
       if( t.getName().equals( BackendConfig.PublicTag ) )
       {
        pub=true;
        break;
       }
      }
      
     }

     if( !pub )
      submission.addAccessTag( getPublicTag(em) );
     
     submission.setRTime( System.currentTimeMillis() / 1000 );
    }
    
    Path relPath = null;
    String rootPathAttr = submission.getRootPath();

    
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
    
    
    if( submission.getAccessTags() != null )
    {
     for( AccessTag t : submission.getAccessTags() )
     {
      if( t.getName().equals( BackendConfig.PublicTag ) )
      {
       submission.setRTime(System.currentTimeMillis() / 1000 );
       submission.setReleased(true);
       break;
      }
     }
    }
    
    if( si.getFileOccurrences() != null )
    {
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
    }

    if(si.getAccNoPrefix() == null && si.getAccNoSuffix() == null && oldSbm == null && submission.getAccNo() != null )
    {
     if(!checkSubmissionIdUniq(submission.getAccNo(), em))
     {
      si.getLogNode().log(Level.ERROR, "Submission accession number '" + submission.getAccNo() + "' is already taken by another submission");
      submOk = false;
     }
    }

    List<SubmissionAttribute> sattrs = si.getSubmission().getAttributes();
    
    if( sattrs != null )
    {
     for( SubmissionAttribute sa : sattrs )
     {
      if( ! sa.getName().equals(Submission.attachToAttribute) )
       continue;
      
      String pAcc = sa.getValue();
      
      if( pAcc == null || (pAcc=pAcc.trim()).length() == 0 )
       continue;
      
      Submission s = getSubmissionByAcc(pAcc, em);
      
      if( s == null )
      {
       si.getLogNode().log(Level.ERROR, "Submission attribute '" + Submission.attachToAttribute + "' points to non existing submission '"+pAcc+"'");
       submOk = false;
       
       continue;
      }

      if( !BackendConfig.getServiceManager().getSecurityManager().mayUserAttachToSubmission(s,usr) )
      {
       si.getLogNode().log(Level.ERROR, "User has no permission to attach to submission: "+pAcc);
       submOk = false;
       continue;
      }
     }
    }
    
    if( si.getGlobalSections() != null )
    {
     for(SectionOccurrence seco : si.getGlobalSections())
     {
      try
      {
       seco.setPrefix(checkAccNoPart(seco.getPrefix()));
      }
      catch(Exception e)
      {
       seco.getSecLogNode().log(Level.ERROR, "Section accession number prefix contains invalid characters");
       submOk = false;
      }

      try
      {
       seco.setSuffix(checkAccNoPart(seco.getSuffix()));
      }
      catch(Exception e)
      {
       seco.getSecLogNode().log(Level.ERROR, "Section accession number prefix contains invalid characters");
       submOk = false;
      }

      if(seco.getSuffix() == null && seco.getPrefix() == null)
      {
       try
       {
        seco.getSection().setAccNo(checkAccNoPart(seco.getSection().getAccNo()));
       }
       catch(Exception e)
       {
        seco.getSecLogNode().log(Level.ERROR, "Section accssesion number contains invalid characters");
        submOk = false;
       }

      }

      if(seco.getPrefix() == null && seco.getSuffix() == null && seco.getSection().getAccNo() != null
        && (goingGlobSecId == null || !goingGlobSecId.contains(seco.getSection().getAccNo())))
      {
       if(!checkSectionIdUniq(seco.getSection().getAccNo(), em))
       {
        seco.getSecLogNode().log(Level.ERROR, "Section accession number '" + seco.getSection().getAccNo() + "' is taken by another section");
        submOk = false;
       }
      }
     }
    }

   }

   if(!submOk || validateOnly )
   {
    SimpleLogNode.setLevels(gln);
    
    if( validateOnly )
     submComplete=true;
    
    return res;
   }

  
   int sbmNo=0;
   
   for(SubmissionInfo si : doc.getSubmissions())
   {
    sbmNo++;
    
    Submission subm =  si.getSubmission();   

    SubmissionMapping sMap = new SubmissionMapping();
    sMap.getSubmissionMapping().setOrigAcc(si.getAccNoOriginal());
    sMap.getSubmissionMapping().setPosition( new int[]{sbmNo} );
    
    res.addSubmissionMapping(sMap);
    
    if(!validateOnly && (si.getAccNoPrefix() != null || si.getAccNoSuffix() != null || subm.getAccNo() == null ))
    {
     while(true)
     {
      try
      {
       String newAcc = BackendConfig.getServiceManager().getAccessionManager().getNextAccNo(si.getAccNoPrefix(), si.getAccNoSuffix(), usr);

       if(checkSubmissionIdUniq(newAcc, em))
       {
        subm.setAccNo(newAcc);
        si.getLogNode().log(Level.INFO, "Submission generated accNo: " + newAcc);

        sMap.getSubmissionMapping().setAssignedAcc(subm.getAccNo());

        break;
       }
      }
      catch(SecurityException e)
      {
       si.getLogNode().log(Level.ERROR, "User has no permission to generate accession number: " + e.getMessage());
       submOk = false;
       break;
      }
     }
    }
    
    if( subm.getTitle() == null )
     subm.setTitle( subm.getAccNo()+" "+SimpleDateFormat.getDateTimeInstance().format( new Date(subm.getCTime()) ) );

    if( si.getGlobalSections() != null  )
    {
     for(SectionOccurrence seco : si.getGlobalSections())
     {
      if(!validateOnly && (seco.getPrefix() != null || seco.getSuffix() != null || seco.getSection().getAccNo() == null))
      {
       String localId = seco.getLocalId();
       
       while(true)
       {
        String newAcc = null;
        try
        {
         newAcc = BackendConfig.getServiceManager().getAccessionManager().getNextAccNo(seco.getPrefix(), seco.getSuffix(), usr);

         if(checkSectionIdUniqTotal(newAcc, em))
         {
          seco.getSection().setAccNo(newAcc);
          seco.getSecLogNode().log(Level.INFO, "Section generated accNo: " + newAcc);
          break;
         }
        }
        catch( SecurityException e)
        {
         seco.getSecLogNode().log(Level.ERROR, "User has no permission to generate accession number: " + e.getMessage());
         submOk = false;
         break;
        }

       }
       
       AccessionMapping secMap = new AccessionMapping();
       
       secMap.setAssignedAcc(seco.getSection().getAccNo());
       secMap.setOrigAcc(localId);
       
       int[] pth = new int[seco.getPath().size()+1];
       
       int i=0;
       
       pth[i++] = sbmNo;
       
       for( SectionOccurrence ptoc : seco.getPath() )
        pth[i++]=ptoc.getPosition();
       
       secMap.setPosition(pth);
       
       sMap.addSectionMapping( secMap );
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
   gln.log(Level.ERROR, "Internal server error");

   t.printStackTrace();
   log.error("Exception during submission process: "+t.getMessage());
   
   submOk = false;
  }
  finally
  {
   try
   {
    
    if(!submOk || !submComplete)
    {
     if(em.getTransaction().isActive())
      em.getTransaction().rollback();

     gln.log(Level.ERROR, "Submit/Update operation failed. Rolling transaction back");

     if(trans != null)
      rollbackFileTransaction(fileMngr, trans, trnPath);

     return res;
    }
    else
    {
     try
     {
      em.getTransaction().commit();

      if(trans != null)
      {
       try
       {
        commitFileTransaction(fileMngr, trans, trnPath);
       }
       catch(IOException ioe)
       {
        String err = "File transaction commit failed: " + ioe.getMessage();

        gln.log(Level.ERROR, err);
        log.error(err);

        ioe.printStackTrace();

        return res;
       }
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

      if(trans != null)
       rollbackFileTransaction(fileMngr, trans, trnPath);

      return res;
     }

     gln.log(Level.INFO, "Database transaction successful");
    }
   }
   finally
   {
    unlockIds(locked);

   }
  }
  
  if( trans != null && BackendConfig.getPublicFTPPath() != null )
   copyToPublicFTP( fileMngr, doc.getSubmissions(), gln );
  
  if( queueProc != null )
  {
   StringBuilder sb = new StringBuilder(10000);
   
   for(SubmissionInfo si : doc.getSubmissions())
   {
    sb.setLength(0);
    
    try
    {
     new PageMLFormatter(sb).format(si.getSubmission(), sb);
    }
    catch(IOException e)
    {
    }
    
    String msg = sb.toString();
    
    while( ! shutdown )
    {
     try
     {
      queueProc.put(msg);
      break;       
     }
     catch(InterruptedException e)
     {
     }
    }
   }
   
  }
  
  
  return res;
 }

 
 
 private Map<String, ElementPointer> checkSubmissionAccNoUniq(PMDoc doc)
 {
  Map<String, ElementPointer> idMap = new HashMap<>();
  
  int conflicts=0;
  
  for(SubmissionInfo si : doc.getSubmissions())
  {
   if( si.getAccNoPrefix() != null || si.getAccNoSuffix() != null || si.getSubmission().getAccNo() == null )
    continue;
   
   ElementPointer sbmPtr = idMap.get( si.getSubmission().getAccNo() );
   
   
   if( sbmPtr != null )
   {
    si.getLogNode().log(Level.ERROR, "Accession number '"+si.getSubmission().getAccNo()+" is already taken by submission at "+sbmPtr);
    conflicts++;
   }
   else
    idMap.put(si.getSubmission().getAccNo(), si.getElementPointer());
  }
  
  return conflicts > 0? null : idMap;
 }

 private Map<String, ElementPointer> checkSectionAccNoUniq(PMDoc doc)
 {
  Map<String, ElementPointer> idMap = new HashMap<>();
  
  int conflicts=0;

  
  for(SubmissionInfo si : doc.getSubmissions())
  {
   if( si.getGlobalSections() == null || si.getGlobalSections().size() == 0  )
    continue;
   
   for( SectionOccurrence seco : si.getGlobalSections() )
   {
    if( seco.getPrefix() != null || seco.getSuffix() != null || seco.getSection().getAccNo() == null)
     continue;
    
   ElementPointer secPtr = idMap.get( seco.getSection().getAccNo() );
   
   if( secPtr != null )
   {
    seco.getSecLogNode().log(Level.ERROR, "Accession number '"+seco.getSection().getAccNo()+" is already taken by section at "+secPtr);
    conflicts++;
   }
   else
    idMap.put(seco.getSection().getAccNo(), seco.getElementPointer());
   }
  }
  
  return conflicts > 0? null : idMap;
 }


 
 private LockedIdSet waitForIdUnlocked(Map<String,ElementPointer> sbmIdMap, Map<String,ElementPointer> secIdMap ) throws InterruptedException
 {
  LockedIdSet lckSet = new LockedIdSet();
  
  lckSet.setSubmissionMap(sbmIdMap);
  lckSet.setSectionMap(secIdMap);
  
  if( lckSet.empty() )
   return null;
  
  synchronized(lockedSmbIds)
  {
   while(true)
   {

    boolean needWait = false;

    if( sbmIdMap != null )
    {
     for(String s : sbmIdMap.keySet())
     {
      if(lockedSmbIds.contains(s))
      {
       needWait = true;
       break;
      }
     }
    }
    
    if( secIdMap != null && ! needWait  )
    {
     for(String s : secIdMap.keySet())
     {
      if(lockedSecIds.contains(s))
      {
       needWait = true;
       break;
      }
     }
    }


    if(!needWait)
    {
     if( sbmIdMap != null )
      lockedSmbIds.addAll(sbmIdMap.keySet());
     
     if( secIdMap != null )
     lockedSecIds.addAll(secIdMap.keySet());
     
     return lckSet;
    }
    
    while( true )
    {
     try
     {
      lockedSmbIds.wait();
      break;
     }
     catch(InterruptedException e)
     {
      if( shutDownManager )
       throw e;
     }
    }
   }
  }

 }

 private void unlockIds( LockedIdSet lset )
 {
  if( lset == null )
   return;
  
  synchronized(lockedSmbIds)
  {
   if( lset.getSubmissionMap() != null )
    lockedSmbIds.removeAll(lset.getSubmissionMap().keySet());
   
   if( lset.getSectionMap() != null )
    lockedSecIds.removeAll(lset.getSectionMap().keySet());
   
   lockedSmbIds.notifyAll();
  }
 }

 private AccessTag getPublicTag( EntityManager em )
 {
  Query q = em.createNamedQuery("AccessTag.getByName");
  q.setParameter("name", BackendConfig.PublicTag);
  
  return (AccessTag)q.getSingleResult();
 }
 
 private String checkAccNoPart( String acc ) throws Exception
 {
  if( acc == null )
   return null;
  
  acc = acc.trim();
  
  if( acc.length() == 0 )
   return null;
  
  if(  ! AccNoUtil.checkAccNoStr(acc) )
   throw new Exception("Invalid characters");
  
  return acc;
 }
 
 private void copyToPublicFTP( FileManager fileMngr, List<SubmissionInfo> list, LogNode gln )
 {
  for(SubmissionInfo si : list)
  {
   Path sourceDir = BackendConfig.getSubmissionFilesPath(si.getSubmission());
   Path targetDir = BackendConfig.getSubmissionPublicFTPPath(si.getSubmission());

   try
   {

    if(Files.exists(targetDir))
     fileMngr.deleteDirectory(targetDir);

    if(Files.exists(sourceDir) && BackendConfig.getServiceManager().getSecurityManager().mayEveryoneReadSubmission(si.getSubmission()) )
    {
     fileMngr.linkOrCopyDirectory(sourceDir, targetDir);

     try
     {
      if(BackendConfig.getServiceManager().getSecurityManager().mayEveryoneReadSubmission(si.getSubmission()))
       Files.setPosixFilePermissions(targetDir, BackendConfig.rwxrwxr_x);
      else
       Files.setPosixFilePermissions(targetDir, BackendConfig.rwxrwx___);
     }
     catch(UnsupportedOperationException e)
     {
     }
     catch (IOException e) 
     {
      e.printStackTrace();
     }
    }
   }
   catch(IOException e)
   {
    si.getLogNode().log(Level.WARN, "Submission files were not copied to public FTP directory due to error");
    log.error("Coping to FTP directory error: "+e.getMessage());
    e.printStackTrace();
   }
  }
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
   
   try
   {
    Files.createDirectories(origDir.getParent());
   }
   catch(IOException e2)
   {
    log.error("Can't create directory: " + origDir.getParent());
    return false; // Bad. We have to break the operation
   }
   
   si.getSubmission().setRelPath(BackendConfig.getSubmissionRelativePath(si.getSubmission()));

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
      fileMngr.moveDirectory(origDir, histDirTmp); // trying to move submission directory to the history dir
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
       Files.createDirectories(histDirTmp);
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
   else if( Files.exists( origDir ) )
   {
    log.warn("Directory " + origDir + " exists unexpectedly");

    try
    {
     if(Files.isDirectory(origDir))
      FileUtils.deleteDirectory(origDir.toFile());
     else
      Files.delete(origDir);
    }
    catch(IOException e)
    {
     e.printStackTrace();
     log.error("Can't remove file/directory: " + origDir);
     return false;
    }
   }

   
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
     Files.setPosixFilePermissions(trnSbmPath, BackendConfig.rwxrwxr_x);
    else
     Files.setPosixFilePermissions(trnSbmPath, BackendConfig.rwxrwx___);
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
      
      fo.getFileRef().setSize( fo.getFilePointer().getSize() );
     }
     catch(IOException e)
     {
      log.error("File " + fo.getFilePointer() + " transfer error: " + e.getMessage());
      return false;
     }
    }
   }
  
   PMDoc doc = new PMDoc();
   doc.addSubmission(si);
   
   try( PrintStream out = new PrintStream( trnSbmPath.resolve(si.getSubmission().getAccNo()+".xml").toFile() ) )
   {
    new PageMLFormatter(out).format(doc);
   }
   catch (Exception e) 
   {
    si.getLogNode().log(Level.ERROR,"Can't generate XML source file");
    log.error("Can't generate XML source file: "+e.getMessage());
    e.printStackTrace();
   }
   
   
   try( PrintStream out = new PrintStream( trnSbmPath.resolve(si.getSubmission().getAccNo()+".json").toFile() ) )
   {
    new JSONFormatter(out).format(doc);
   }
   catch (Exception e) 
   {
    si.getLogNode().log(Level.ERROR,"Can't generate JSON source file");
    log.error("Can't generate JSON source file: "+e.getMessage());
    e.printStackTrace();
   }

   try( PrintStream out = new PrintStream( trnSbmPath.resolve(si.getSubmission().getAccNo()+".pagetab.tsv").toFile() ) )
   {
    new CellFormatter( XSVCellStream.getTSVCellStream(out) ).format(doc);
   }
   catch (Exception e) 
   {
    si.getLogNode().log(Level.ERROR,"Can't generate Page-Tab source file");
    log.error("Can't generate Page-Tab source file: "+e.getMessage());
    e.printStackTrace();
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
   
   Counter cnt = new Counter();
   cnt.setMaxCount(0);
   cnt.setName(""+prefix+"000"+suffix);
   
   em.persist(cnt);
   
   gen.setCounter(cnt );
   
   
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
   cnt.setName(""+prefix+"000"+suffix);
   
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


 @Override
 public void shutdown()
 {
  shutdown = true;
  
  if( queueProc != null )
   queueProc.shutdown();
 }


 @Override
 public LogNode tranklucateSubmissionById(int id, User user)
 {
  return new SimpleLogNode(Level.ERROR, "Tranklucating submissions by id is not implemented", null);
 }

 @Override
 public LogNode tranklucateSubmissionByAccessionPattern(String accPfx, User usr)
 {
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Tranklucating submissions by pattern '"+accPfx+"'", null);
  
  if( shutdown )
  {
   gln.log(Level.ERROR, "Service is shut down");
   return gln;
  }
  
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();
  
  List<String> res = null;
  
  try
  {
   TypedQuery<String> pq = em.createNamedQuery("Submission.getAccByPat",String.class);
   
   pq.setParameter("pattern", accPfx);
   
   res = pq.getResultList();
   
   
   if( res.size() == 0 )
   {
    gln.log(Level.INFO, "No matches");
    return gln;
   }
    
   gln.log(Level.INFO, "Found "+res.size()+" matches");

   Query q = em.createNamedQuery("Submission.getAllByAcc");

   for( String acc : res )
   {
    tranklucateSubmissionByAccession(acc, usr, gln.branch("Tranklucating submission '"+acc+"'"), em, q);
   }
   
  }
  catch( Exception e )
  {
   e.printStackTrace();
   
   log.error("Exception: "+e.getClass()+" Message: "+e.getMessage());
   
   gln.log(Level.ERROR, "Internal server error");
  }

  

  return gln;
 }
 
 @Override
 public LogNode tranklucateSubmissionByAccession(String acc, User usr)
 {
  SimpleLogNode gln = new SimpleLogNode(Level.SUCCESS, "Tranklucating submission '"+acc+"'", null);

  if( shutdown )
  {
   gln.log(Level.ERROR, "Service is shut down");
   return gln;
  }
  
  EntityManager em = BackendConfig.getServiceManager().getSessionManager().getSession().getEntityManager();

  try
  {
   Query q = em.createNamedQuery("Submission.getAllByAcc");

   tranklucateSubmissionByAccession(acc, usr, gln, em, q);
  }
  catch( Exception e )
  {
   e.printStackTrace();
   
   log.error("Exception: "+e.getClass()+" Message: "+e.getMessage());
   
   gln.log(Level.ERROR, "Internal server error");
  }
  
  return gln;
 }
 
 
 private LogNode tranklucateSubmissionByAccession(String acc, User usr, LogNode gln,EntityManager em, Query q)
 {
  FileManager fileMngr = BackendConfig.getServiceManager().getFileManager();
  
  boolean trnOk = false;
  
  Submission mainSbm = null;

  try
  {
   em.getTransaction().begin();

   q.setParameter("accNo", acc);


   @SuppressWarnings("unchecked")
   List<Submission> res = q.getResultList();

   if(res.size() == 0)
   {
    gln.log(Level.ERROR, "Submission not found");
    return gln;
   }
   
   for( Submission s : res )
   {
    if( s.getVersion() > 0 )
    {
     mainSbm = s;
     break;
    }
   }

   if(mainSbm !=null && !BackendConfig.getServiceManager().getSecurityManager().mayUserDeleteSubmission(mainSbm, usr))
   {
    gln.log(Level.ERROR, "User has no permission to delete this submission");
    return gln;
   }

   for( Submission s : res )
    em.remove(s);
   
   trnOk = true;
   
   for( Submission s : res )
   {
    Path dir = s == mainSbm ? BackendConfig.getSubmissionPath(s) : BackendConfig.getSubmissionHistoryPath(s);

    if(Files.exists(dir))
    {
     try
     {
      fileMngr.deleteDirectory(dir);
     }
     catch(Exception e)
     {
      log.error("Can't delete submission directory " + dir + " Error: " + e.getMessage());
      e.printStackTrace();
      gln.log(Level.WARN, "Submission directory was not deleted");
     }
    }
   }
   
  }
  finally
  {
   
   try
   {
    if( trnOk )
    {
     em.getTransaction().commit();
     gln.log(Level.INFO, "Transaction successful");
    }
    else
     em.getTransaction().rollback();
   }
   catch(Throwable t)
   {
    trnOk = false;
    
    String err = "Database transaction failed: " + t.getMessage();

    gln.log(Level.ERROR, err);

    if(em.getTransaction().isActive())
     em.getTransaction().rollback();
   }

  }
  
  if( trnOk && BackendConfig.getPublicFTPPath() != null && mainSbm != null )
  {
   Path ftpPath = BackendConfig.getSubmissionPublicFTPPath(mainSbm);
   
   if( Files.exists(ftpPath) )
   {
    try
    {
     fileMngr.deleteDirectory(ftpPath);
    }
    catch(Exception e)
    {
     log.error("Can't delete public ftp directory "+ftpPath+" Error: "+e.getMessage());
     e.printStackTrace();
     gln.log(Level.WARN, "Public FTP directory was not deleted");
    }
   }
   
   if( trnOk && queueProc != null && mainSbm != null )
   {
    StringBuilder out = new StringBuilder();
    
    
    out.append('<').append(SUBMISSION.getElementName()).append(' ').append(ACCNO.getAttrName()).append("=\"");
    
    try
    {
     xmlEscaped(mainSbm.getAccNo(), out);
    }
    catch(IOException e)
    {
    }

    out.append("\" ").append(ID.getAttrName()).append("=\"").append( String.valueOf(mainSbm.getId()) );
    out.append("\" delete=\"true\"/>\n");

    String msg = out.toString();
    
    while( ! shutdown )
    {
     try
     {
      queueProc.put(msg);
      break;       
     }
     catch(InterruptedException e)
     {
     }
    }
   }
    
  }
  
  return gln;
 }

}
