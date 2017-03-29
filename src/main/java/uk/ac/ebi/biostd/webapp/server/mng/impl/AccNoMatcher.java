package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.model.Submission;

public class AccNoMatcher
{
 public enum Match
 {
  YES,
  NO,
  NOINFO
 }

 public static Match match(SubmissionInfo si, Submission host)
 {
  String pat = Submission.getNodeAccNoPattern(host);
  
  if( pat == null )
   return Match.NOINFO;
  
  Matcher mtch = Pattern.compile(pat).matcher("");
  
  if( si.getAccNoPrefix() == null && si.getAccNoSuffix() == null )
   mtch.reset( si.getAccNoOriginal() );
  else
   mtch.reset( (si.getAccNoPrefix() == null?"":si.getAccNoPrefix())+"000"+(si.getAccNoSuffix() == null?"":si.getAccNoSuffix()) );
  
  return mtch.matches()?Match.YES:Match.NO;
 }
}
