package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.treelog.LogNode;

public interface SubmissionManager
{
 Collection< Submission > getSubmissionsByOwner( User u );

 LogNode createJSONSubmission(String txt, User usr);
 LogNode createXMLSubmission(String txt, User usr);
 LogNode createPageTabSubmission(String txt, User usr);
 
 LogNode updateJSONSubmission(String txt, User usr);
 LogNode updateXMLSubmission(String txt, User usr);
 LogNode updatePageTabSubmission(String txt, User usr);

}
