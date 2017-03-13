package uk.ac.ebi.biostd.webapp.server.util;

import java.io.IOException;
import java.nio.charset.Charset;

public interface Resource
{

 boolean isValid();

 String readToString(Charset utf8) throws IOException;

}
