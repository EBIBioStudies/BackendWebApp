package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.Map;

import uk.ac.ebi.biostd.in.ElementPointer;

public class LockedIdSet
{
 private Map<String, ElementPointer> submissionMap;
 private Map<String, ElementPointer> sectionMap;
 private int waitCount=0;

 public Map<String, ElementPointer> getSubmissionMap()
 {
  return submissionMap;
 }

 public void setSubmissionMap(Map<String, ElementPointer> submissionMap)
 {
  this.submissionMap = submissionMap;
 }

 public Map<String, ElementPointer> getSectionMap()
 {
  return sectionMap;
 }

 public void setSectionMap(Map<String, ElementPointer> sectionMap)
 {
  this.sectionMap = sectionMap;
 }

 public boolean empty()
 {
  return ( submissionMap == null || submissionMap.size()==0 ) && ( sectionMap == null || sectionMap.size()==0 );
 }

 public int getWaitCount()
 {
  return waitCount;
 }
 
 public void incWaitCount()
 {
  waitCount++;
 }
}
