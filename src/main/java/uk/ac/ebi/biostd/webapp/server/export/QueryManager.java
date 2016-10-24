package uk.ac.ebi.biostd.webapp.server.export;

import java.io.PrintWriter;
import java.util.List;

import uk.ac.ebi.biostd.model.Submission;

public interface QueryManager
{
 List<Submission> getSubmissions(PrintWriter out);
 
 int getChunkSize();
 
 int getRecovers();
 
 void release();

 void close();
}
