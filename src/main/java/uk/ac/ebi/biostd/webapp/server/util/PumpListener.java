package uk.ac.ebi.biostd.webapp.server.util;

public interface PumpListener
{
 void dataPumped(int k);

 void endOfStream();
}
