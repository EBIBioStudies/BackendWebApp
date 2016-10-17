package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.SubmissionReport;
import uk.ac.ebi.biostd.util.DataFormat;
import uk.ac.ebi.biostd.webapp.shared.tags.TagRef;

public interface SubmissionManager
{
 enum Operation
 {
  CREATE,
  CREATEUPDATE,
  UPDATE,
  OVERRIDE,
  CREATEOVERRIDE,
  DELETE,
  REMOVE,
  TRANKLUCATE,
  SETMETA
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
 
 SubmissionReport createSubmission(byte[] data, DataFormat fmt, String charset, Operation op, User usr, boolean validateOnly, boolean ignoreAbsFiles);

 LogNode updateSubmissionMeta(String sbmAcc, Collection<TagRef> tags, Set<String> access, long rTime, User user);
 
 LogNode deleteSubmissionByAccession(String acc, boolean toHistory, User usr);
 
 LogNode tranklucateSubmissionById(int id, User user);
 LogNode tranklucateSubmissionByAccession(String sbmAcc, User user);
 LogNode tranklucateSubmissionByAccessionPattern(String accPfx, User usr);

 void shutdown();
}
