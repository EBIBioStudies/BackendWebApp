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

package uk.ac.ebi.biostd.webapp.server.export;



public class ControlMessage
{
 public enum Type
 {
  OUTPUT_FINISH,
  OUTPUT_ERROR,
  PROCESS_FINISH,
  PROCESS_TTL,
  PROCESS_ERROR,
  TERMINATE
 }

 private final String threadName;
 private final Type      type;
 private final Object    subject;
 private Throwable exception;

 public ControlMessage(String tName, Type type, Object subject)
 {
  super();
  this.type = type;
  this.subject = subject;
  threadName = tName;
 }

 public ControlMessage(String tName, Type type, Object subject, Throwable exception)
 {
  super();
  this.type = type;
  this.subject = subject;
  this.exception = exception;
  threadName = tName;
 }

 public ControlMessage(Type type, Object subject)
 {
  this(Thread.currentThread().getName(),type,subject);
 }

 public ControlMessage(Type type, Object subject, Throwable exception)
 {
  this(Thread.currentThread().getName(),type,subject,exception);
 }
 
 public Type getType()
 {
  return type;
 }

 public Object getSubject()
 {
  return subject;
 }

 public Throwable getException()
 {
  return exception;
 }

 public String getThreadName()
 {
  return threadName;
 }

}
