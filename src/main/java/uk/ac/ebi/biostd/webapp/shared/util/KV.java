package uk.ac.ebi.biostd.webapp.shared.util;

public class KV
{
 private String prefix;
 private String key;
 private String value;
 
 public KV()
 {}

 public KV(String key, String value)
 {
  super();
  this.key = key;
  this.value = value;
 }
 
 public KV(String pref, String key, String value)
 {
  super();
  this.key = key;
  this.value = value;
  prefix = pref;
 }
 
 public String getKey()
 {
  return key;
 }

 public void setKey(String key)
 {
  this.key = key;
 }

 public String getValue()
 {
  return value;
 }

 public void setValue(String value)
 {
  this.value = value;
 }

 public String getPrefix()
 {
  return prefix;
 }

 public void setPrefix(String prefix)
 {
  this.prefix = prefix;
 }

 
 
}
