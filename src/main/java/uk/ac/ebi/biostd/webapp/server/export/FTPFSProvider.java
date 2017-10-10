/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Mikhail Gostev <gostev@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FTPFSProvider implements FSProvider {

    private static Logger log = null;

    private String server;
    private String user;
    private String passwd;

    private FTPClient ftp;

    private Map<String, FTPFile[]> cache = new HashMap<>();

    public FTPFSProvider(String connStr) {
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }

        if (connStr.startsWith("ftp:")) {
            connStr = connStr.substring(4);
        }

        int i = 0;

        for (i = 0; i < connStr.length() && connStr.charAt(i) == '/'; i++) {
        }

        if (i > 0) {
            connStr = connStr.substring(i);
        }

        int pos = connStr.indexOf('@');

        if (pos >= 0) {
            user = connStr.substring(0, pos);
            server = connStr.substring(pos + 1);

            pos = user.indexOf(':');

            if (pos >= 0) {
                passwd = user.substring(pos + 1);
                user = user.substring(0, pos);
            }
        } else {
            server = connStr;

            user = "anonymous";
            passwd = "anonymous@ebi.ac.uk";
        }

        ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();

        ftp.setConnectTimeout(20000);
        ftp.setDefaultTimeout(20000);
        ftp.setDataTimeout(20000);

        ftp.configure(config);


    }

    private void connect() throws IOException {
        boolean connected = ftp.isConnected();

        if (connected) {
            try {
                ftp.sendNoOp();
            } catch (Exception e) {
                connected = false;
                ftp.disconnect();
            }
        }

        if (!connected) {
            ftp.connect(server);
            ftp.login(user, passwd);
            ftp.enterLocalPassiveMode();
//    ftp.enterRemotePassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
        }

    }

    @Override
    public boolean exists(Path file) throws IOException {
        connect();

        String pathstr = file.getParent().toString();

        if (file.getFileSystem().getSeparator().equals("/")) {
            pathstr = FilenameUtils.separatorsToUnix(pathstr);
        }

        FTPFile[] fl = cache.get(pathstr);

        if (fl == null) {
            fl = ftp.listFiles(pathstr);
            cache.put(pathstr, fl);
        }

        String name = file.getFileName().toString();

        for (FTPFile fff : fl) {
            if (name.equals(fff.getName())) {
                return true;
            }
        }

        return false;
    }


    @Override
    public void createDirectories(Path path) throws IOException {
        connect();

//  FTPFile[] fls = ftp.listFiles("/zs11dw62");

        StringBuilder sb = new StringBuilder();

        sb.append('/');

        String cPath = sb.toString();

        for (int i = 0; i < path.getNameCount(); i++) {
            String cName = path.getName(i).toString();

            FTPFile[] fl = cache.get(cPath);

            if (fl == null) {
                fl = ftp.listFiles(cPath);
                cache.put(cPath, fl);
            }

            boolean exists = false;

            for (FTPFile fff : fl) {
                if (cName.equals(fff.getName())) {
                    if (!fff.isDirectory()) {
                        throw new IOException(cPath + "/" + cName + " is not directory");
                    }

                    exists = true;
                    break;
                }

            }

            if (i > 0) {
                sb.append('/');
            }

            sb.append(cName);

            String nPath = sb.toString();

            if (!exists) {
                cache.remove(cPath);
                ftp.makeDirectory(nPath);
            }

            cPath = nPath;

        }

    }

    private String pathToStr(Path file) {
        String pathstr = file.toString();

        if (!file.getFileSystem().getSeparator().equals("/")) {
            pathstr = FilenameUtils.separatorsToUnix(pathstr);
        }

        return pathstr;
    }

    @Override
    public boolean isWritable(Path file) throws IOException {
        return true;
    }

    @Override
    public boolean isDirectory(Path file) throws IOException {
        connect();

        String pathstr = file.getParent().toString();

        if (!file.getFileSystem().getSeparator().equals("/")) {
            pathstr = FilenameUtils.separatorsToUnix(pathstr);
        }

        FTPFile[] fl = cache.get(pathstr);

        if (fl == null) {
            fl = ftp.listFiles(pathstr);
            cache.put(pathstr, fl);
        }

        String name = file.getFileName().toString();

        for (FTPFile fff : fl) {
            if (name.equals(fff.getName())) {
                return fff.isDirectory();
            }
        }

        return false;
    }

    @Override
    public PrintStream createPrintStream(Path file, String enc) throws IOException {
        connect();

        String pathstr = file.toString();

        if (!file.getFileSystem().getSeparator().equals("/")) {
            pathstr = FilenameUtils.separatorsToUnix(pathstr);
        }

        OutputStream ofs = ftp.storeFileStream(pathstr);

        if (ofs == null || !FTPReply.isPositivePreliminary(ftp.getReplyCode())) {
            throw new IOException("Can't open file for writing: " + pathstr);
        }

        return new FTPPrintStream(ofs, enc, ftp);
    }

    @Override
    public void move(Path from, Path to) throws IOException {
        String fromstr = from.toString();
        String tostr = to.toString();

        if (!from.getFileSystem().getSeparator().equals("/")) {
            fromstr = FilenameUtils.separatorsToUnix(fromstr);
            tostr = FilenameUtils.separatorsToUnix(tostr);
        }

        connect();

        ftp.rename(fromstr, tostr);

        cache.remove(fromstr);
    }

    @Override
    public void copyDirectory(Path tmpDir, Path tmpOutDir) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    public void deleteDirectoryContents(Path dir) throws IOException {
        connect();

        String wd = ftp.printWorkingDirectory();

        String dirstr = dir.toString();

        if (!dir.getFileSystem().getSeparator().equals("/")) {
            dirstr = FilenameUtils.separatorsToUnix(dirstr);
        }

        FTPFile[] fl = cache.get(dirstr);

        if (fl == null) {
            fl = ftp.listFiles(dirstr);
            cache.put(dirstr, fl);
        }

        ftp.changeWorkingDirectory(dirstr);

        for (FTPFile fff : fl) {
            if (fff.isDirectory()) {
                deleteDirectory(dir.resolve(fff.getName()));
            } else {
                ftp.deleteFile(fff.getName());
            }
        }

        ftp.changeWorkingDirectory(wd);

        cache.remove(dirstr);
    }

    @Override
    public void deleteDirectory(Path dir) throws IOException {
        deleteDirectoryContents(dir);

        String dirstr = dir.toString();

        if (!dir.getFileSystem().getSeparator().equals("/")) {
            dirstr = FilenameUtils.separatorsToUnix(dirstr);
        }

        ftp.removeDirectory(dirstr);
        cache.remove(dirstr);

        int pos = dirstr.lastIndexOf('/');

        if (pos > 0) {
            cache.remove(dirstr.substring(0, pos));
        }
    }

    @Override
    public void close() {
        cache.clear();

        try {
            ftp.disconnect();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static class FTPPrintStream extends PrintStream {

        OutputStream out;
        private FTPClient ftp;

        public FTPPrintStream(OutputStream ofs, String enc, FTPClient ftp) throws UnsupportedEncodingException {
            super(ofs, false, enc);

            this.ftp = ftp;
            out = ofs;

        }

        @Override
        public void close() {
            super.flush();

//   super.close();

            try {
                out.close();

                if (ftp.isConnected()) {
                    ftp.completePendingCommand();
                }
            } catch (FTPConnectionClosedException e) {
                log.warn("FTP connection is already closed");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
