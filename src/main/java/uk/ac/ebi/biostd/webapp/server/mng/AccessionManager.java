package uk.ac.ebi.biostd.webapp.server.mng;

import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.webapp.server.mng.exception.ServiceException;

public interface AccessionManager
{

 String getNextAccNo(String prefix, String suffix, User usr) throws SecurityException, ServiceException;

 long incrementIdGen(String prefix, String suffix, int num, User usr) throws SecurityException, ServiceException;

}
