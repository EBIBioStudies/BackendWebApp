package uk.ac.ebi.biostd.webapp.server.export;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public interface FSProvider
{

 boolean exists(Path tmpDir) throws IOException;

 void createDirectories(Path tmpDir) throws IOException;

 boolean isWritable(Path tmpDir) throws IOException;

 boolean isDirectory(Path outDir) throws IOException;

 PrintStream createPrintStream(Path resolve, String string) throws UnsupportedEncodingException, IOException;

 void move(Path tmpDir, Path tmpOutDir) throws IOException;

 void copyDirectory(Path tmpDir, Path tmpOutDir) throws IOException;

 void deleteDirectoryContents(Path tmpDir) throws IOException;

 void deleteDirectory(Path tmpOutDir2) throws IOException;

 void close();

}
