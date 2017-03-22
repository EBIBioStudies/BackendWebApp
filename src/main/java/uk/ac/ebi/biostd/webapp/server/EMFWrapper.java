package uk.ac.ebi.biostd.webapp.server;

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

public class EMFWrapper implements EntityManagerFactory
{
 private EntityManagerFactory emf;

 public EMFWrapper( EntityManagerFactory emf )
 {
  this.emf=emf;
 }
 
 public EntityManager createEntityManager()
 {
  return emf.createEntityManager();
 }

 public EntityManager createEntityManager(Map map)
 {
  return emf.createEntityManager(map);
 }

 public EntityManager createEntityManager(SynchronizationType synchronizationType)
 {
  return emf.createEntityManager(synchronizationType);
 }

 public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map)
 {
  return emf.createEntityManager(synchronizationType, map);
 }

 public CriteriaBuilder getCriteriaBuilder()
 {
  return emf.getCriteriaBuilder();
 }

 public Metamodel getMetamodel()
 {
  return emf.getMetamodel();
 }

 public boolean isOpen()
 {
  return emf.isOpen();
 }

 public void close()
 {
  try{
   throw new Exception();
  }
  catch (Exception e) {
   e.printStackTrace();
  }
  
  emf.close();
 }

 public Map<String, Object> getProperties()
 {
  return emf.getProperties();
 }

 public Cache getCache()
 {
  return emf.getCache();
 }

 public PersistenceUnitUtil getPersistenceUnitUtil()
 {
  return emf.getPersistenceUnitUtil();
 }

 public void addNamedQuery(String name, Query query)
 {
  emf.addNamedQuery(name, query);
 }

 public <T> T unwrap(Class<T> cls)
 {
  return emf.unwrap(cls);
 }

 public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph)
 {
  emf.addNamedEntityGraph(graphName, entityGraph);
 }

}
