package uk.ac.ebi.biostd.webapp.server.export;

import java.text.SimpleDateFormat;
import java.util.Date;

import uk.ac.ebi.biostd.util.StringUtils;

public class ExporterStat
{
 private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 
 private int submissionCount=0;
 private final Date now;
 private int threads;
 private int errorRecoverCount=0;
 
 public ExporterStat( Date now )
 {
  this.now = now;
 }
 
 public void reset()
 {
 }
 
 public Date getNowDate()
 {
  return now;
 }
 
 public synchronized void incRecoverAttempt()
 {
  errorRecoverCount++;
 }
 

 public synchronized void addRecoverAttempt(int recovers)
 {
  errorRecoverCount+=recovers;
 }
 
 public synchronized int getRecoverAttempt()
 {
  return errorRecoverCount;
 }

 
 public int getSubmissionCount()
 {
  return submissionCount;
 }


 public synchronized void incSubmissionCount()
 {
  submissionCount++;
 }
 


 public String createReport(Date startTime, Date endTime, int threads)
 {
  long startTs = startTime.getTime();
  long endTs = endTime.getTime();
  
  long rate = getSubmissionCount()!=0? (endTs-startTs)/getSubmissionCount():0;
  
  StringBuffer summaryBuf = new StringBuffer();

  summaryBuf.append("\n<!-- Exported: ").append(getSubmissionCount()).append(" submissions in ").append(threads).append(" threads. Rate: ").append(rate).append("ms per msi -->");

  summaryBuf.append("\n<!-- Start time: ").append(simpleDateFormat.format(startTime)).append(" -->");
  summaryBuf.append("\n<!-- End time: ").append(simpleDateFormat.format(endTime)).append(". Time spent: "+StringUtils.millisToString(endTs-startTs)).append(" -->");
  summaryBuf.append("\n<!-- I/O error recovered: ").append(getRecoverAttempt()).append(" -->");
  summaryBuf.append("\n<!-- Thank you. Good bye. -->\n");
  
  return summaryBuf.toString();

 }

 public int getThreads()
 {
  return threads;
 }

 public void setThreads(int threads)
 {
  this.threads = threads;
 }



}
