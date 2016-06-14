package uk.ac.ebi.biostd.webapp.server.mng.impl;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biostd.authz.Classifier;
import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.mng.SecurityException;
import uk.ac.ebi.biostd.webapp.server.mng.TagManager;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public class JPATagManager implements TagManager
{

 @Override
 public void createTag(String tagName, String desc, String classifierName, String parentTag, User user) throws SecurityException, ServiceException
 {
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags( user ) )
   throw new SecurityException("User has no perimission to manage tags");
  
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  boolean trnOk=false;
  
  try
  {
   trn.begin();
   
   Query clq = em.createNamedQuery(Classifier.GetByNameQuery);
   
   clq.setParameter(Classifier.NameQueryParameter, classifierName);
   
   List<Classifier> clres = clq.getResultList();
   
   if( clres.size() != 1 )
    throw new ServiceException("Invalid classifier: '"+classifierName+"'");
   
   Classifier clsf = clres.get(0);
   
   Query tgq = em.createNamedQuery(Tag.GetByNameQuery);
   
   tgq.setParameter(Tag.TagNameQueryParameter, tagName);
   tgq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);
   
   List<Tag> tgres  = tgq.getResultList();
   
   if( tgres.size() > 0 )
    throw new ServiceException("Tag already exists: '"+classifierName+"."+tagName+"'");
    
   Tag pTag = null;
   
   if( parentTag != null )
   {
    tgq.setParameter(Tag.TagNameQueryParameter, parentTag);
    tgq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);

    tgres = tgq.getResultList();
    
    if( tgres.size() != 1 )
     throw new ServiceException("Invalid parent tag: '"+classifierName+"."+parentTag+"'");
    
    pTag = tgres.get(0);
   }
   
   Tag nt = new Tag();
   
   nt.setName(tagName);
   nt.setDescription(desc);
   nt.setParentTag(pTag);
   nt.setClassifier(clsf);
   
   em.persist(nt);
   trnOk = true;
  }
  finally
  {
   if( trn.isActive() )
   {
    try
    {
     if( trnOk )
      trn.commit();
     else
      trn.rollback();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   em.close();
  }
  
 }

 @Override
 public void createClassifier(String classifierName, String description, User user) throws SecurityException, ServiceException
 {
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags( user ) )
   throw new SecurityException("User has no perimission to manage classifiers");
  
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  boolean trnOk=false;
  
  try
  {
   trn.begin();
   
   Query clq = em.createNamedQuery(Classifier.GetByNameQuery);
   
   clq.setParameter(Classifier.NameQueryParameter, classifierName);
   
   List<Classifier> clres = clq.getResultList();
   
   if( clres.size() > 0 )
    throw new ServiceException("Classifier already exists: '"+classifierName+"'");
   
   
   Classifier ncl = new Classifier();
   
   ncl.setName(classifierName);
   ncl.setDescription(description);

   
   em.persist(ncl);
   trnOk = true;
  }
  finally
  {
   if( trn.isActive() )
   {
    try
    {
     if( trnOk )
      trn.commit();
     else
      trn.rollback();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   em.close();
  }
 }

 @Override
 public void deleteClassifier(String classifierName, User user) throws SecurityException, ServiceException
 {
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags( user ) )
   throw new SecurityException("User has no perimission to manage classifiers");
  
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  boolean trnOk=false;
  
  try
  {
   trn.begin();
   
   Query clq = em.createNamedQuery(Classifier.GetByNameQuery);
   
   clq.setParameter(Classifier.NameQueryParameter, classifierName);
   
   List<Classifier> clres = clq.getResultList();
   
   if( clres.size() == 0 )
    throw new ServiceException("Classifier doesn't exist: '"+classifierName+"'");
   
   
   em.remove(clres.get(0));
   trnOk = true;
  }
  finally
  {
   if( trn.isActive() )
   {
    try
    {
     if( trnOk )
      trn.commit();
     else
      trn.rollback();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   em.close();
  }
 }

 @Override
 public void deleteTag(String tagName, String classifierName, boolean cascade, User user) throws SecurityException, ServiceException
 {
  if( ! BackendConfig.getServiceManager().getSecurityManager().mayUserManageTags( user ) )
   throw new SecurityException("User has no perimission to manage tags");
  
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  boolean trnOk=false;
  
  try
  {
   trn.begin();
   
   Query clq = em.createNamedQuery(Tag.GetByNameQuery);
   
   clq.setParameter(Tag.ClassifierNameQueryParameter, classifierName);
   clq.setParameter(Tag.TagNameQueryParameter, tagName);
   
   List<Tag> clres = clq.getResultList();
   
   if( clres.size() == 0 )
    throw new ServiceException("Tag doesn't exist: '"+classifierName+"."+tagName+"'");
   
   Tag t = clres.get(0);
   
   if( ! cascade )
   {
    if( t.getSubTags() != null )
    {
     for( Tag st : t.getSubTags() )
      st.setParentTag(t.getParentTag());
    }
    
    t.getSubTags().clear();
   }
   
   em.remove(t);

   
   trnOk = true;
  }
  finally
  {
   if( trn.isActive() )
   {
    try
    {
     if( trnOk )
      trn.commit();
     else
      trn.rollback();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   em.close();
  }
 }

 @Override
 public Collection<Tag> listTags() throws ServiceException
 {
  EntityManager em = BackendConfig.getEntityManagerFactory().createEntityManager();
  
  EntityTransaction trn = em.getTransaction();
  
  boolean trnOk=true;
  
  try
  {
   trn.begin();
   
   Query clq = em.createNamedQuery(Tag.GetAllQuery);
   
   List<Tag> res = clq.getResultList();
   
   for( Tag t : res )
   {
    t.getClassifier().getName().length();
    if( t.getParentTag() != null )
     t.getParentTag().getName().length();
   }
   
   return res;
  }
  catch( Throwable t )
  {
   trnOk = false;
   
   throw new ServiceException( t.getMessage(), t );
  }
  finally
  {
   if( trn.isActive() )
   {
    try
    {
     if( trnOk )
      trn.commit();
     else
      trn.rollback();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   em.close();
  }

 }

}
