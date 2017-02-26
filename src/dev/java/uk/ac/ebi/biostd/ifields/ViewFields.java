package uk.ac.ebi.biostd.ifields;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class ViewFields
{

 public static void main(String[] args) throws IOException
 {

//  IndexReader r = DirectoryReader.open(FSDirectory.open(Paths.get("e:\\games\\index\\uk.ac.ebi.biostd.model.Submission")));
  IndexReader r = DirectoryReader.open(FSDirectory.open(Paths.get("c:\\Dev\\tmp\\biostd_app\\index\\uk.ac.ebi.biostd.model.Submission")));
  
  
  for( LeafReaderContext rc : r.leaves() )
  {
   for( FieldInfo fi : rc.reader().getFieldInfos() )
    System.out.println(fi.name+" "+fi.getDocValuesType());

   System.out.println("---");
  }
  
  int num = r.numDocs();
  for ( int i = 0; i < num && i < 10; i++)
  {
          Document d = r.document(i);
          
          for( IndexableField f : d.getFields() )
          {
           System.out.println(f.name()+"="+f.stringValue());
          }
          
  }
  
  IndexSearcher sr = new IndexSearcher(r);
  
  TopDocs docs = sr.search(NumericRangeQuery.newLongRange("owner.numid", 5L, 5L, true, true), 10);
  
  System.out.println("Found: "+docs.totalHits);
  
  r.close();
 }

}
