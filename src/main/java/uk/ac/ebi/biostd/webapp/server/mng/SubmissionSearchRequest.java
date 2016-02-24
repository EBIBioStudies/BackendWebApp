package uk.ac.ebi.biostd.webapp.server.mng;

public class SubmissionSearchRequest
{
 public static enum SortFields
 {
  CTime,
  MTime,
  RTime
 }
 
 private String keywords;

 private long fromCTime;
 private long toCTime;

 private long fromMTime;
 private long toMTime;

 private long fromRTime;
 private long toRTime;
 
 private String owner;
 private String accNo;
 
 private SortFields sortBy;
 
 private int skip;
 private int limit;

 public String getKeywords()
 {
  return keywords;
 }

 public void setKeywords(String keywords)
 {
  this.keywords = keywords;
 }

 public long getFromCTime()
 {
  return fromCTime;
 }

 public void setFromCTime(long fromCTime)
 {
  this.fromCTime = fromCTime;
 }

 public long getToCTime()
 {
  return toCTime;
 }

 public void setToCTime(long toCTime)
 {
  this.toCTime = toCTime;
 }

 public long getFromMTime()
 {
  return fromMTime;
 }

 public void setFromMTime(long fromMTime)
 {
  this.fromMTime = fromMTime;
 }

 public long getToMTime()
 {
  return toMTime;
 }

 public void setToMTime(long toMTime)
 {
  this.toMTime = toMTime;
 }

 public long getFromRTime()
 {
  return fromRTime;
 }

 public void setFromRTime(long fromRTime)
 {
  this.fromRTime = fromRTime;
 }

 public long getToRTime()
 {
  return toRTime;
 }

 public void setToRTime(long toRTime)
 {
  this.toRTime = toRTime;
 }

 public SortFields getSortBy()
 {
  return sortBy;
 }

 public void setSortBy(SortFields sortBy)
 {
  this.sortBy = sortBy;
 }

 public int getSkip()
 {
  return skip;
 }

 public void setSkip(int skip)
 {
  this.skip = skip;
 }

 public int getLimit()
 {
  return limit;
 }

 public void setLimit(int limit)
 {
  this.limit = limit;
 }

 public String getOwner()
 {
  return owner;
 }

 public void setOwner(String owner)
 {
  this.owner = owner;
 }

 public String getAccNo()
 {
  return accNo;
 }

 public void setAccNo(String accNo)
 {
  this.accNo = accNo;
 }
 
 
}
