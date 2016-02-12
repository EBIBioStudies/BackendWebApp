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