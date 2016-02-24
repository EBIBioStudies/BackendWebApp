package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;

import org.apache.lucene.queryparser.classic.ParseException;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;

public interface SubmissionManager
{
 enum Operation
 {
  CREATE,
  UPDATE,
  REPLACE,
  DELETE,
  TRANKLUCATE
 }
 
 Collection< Submission > getSubmissionsByOwner( User u, int offset, int limit );
 Submission getSubmissionsByAccession( String acc );
 
 Collection< Submission > searchSubmissions( User user, SubmissionSearchRequest ssr ) throws ParseException;
 
 /*
 LogNode createJSONSubmission(String txt, User usr);
 LogNode createXMLSubmission(String txt, User usr);
 LogNode createPageTabSubmission(String txt, User usr);
 
 LogNode updateJSONSubmission(String txt, User usr);
 LogNode updateXMLSubmission(String txt, User usr);
 LogNode updatePageTabSubmission(String txt, User usr);
*/
 
 SubmissionReport createSubmission(byte[] data, DataFormat fmt, String charset, Operation op, User usr, boolean validateOnly);

 
 LogNode deleteSubmissionByAccession(String acc, User usr);
 
 LogNode tranklucateSubmissionById(int id, User user);
 LogNode tranklucateSubmissionByAccession(String sbmAcc, User user);
 LogNode tranklucateSubmissionByAccessionPattern(String accPfx, User usr);

 void shutdown();
}
