package uk.ac.ebi.biostd.webapp.server.mng.impl;

import javax.persistence.EntityManager;

import uk.ac.ebi.biostd.authz.AccessTag;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.db.TagResolver;

public class TagResolverImpl implements TagResolver
{
 private EntityManager em;
 
 public TagResolverImpl( EntityManager emngr )
 {
  em = emngr;
 }

 @Override
 public Tag getTagByName(String clsfName, String tagName)
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public AccessTag getAccessTagByName(String tagName)
 {
  // TODO Auto-generated method stub
  return null;
 }

}
