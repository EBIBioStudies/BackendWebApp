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

package uk.ac.ebi.biostd.webapp.server.endpoint.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.model.FileRef;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.mng.FileManager;

/**
 * Servlet implementation class ToolsServlet
 */

public class ToolsServlet extends ServiceServlet {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ToolsServlet.class);
    private volatile boolean moreWorkToDo = true;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {
        if (sess == null || sess.isAnonymouns()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("FAIL User not logged in");
            return;
        }

        if (!sess.getUser().isSuperuser()) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().print("FAIL Only superuser can run it");
            return;
        }

        Operation act = null;

        String pi = req.getPathInfo();

        if (pi != null && pi.length() > 1) {
            pi = pi.substring(1);

            for (Operation op : Operation.values()) {
                if (op.name().equalsIgnoreCase(pi)) {
                    act = op;
                    break;
                }
            }

        }

        if (act == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("FAIL Invalid path: " + pi);
            return;
        }

        switch (act) {
            case FIX_FILE_TYPE:

                resp.setContentType("text/plain");
                fixFileType(resp.getWriter());

                break;

            case FIX_FILE_SIZE:

                resp.setContentType("text/plain");
                fixFileSize(resp.getWriter());

                break;

            case FIX_DIRECTORY_SIZE:

                resp.setContentType("text/plain");
                fixDirectorySize(resp.getWriter());

                break;

            case FIX_SECRET_KEY:

                resp.setContentType("text/plain");
                fixSecretKey(resp.getWriter());

                break;

            case UPDATE_USER_DIR_LAYOUT:

                resp.setContentType("text/plain");
                updateUserDirLayout(resp.getWriter(), req.getParameter("OldDir"));

                break;

            case REVERSE_USER_DIR_LAYOUT:

                resp.setContentType("text/plain");
                reverseUserDirLayout(resp.getWriter(), req.getParameter("OldDir"));

                break;

            case RELINK_DROPBOXES:

                resp.setContentType("text/plain");
                relinkDropboxes(resp.getWriter());

                break;

            case CLEAN_EXP_USERS:

                BackendConfig.getServiceManager().getSecurityManager().removeExpiredUsers();

                break;

            case REFRESH_USERS:

                BackendConfig.getServiceManager().getSecurityManager().refreshUserCache();

                break;

            case REGENERATE_JSON:

                resp.setContentType("text/plain");
                //regenerateJsonFiles(resp.getWriter());
                regenerateJsonFilesNoThreads(resp.getWriter());

                break;
            default:
                break;
        }

    }

    private void fixSecretKey(PrintWriter writer) {

        EntityManager mngr = null;
        TypedQuery<Submission> query = null;

        int count = 0;
        int emCount = 0;

        while (true) {
            if (emCount > 1000) {
                emCount = 0;
                mngr.close();
                mngr = null;
            }

            if (mngr == null) {
                mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

                query = mngr.createQuery("select s from Submission s where s.secretKey is null", Submission.class);
                query.setMaxResults(50);
            }

            List<Submission> sLst = null;

            EntityTransaction t = mngr.getTransaction();

            t.begin();

            sLst = query.getResultList();

            if (sLst.size() == 0) {
                t.commit();

                writer.println("Processing finished. Total: " + count);
                break;
            }

            for (Submission s : sLst) {
                s.setSecretKey(UUID.randomUUID().toString());
                count++;
                emCount++;
            }

            t.commit();

            writer.println("Processed submissions: " + count);

        }

        if (mngr != null) {
            mngr.close();
        }
    }

    private void relinkDropboxes(PrintWriter out) throws IOException {
        FileManager fMgr = BackendConfig.getServiceManager().getFileManager();

        Consumer<Path> wiper = new Consumer<Path>() {
            @Override
            public void accept(Path f) {
                try {
                    if (Files.isDirectory(f)) {
                        fMgr.deleteDirectory(f);
                    } else {
                        Files.delete(f);
                    }
                } catch (Exception e) {
                    out.println("Can't delete: " + f);
                }

            }
        };

        Files.list(BackendConfig.getUsersIndexPath()).forEach(wiper);
        Files.list(BackendConfig.getGroupIndexPath()).forEach(wiper);

        EntityManager mngr = null;

        mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction t = mngr.getTransaction();

        t.begin();

        List<User> users = mngr.createQuery("select u from User u where u.active=1", User.class).getResultList();

        for (User u : users) {

            Path nud = BackendConfig.getUserDirPath(u);

            if (!Files.exists(nud)) {
                Files.createDirectories(nud);

                if (BackendConfig.isPublicDropboxes()) {
                    try {
                        Files.setPosixFilePermissions(nud.getParent(), BackendConfig.rwx__x__x);
                        Files.setPosixFilePermissions(nud, BackendConfig.rwxrwxrwx);
                    } catch (Exception e) {
                        out.println("ERROR: setPosixFilePermissions failed " + nud + " : " + e);
                        continue;
                    }
                }
            }

            if (u.getLogin() != null) {
                Path ll = BackendConfig.getUserLoginLinkPath(u);

                try {
                    Files.createDirectories(ll.getParent());
                    Files.createSymbolicLink(ll, nud);
                } catch (Exception e) {
                    out.println("ERROR: Linking " + ll + " -> " + nud + " failed: " + e);
                    continue;
                }
            }

            if (u.getEmail() != null) {
                Path ll = BackendConfig.getUserEmailLinkPath(u);

                try {
                    Files.createDirectories(ll.getParent());
                    Files.createSymbolicLink(ll, nud);
                } catch (Exception e) {
                    out.println("ERROR: Linking " + ll + " -> " + nud + " failed: " + e);
                    continue;
                }
            }

            out.println("OK user: " + u.getFullName() + " <" + u.getEmail() + ">");
        }

        t.commit();

        t.begin();

        List<UserGroup> groups = mngr.createQuery("select g from UserGroup g where g.project=1", UserGroup.class)
                .getResultList();

        for (UserGroup g : groups) {

            Path nud = BackendConfig.getGroupDirPath(g);

            if (!Files.exists(nud)) {
                Files.createDirectories(nud);

                if (BackendConfig.isPublicDropboxes()) {
                    try {
                        Files.setPosixFilePermissions(nud.getParent(), BackendConfig.rwx__x__x);
                        Files.setPosixFilePermissions(nud, BackendConfig.rwxrwxrwx);
                    } catch (Exception e) {
                        out.println("ERROR: setPosixFilePermissions failed " + nud + " : " + e);
                        continue;
                    }
                }
            }

            if (g.getName() != null) {
                Path ll = BackendConfig.getGroupLinkPath(g);

                try {
                    Files.createDirectories(ll.getParent());
                    Files.createSymbolicLink(ll, nud);
                } catch (Exception e) {
                    out.println("ERROR: Linking " + ll + " -> " + nud + " failed: " + e);
                    continue;
                }
            }

            out.println("OK group: " + g.getName());
        }

        t.commit();

        mngr.close();
    }

    private void fixFileSize(PrintWriter out) {
        EntityManager mngr = null;

        int blockSz = 5000;

        int success = 0;
        int fail = 0;
        int skip = 0;

        try {
            while (true) {
                if (mngr != null) {
                    mngr.close();
                }

                mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

                EntityTransaction t = mngr.getTransaction();

                t.begin();

                Query q = mngr.createQuery("select fr from FileRef fr WHERE size=0 AND directory=0 ");

                q.setMaxResults(blockSz);

                if (skip > 0) {
                    q.setFirstResult(skip);
                }

                List<FileRef> res = q.getResultList();

                if (res.size() == 0) {
                    break;
                }

                for (FileRef fr : res) {
                    Submission s = fr.getHostSection().getSubmission();
                    Path filesPath = null;

                    if (s.getVersion() > 0) {
                        filesPath = BackendConfig.getSubmissionFilesPath(s);
                    } else {
                        filesPath = BackendConfig.getSubmissionHistoryPath(s).resolve("Files");
                    }

                    String relpath = fr.getPath();

                    if (relpath == null) {
                        relpath = fr.getName();
                    }

                    Path file = filesPath.resolve(relpath);

                    if (Files.exists(file)) {
                        fr.setDirectory(Files.isDirectory(file));
                        fr.setSize(Files.size(file));

                        if (fr.getSize() == 0) {
                            skip++;
                            out.println("Zero length file: " + file);
                        }

                        success++;
                    } else {
                        out.println("Missing: " + file);
                        fail++;
                    }
                }

                t.commit();

                out.append("Processed " + (success + fail) + "\n");
                out.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(out);
        } finally {
            if (mngr != null && mngr.isOpen()) {
                mngr.close();
            }
        }

        out.append("Finished success: " + success + " fail: " + fail);
    }


    private void fixDirectorySize(PrintWriter out) {
        EntityManager mngr = null;

        int blockSz = 5000;

        int success = 0;
        int fail = 0;
        int skip = 0;

        try {
            while (true) {
                if (mngr != null) {
                    mngr.close();
                }

                mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

                EntityTransaction t = mngr.getTransaction();

                t.begin();

                TypedQuery<FileRef> q = mngr
                        .createQuery("select fr from FileRef fr WHERE size=0 AND directory=1 ", FileRef.class);

                q.setMaxResults(blockSz);

                if (skip > 0) {
                    q.setFirstResult(skip);
                }

                List<FileRef> res = q.getResultList();

                if (res.size() == 0) {
                    break;
                }

                for (FileRef fr : res) {
                    Submission s = fr.getHostSection().getSubmission();
                    Path filesPath = null;

                    if (s.getVersion() > 0) {
                        filesPath = BackendConfig.getSubmissionFilesPath(s);
                    } else {
                        filesPath = BackendConfig.getSubmissionHistoryPath(s).resolve("Files");
                    }

                    String relpath = fr.getPath();

                    if (relpath == null) {
                        relpath = fr.getName();
                    }

                    Path file = filesPath.resolve(relpath);

                    if (Files.exists(file)) {
                        if (!Files.isDirectory(file)) {
                            out.println("Not directory: " + file);
                            fr.setDirectory(false);
                            fr.setSize(Files.size(file));
                        } else {
                            fr.setSize(BackendConfig.getServiceManager().getFileManager().countDirectorySize(file));
                        }

                        if (fr.getSize() == 0) {
                            skip++;
                        }

                        success++;
                    } else {
                        out.println("Missing: " + file);
                        fail++;
                    }
                }

                t.commit();

                out.append("Processed " + (success + fail) + "\n");
                out.flush();

            }

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(out);
        } finally {
            if (mngr != null && mngr.isOpen()) {
                mngr.close();
            }
        }

        out.append("Finished success: " + success + " fail: " + fail);
    }


    private void updateUserDirLayout(PrintWriter out, String oldDir) {
        if (oldDir == null) {
            out.println("OldDir parameter missing");
            return;
        }

        Path p = FileSystems.getDefault().getPath(oldDir);

        if (!Files.exists(p) || !Files.isDirectory(p)) {
            out.println("OldDir parameter has invaid value");
            return;
        }

        EntityManager mngr = null;

        mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction t = mngr.getTransaction();

        t.begin();

        List<User> users = mngr.createQuery("select u from User u where u.active=1", User.class).getResultList();

        for (User u : users) {
            if (u.getSecret() == null) {
                u.setSecret(UUID.randomUUID().toString());
            }

            Path oup = p.resolve(String.valueOf(u.getId()));
            Path nud = BackendConfig.getUserDirPath(u);

            if (Files.exists(nud)) {
                out.println("ERROR: Path exists: " + nud);
                continue;
            }

            if (Files.exists(oup) && Files.isDirectory(oup)) {

                try {
                    BackendConfig.getServiceManager().getFileManager().linkOrCopyDirectory(oup, nud);

                    //Files.move(oup, nud);
                } catch (IOException e) {
                    out.println("ERROR: Moving " + oup + " -> " + nud + " failed: " + e);
                    continue;
                }
            } else {
                try {
                    Files.createDirectories(nud);
                } catch (Exception e) {
                    out.println("ERROR: Can't create directory " + nud + " : " + e);
                    continue;
                }
            }

            if (BackendConfig.isPublicDropboxes()) {
                try {
                    Files.setPosixFilePermissions(nud.getParent(), BackendConfig.rwx__x__x);
                    Files.setPosixFilePermissions(nud, BackendConfig.rwxrwxrwx);
                } catch (Exception e) {
                    out.println("ERROR: setPosixFilePermissions failed " + nud + " : " + e);
                    continue;
                }
            }

            if (u.getLogin() != null) {
                Path ll = BackendConfig.getUserLoginLinkPath(u);

                try {
                    Files.createDirectories(ll.getParent());
                    Files.createSymbolicLink(ll, nud);
                } catch (Exception e) {
                    out.println("ERROR: Linking " + ll + " -> " + nud + " failed: " + e);
                    continue;
                }
            }

            if (u.getEmail() != null) {
                Path ll = BackendConfig.getUserEmailLinkPath(u);

                try {
                    Files.createDirectories(ll.getParent());
                    Files.createSymbolicLink(ll, nud);
                } catch (Exception e) {
                    out.println("ERROR: Linking " + ll + " -> " + nud + " failed: " + e);
                    continue;
                }
            }

            out.println("OK: " + u.getFullName() + " <" + u.getEmail() + ">");
        }

        t.commit();

        mngr.close();

    }

    private void reverseUserDirLayout(PrintWriter out, String oldDir) {
        if (oldDir == null) {
            out.println("OldDir parameter missing");
            return;
        }

        Path p = FileSystems.getDefault().getPath(oldDir);

        if (!Files.exists(p) || !Files.isDirectory(p)) {
            out.println("OldDir parameter has invaid value");
            return;
        }

        EntityManager mngr = null;

        mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

        EntityTransaction t = mngr.getTransaction();

        t.begin();

        List<User> users = mngr.createQuery("select u from User u where u.active=1", User.class).getResultList();

        for (User u : users) {

            Path oup = p.resolve(String.valueOf(u.getId()));
            Path nud = BackendConfig.getUserDirPath(u);

            if (!Files.exists(nud)) {
                continue;
            }

            try {
                BackendConfig.getServiceManager().getFileManager().linkOrCopyDirectory(nud, oup);
            } catch (IOException e) {
                out.println("Moving " + nud + " -> " + oup + " failed: " + e);
                continue;
            }

            out.println("OK: " + u.getFullName() + " <" + u.getEmail() + ">");

        }

        t.commit();

        mngr.close();

    }

    private void fixFileType(PrintWriter out) {
        EntityManager mngr = null;

        int blockSz = 1000;

        try {
            int offset = 0;

            while (true) {
                if (mngr != null) {
                    mngr.close();
                }

                mngr = BackendConfig.getEntityManagerFactory().createEntityManager();

                EntityTransaction t = mngr.getTransaction();

                t.begin();

                Query q = mngr.createQuery("select sb from Submission sb");

                q.setFirstResult(offset);
                q.setMaxResults(blockSz);

                List<Submission> res = q.getResultList();

                if (res.size() == 0) {
                    break;
                }

                for (Submission s : res) {
                    fixFileType(s, s.getRootSection());
                }

                t.commit();

                out.append("Processed " + offset + " to " + (offset + res.size()) + "\n");

                offset += res.size();
            }

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(out);
        } finally {
            if (mngr != null && mngr.isOpen()) {
                mngr.close();
            }
        }

        out.append("Finished");

    }

    private void fixFileType(Submission s, Section sec) {
        if (sec.getSections() != null) {
            for (Section ss : sec.getSections()) {
                fixFileType(s, ss);
            }
        }

        if (sec.getFileRefs() != null) {
            Path filesPath = BackendConfig.getSubmissionFilesPath(s);

            for (FileRef fr : sec.getFileRefs()) {
                Path p = filesPath.resolve(fr.getName());
                fr.setDirectory(Files.isDirectory(p));
            }
        }
    }

    private void regenerateJsonFiles(PrintWriter servletOut) {
        EntityManager entityMan = null;
        moreWorkToDo = true;

        int blockSz = 100;

        ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        pool.allowCoreThreadTimeOut(true);

        int offset = 0;
        boolean processing = true;

        while (moreWorkToDo) {
            pool.execute(new RegenerateJsonOuputRunnable(offset, blockSz, servletOut));
            offset += blockSz;

   /*
   while (pool.getActiveCount() > 10) {
    try { Thread.sleep(50);} catch (Exception ex){}
   }
   */
        }

        servletOut.append("Terminating thread pool");
        servletOut.flush();
        log.info("Terminating thread pool");

        pool.shutdown();
        while (!pool.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {
            }
        }

        servletOut.append("Finished");
        servletOut.flush();
        log.info("Finished regenerating json files!");

    }

    private void regenerateJsonFilesNoThreads(PrintWriter servletOut) {
        moreWorkToDo = true;

        int blockSz = 1000;
        int offset = 0;
        boolean processing = true;

        String threadName = Thread.currentThread().getName();
        EntityManager entityMan = null;

        try {
            while (processing) {
                if (entityMan != null) {
                    entityMan.close();
                }

                entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();

                Query q = entityMan.createQuery("select sb from Submission sb");
                q.setFirstResult(offset);
                q.setMaxResults(blockSz);

                servletOut.append(threadName + " Collecting from " + offset + " to " + (offset + blockSz)
                        + " submissions\n");
                servletOut.flush();
                log.info(threadName + " Collecting from " + offset + " to " + (offset + blockSz) + " submissions");

                List<Submission> res = q.getResultList();

                if (res.size() == 0) {
                    break;
                }

                int cnt = 0;
                for (Submission s : res) {
                    Path filesP = BackendConfig.getSubmissionPath(s);

                    try (PrintStream jsonOut = new PrintStream(filesP.resolve(s.getAccNo() + ".json").toFile())) {
                        new JSONFormatter(jsonOut, true).format(s, jsonOut);
                        jsonOut.close();
                        cnt++;
                    } catch (Exception e) {
                        log.error(threadName + " Error regenerating json files " + e.getMessage());
                        servletOut.append(threadName + " Can't generate JSON source file: " + e.getMessage() + "\n");
                        e.printStackTrace(servletOut);
                    }
                }
                log.info(threadName + " Exported : " + res.size() + " (" + cnt + ") submissions");
                servletOut.append(threadName + " Exported : " + res.size() + " (" + cnt + ") submissions\n");

                offset += res.size();
            }
        } finally {
            if (entityMan != null && entityMan.isOpen()) {
                entityMan.close();
            }
        }

        servletOut.append("Finished");
        servletOut.flush();
        log.info("Finished regenerating json files!");
    }

    private enum Operation {
        RELINK_DROPBOXES,
        FIX_SECRET_KEY,
        FIX_FILE_TYPE,
        FIX_FILE_SIZE,
        FIX_DIRECTORY_SIZE,
        CLEAN_EXP_USERS,
        REFRESH_USERS,
        UPDATE_USER_DIR_LAYOUT,
        REVERSE_USER_DIR_LAYOUT,
        REGENERATE_JSON
    }

    public class RegenerateJsonOuputRunnable implements Runnable {

        private PrintWriter servletOut;
        private int offset;
        private int blockSz;

        public RegenerateJsonOuputRunnable(int offset, int blockSz, PrintWriter servletOut) {
            this.offset = offset;
            this.blockSz = blockSz;
            this.servletOut = servletOut;
        }

        public void run() {

            String threadName = Thread.currentThread().getName();
            EntityManager entityMan = null;

            try {
                entityMan = BackendConfig.getEntityManagerFactory().createEntityManager();

                Query q = entityMan.createQuery("select sb from Submission sb");
                q.setFirstResult(offset);
                q.setMaxResults(blockSz);

                servletOut.append(threadName + " Collecting from " + offset + " to " + (offset + blockSz)
                        + " submissions\n");
                servletOut.flush();
                log.info(threadName + " Collecting from " + offset + " to " + (offset + blockSz) + " submissions");

                List<Submission> res = q.getResultList();

                if (res.size() == 0) {
                    moreWorkToDo = false;
                    return;
                }

                int cnt = 0;
                for (Submission s : res) {

                    Path filesP = BackendConfig.getSubmissionPath(s);

                    try (PrintStream jsonOut = new PrintStream(filesP.resolve(s.getAccNo() + ".json").toFile())) {
                        new JSONFormatter(jsonOut, true).format(s, jsonOut);
                        jsonOut.close();
                        cnt++;
                    } catch (Exception e) {
                        log.error(threadName + " Error regenerating json files " + e.getMessage());
                        servletOut.append(threadName + " Can't generate JSON source file: " + e.getMessage() + "\n");
                        e.printStackTrace(servletOut);
                    }
                }
                log.info(threadName + "Exported : " + res.size() + " (" + cnt + ") submissions");
                servletOut.append(threadName + " - Exported : " + res.size() + " (" + cnt + ") submissions\n");


            } finally {
                if (entityMan != null && entityMan.isOpen()) {
                    entityMan.close();
                }
            }
        }
    }

}
