package uk.ac.ebi.biostd.webapp.server.export;

public class MTTaskConfig
{
 private long    since;
 private int     maxItemsPerThread=-1;

 
 public int getItemsPerThreadLimit()
 {
  return maxItemsPerThread;
 }

 public void setItemsPerThreadLimit(int maxItemsPerTread)
 {
  this.maxItemsPerThread = maxItemsPerTread;
 }

 public long getSince()
 {
  return since;
 }

 public void setSince(long since)
 {
  this.since = since;
 }

}
