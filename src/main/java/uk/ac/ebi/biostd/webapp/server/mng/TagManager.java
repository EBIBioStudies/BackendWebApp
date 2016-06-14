package uk.ac.ebi.biostd.webapp.server.mng;

import java.util.Collection;

import uk.ac.ebi.biostd.authz.Tag;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public interface TagManager
{

 void createTag(String tagName, String desc, String classifierName, String parentTag, User user) throws SecurityException, ServiceException;

 void createClassifier(String classifierName, String description, User user) throws SecurityException, ServiceException;

 void deleteClassifier(String classifierName, User user) throws SecurityException, ServiceException;

 void deleteTag(String tagName, String classifierName, boolean cascade, User user) throws SecurityException, ServiceException;

 Collection<Tag> listTags() throws  ServiceException;

}
