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

package uk.ac.ebi.biostd.webapp.server.export.epmc;
public enum EPMCLinkElements
{
 ROOT("links"),
 LINK( "link" ),
 RESOURCE("resource"),
 TITLE("title"),
 RECORD("record"),
 SOURCE("source"),
 ID("id"),
 URL("url"),
 
 PROVIDER_ID_ATTR("providerId")
 ;
 
 private EPMCLinkElements( String el )
 {
  elementName = el;
 }
 
 private String elementName;

 public String getElementName()
 {
  return elementName;
 }
}