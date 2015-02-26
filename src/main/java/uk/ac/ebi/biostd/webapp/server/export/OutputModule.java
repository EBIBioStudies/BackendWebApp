package uk.ac.ebi.biostd.webapp.server.export;

import java.io.IOException;

import uk.ac.ebi.biostd.out.Formatter;

public interface OutputModule
{

 Formatter getFormatter();

 Appendable getOut();
 
 void start() throws IOException;
 void finish(ExporterStat stat) throws IOException;

 void cancel() throws IOException;
 
 String getName();
 
}
