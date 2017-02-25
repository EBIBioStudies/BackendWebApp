package uk.ac.ebi.biostd.webapp.server.search;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.model.Submission;

public class SearchMapper
{
 public static final String idField = "id";

 public static final String loginField = "login";
 public static final String emailField = "email";

 public static final String accNoField = "accNo";
 public static final String titleField = "title";
 public static final String cTimeField = "CTime";
 public static final String mTimeField = "MTime";
 public static final String rTimeField = "RTime";
 public static final String ownerField = "owner";
 public static final String versionField = "version";


 public static SearchMapping makeMapping()
 {
  SearchMapping mapping = new SearchMapping();
  
  mapping.entity(User.class)
   .property(idField, ElementType.METHOD)
     .documentId()
   .property(loginField, ElementType.METHOD)
    .field()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
   .property(emailField, ElementType.METHOD)
    .field()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
  ;

  mapping.entity(Submission.class)
  .indexed()
   .property(idField, ElementType.METHOD)
    .documentId()
   .property(accNoField, ElementType.METHOD)
    .field()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
   .property(titleField, ElementType.METHOD)
    .field()
     .analyze(Analyze.YES)
     .index(Index.YES)
     .store(Store.NO)
   .property(cTimeField, ElementType.METHOD)
    .field()
     .numericField()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
     .sortableField()
   .property(mTimeField, ElementType.METHOD)
    .field()
     .numericField()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
     .sortableField()
   .property(rTimeField, ElementType.METHOD)
    .field()
     .numericField()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
     .sortableField()
   .property(versionField, ElementType.METHOD)
    .field()
     .numericField()
     .analyze(Analyze.NO)
     .index(Index.YES)
     .store(Store.NO)
   .property(ownerField, ElementType.METHOD)
    .indexEmbedded()
    .includeEmbeddedObjectId(true)
    .includePaths(emailField,idField)

 ;
  
  return mapping;
 }
 
}
