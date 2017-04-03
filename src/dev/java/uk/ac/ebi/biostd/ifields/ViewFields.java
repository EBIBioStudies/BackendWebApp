/**

Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Mikhail Gostev <gostev@gmail.com>

**/

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
  IndexReader r = DirectoryReader.open(FSDirectory.open(Paths.get(args[0])));
  
  
  for( LeafReaderContext rc : r.leaves() )
  {
   for( FieldInfo fi : rc.reader().getFieldInfos() )
    System.out.println(fi.name+" "+fi.getDocValuesType());

   System.out.println("---");
  }
  
  int num = r.numDocs();
  
  System.out.println("Total docs: "+num);
  
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
