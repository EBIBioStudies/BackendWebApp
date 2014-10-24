package uk.ac.ebi.biostd.webapp.server.mng;

public enum BuiltInUsers
{
 ANONYMOUS("$anonymous", "Built-in anonymous user"),
 SUPERVISOR("$supervisor", "Built-in supervisor user");
 
 private String name;
 private String description;
 
 BuiltInUsers( String name, String desc )
 {
  this.name=name;
  description=desc;
 }

 public String getName()
 {
  return name;
 }

 public String getDescription()
 {
  return description;
 }

}
