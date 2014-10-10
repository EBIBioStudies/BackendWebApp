package uk.ac.ebi.biostd.webapp.shared.dto.log;

public interface ErrorCounter
{
 int getErrorCounter();
 void incErrorCounter();
 void addErrorCounter(int countErrors);
 void resetErrorCounter();
}
