package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.mng.SubmissionManager;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.pagetab.PageTabSyntaxParser2;
import uk.ac.ebi.biostd.pagetab.ParserConfig;
import uk.ac.ebi.biostd.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;

public class JPASubmissionManager implements SubmissionManager
{

 private EntityManagerFactory emf;
 private EntityManager em;
 
 private PageTabSyntaxParser2 ptParser;
 
 public JPASubmissionManager(EntityManagerFactory emf)
 {
  this.emf=emf;

  ParserConfig pc = new ParserConfig();
  
  pc.setMultipleSubmissions(true);
  
  ptParser = new PageTabSyntaxParser2(null, pc);
 }

 
 @Override
 public Collection<Submission> getSubmissionsByOwner(User u)
 {
  if(em == null)
   em = emf.createEntityManager();

  Query q = em.createQuery("select s from Submission s JOIN Submission.owner u where u.id=?1");

  q.setParameter(1, u.getId());

  @SuppressWarnings("unchecked")
  List<Submission> res = q.getResultList();

  return res;
 }

 @Override
 public LogNode createSubmission( String txt )
 {
  ErrorCounter ec = new ErrorCounterImpl();
  
  LogNode gln = new SimpleLogNode(Level.SUCCESS, "", ec);
  
  List<SubmissionInfo> subms =  ptParser.parse(txt, gln);
  
  if( gln.getLevel() != Level.SUCCESS )
   return gln;
  
  for( SubmissionInfo si : subms )
   si.getSubmission();
  
  return gln;

 }
 
}
