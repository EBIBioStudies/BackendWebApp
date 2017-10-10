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

package uk.ac.ebi.biostd.webapp.server.endpoint.dir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.ebi.biostd.authz.Session;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.util.StringUtils;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.endpoint.ServiceServlet;
import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;
import uk.ac.ebi.biostd.webapp.server.vfs.InvalidPathException;
import uk.ac.ebi.biostd.webapp.server.vfs.PathInfo;
import uk.ac.ebi.biostd.webapp.server.vfs.PathTarget;

/**
 * Servlet implementation class DirServlet
 */

public class DirServlet extends ServiceServlet {

    public static final String USER_VIRT_DIR = "User";
    public static final String GROUP_VIRT_DIR = "Groups";
    private static final long serialVersionUID = 1L;
    private static final String SHOW_ARCHIVE_PARAMETER = "showArchive";
    private static final String PATH_PARAMETER = "path";
    private static final String PATTERN_PARAMETER = "pattern";
    private static final String FROM_PARAMETER = "from";
    private static final String TO_PARAMETER = "to";
    private static final String DEPTH_PARAMETER = "depth";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public DirServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp, Session sess)
            throws ServletException, IOException {

        if (sess == null || sess.isAnonymouns()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"User not logged in\"\n}");
            return;
        }

        String cmd = req.getParameter("command");

        if (cmd == null || Operation.DIR.name().equalsIgnoreCase(cmd)) {
            String shwAparm = req.getParameter(SHOW_ARCHIVE_PARAMETER);
            boolean showArch = shwAparm != null && ("1".equals(shwAparm) || "true".equalsIgnoreCase(shwAparm) || "yes"
                    .equalsIgnoreCase(shwAparm));
            String dpthPrm = req.getParameter(DEPTH_PARAMETER);

            int depth = 1;

            if (dpthPrm != null) {
                try {
                    depth = -1;
                    depth = Integer.parseInt(dpthPrm);
                } catch (Exception e) {
                }

            }

            dirList(req.getParameter(PATH_PARAMETER), depth, showArch, resp, sess);
        } else if (Operation.MOVE.name().equalsIgnoreCase(cmd)) {
            move(req, resp, sess, false);
        } else if (Operation.COPY.name().equalsIgnoreCase(cmd)) {
            move(req, resp, sess, true);
        } else if (Operation.RM.name().equalsIgnoreCase(cmd)) {
            delete(req, resp, sess, false);
        } else if (Operation.RMDIR.name().equalsIgnoreCase(cmd)) {
            delete(req, resp, sess, true);
        } else if (Operation.MKDIR.name().equalsIgnoreCase(cmd)) {
            mkdir(req, resp, sess);
        } else if (Operation.SEARCH.name().equalsIgnoreCase(cmd)) {
            search(req, resp, sess);
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid command\"\n}");
        }

    }


    private void search(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException {
        String pattern = req.getParameter(PATTERN_PARAMETER);
        String path = req.getParameter(PATH_PARAMETER);

        AbstractFileFilter filt = new WildcardFileFilter(pattern);

        User user = sess.getUser();

        PathInfo pi = null;

        if (path == null) {
            path = "/";
        }

        if (pattern == null) {
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Pattern not specified\"\n}");
            return;
        }

        try {
            pi = PathInfo.getPathInfo(path, user);
        } catch (InvalidPathException e1) {
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
            return;
        }

        PrintWriter out = resp.getWriter();

        JSONObject jsDir = new JSONObject();

        try {
            JSONArray files = new JSONArray();
            jsDir.put("files", files);
            jsDir.put("status", "OK");

            Path udir = BackendConfig.getUserDirPath(user);

            if (pi.getTarget() == PathTarget.ROOT) {
                findFiles(udir.toFile(), "/" + USER_VIRT_DIR, filt, files);

                if (user.getGroups() != null) {
                    for (UserGroup g : user.getGroups()) {
                        if (!g.isProject() || !BackendConfig.getServiceManager().getSecurityManager()
                                .mayUserReadGroupFiles(user, g)) {
                            continue;
                        }

                        String gpath = "/" + GROUP_VIRT_DIR + "/" + g.getName();
                        File rFile = BackendConfig.getGroupDirPath(g).toFile();

                        if (rFile.exists()) {
                            findFiles(rFile, gpath, filt, files);
                        }
                    }
                }
            } else if (pi.getTarget() == PathTarget.GROUPS) {
                if (user.getGroups() != null) {
                    for (UserGroup g : user.getGroups()) {
                        if (!g.isProject() || !BackendConfig.getServiceManager().getSecurityManager()
                                .mayUserReadGroupFiles(user, g)) {
                            continue;
                        }

                        String gpath = "/" + GROUP_VIRT_DIR + "/" + g.getName();
                        File rFile = BackendConfig.getGroupDirPath(g).toFile();

                        if (rFile.exists()) {
                            findFiles(rFile, gpath, filt, files);
                        }
                    }
                }
            } else if (Files.exists(pi.getRealPath())) {
                findFiles(pi.getRealPath().toFile(), FileNameUtil.toUnixPath(pi.getVirtPath()), filt, files);
            }

            out.println(jsDir.toString());

            return;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    private void findFiles(File realPath, String pathPfx, AbstractFileFilter filt, JSONArray files)
            throws IOException, JSONException {
        if (realPath.isDirectory()) {
            for (File p : realPath.listFiles()) {
                if (p.isDirectory()) {
                    findFiles(p, pathPfx + '/' + p.getName(), filt, files);
                } else if (filt.accept(p)) {
                    findFiles(p, pathPfx + '/' + p.getName(), filt, files);
                }
            }
        } else {
            JSONObject ent = new JSONObject();
            ent.put("name", realPath.getName());
            ent.put("path", pathPfx);
            ent.put("size", realPath.length());

            files.put(ent);
        }

    }

    private void mkdir(HttpServletRequest req, HttpServletResponse resp, Session sess) throws IOException {
        String from = req.getParameter(PATH_PARAMETER);

        if (from == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'dir' parameter is not defined\"\n}");
            return;
        }

        PathInfo dirPi = null;

        boolean pathOk = false;

        try {
            dirPi = PathInfo.getPathInfo(from, sess.getUser());

            if (!FileNameUtil.encodeOrCheck(dirPi)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
                return;
            }

            if (dirPi.getTarget() == PathTarget.USERREL || dirPi.getTarget() == PathTarget.GROUPREL) {
                if (Files.exists(dirPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Path exists\"\n}");
                    return;
                }

                pathOk = true;
            }
        } catch (Exception e) {
        }

        if (!pathOk) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
            return;
        }

        if (dirPi.getTarget() == PathTarget.GROUPREL && !BackendConfig.getServiceManager().getSecurityManager()
                .mayUserWriteGroupFiles(sess.getUser(), dirPi.getGroup())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter()
                    .print("{\n\"status\": \"FAIL\",\n\"message\": \"User has no permission to create directories in "
                            + "group's directory\"\n}");
            return;
        }

        try {
            Files.createDirectories(dirPi.getRealPath());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Directory create failed\"\n}");
            return;
        }

        resp.getWriter().print("{\n\"status\": \"OK\",\n\"message\": \"Directory create success\"\n}");
    }


    private void delete(HttpServletRequest req, HttpServletResponse resp, Session sess, boolean rmdir)
            throws IOException {
        String from = req.getParameter(PATH_PARAMETER);

        if (from == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'file' parameter is not defined\"\n}");
            return;
        }

        PathInfo delPi = null;

        boolean pathOk = false;

        try {
            delPi = PathInfo.getPathInfo(from, sess.getUser());

            if (!FileNameUtil.encodeOrCheck(delPi)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
                return;
            }

            if (delPi.getTarget() == PathTarget.USERREL || delPi.getTarget() == PathTarget.GROUPREL) {
                if (!Files.exists(delPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File doesn't exist\"\n}");
                    return;
                }

                pathOk = true;
            }
        } catch (Exception e) {
        }

        if (!pathOk) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
            return;
        }

        if (delPi.getTarget() == PathTarget.GROUPREL && !BackendConfig.getServiceManager().getSecurityManager()
                .mayUserWriteGroupFiles(sess.getUser(), delPi.getGroup())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter()
                    .print("{\n\"status\": \"FAIL\",\n\"message\": \"User has no permission to delete files in "
                            + "group's directory\"\n}");
            return;
        }

        try {
            if (rmdir) {
                if (!Files.isDirectory(delPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter()
                            .print("{\n\"status\": \"FAIL\",\n\"message\": \"Path must point to directory\"\n}");
                    return;
                }

                BackendConfig.getServiceManager().getFileManager().deleteDirectory(delPi.getRealPath());
            } else {
                if (Files.isDirectory(delPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter()
                            .print("{\n\"status\": \"FAIL\",\n\"message\": \"Path must point to regular file\"\n}");
                    return;
                }

                Files.delete(delPi.getRealPath());
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"File delete failed\"\n}");
            return;
        }

        resp.getWriter().print("{\n\"status\": \"OK\",\n\"message\": \"File delete success\"\n}");
    }


    private void move(HttpServletRequest req, HttpServletResponse resp, Session sess, boolean copy) throws IOException {
        String from = req.getParameter(FROM_PARAMETER);

        if (from == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'from' parameter is not defined\"\n}");
            return;
        }

        String to = req.getParameter(TO_PARAMETER);

        if (to == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"'to' parameter is not defined \"\n}");
            return;
        }

        PathInfo fromPi = null;
        PathInfo toPi = null;

        boolean pathOk = false;

        try {
            fromPi = PathInfo.getPathInfo(from, sess.getUser());

            if (!FileNameUtil.encodeOrCheck(fromPi)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid 'from' path characters\"\n}");
                return;
            }

            if (fromPi.getTarget() == PathTarget.USERREL || fromPi.getTarget() == PathTarget.GROUPREL) {
                if (!Files.exists(fromPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Source file doesn't exist\"\n}");
                    return;
                }

                pathOk = true;
            }
        } catch (Exception e) {
        }

        if (!pathOk) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid source path\"\n}");
            return;
        }

        pathOk = false;

        try {
            toPi = PathInfo.getPathInfo(to, sess.getUser());

            if (!FileNameUtil.encodeOrCheck(toPi)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid 'to' path characters\"\n}");
                return;
            }

            if (toPi.getTarget() == PathTarget.USERREL || toPi.getTarget() == PathTarget.GROUPREL) {
                if (Files.exists(toPi.getRealPath())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter()
                            .print("{\n\"status\": \"FAIL\",\n\"message\": \"Destination file already exists\"\n}");
                    return;
                }

                pathOk = true;
            }
        } catch (Exception e) {
        }

        if (!pathOk) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid destination path\"\n}");
            return;
        }

        if (toPi.getTarget() == PathTarget.GROUPREL && !BackendConfig.getServiceManager().getSecurityManager()
                .mayUserWriteGroupFiles(sess.getUser(), toPi.getGroup())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter()
                    .print("{\n\"status\": \"FAIL\",\n\"message\": \"User has no permission to write files to group's"
                            + " directory\"\n}");
            return;
        }

        try {
            if (copy) {
                if (Files.isDirectory(fromPi.getRealPath())) {
                    BackendConfig.getServiceManager().getFileManager()
                            .linkOrCopyDirectory(fromPi.getRealPath(), toPi.getRealPath());
                } else {
                    BackendConfig.getServiceManager().getFileManager()
                            .linkOrCopyFile(fromPi.getRealPath(), toPi.getRealPath());
                }
            } else {
                Files.createDirectories(toPi.getRealPath().getParent());
                Files.move(fromPi.getRealPath(), toPi.getRealPath());
            }

        } catch (Exception e) {
            e.printStackTrace();

            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Operation failed\"\n}");
            return;
        }

        resp.getWriter().print("{\n\"status\": \"OK\",\n\"message\": \"Operation success\"\n}");
    }


    private void dirList(String path, int depth, boolean showArch, HttpServletResponse resp, Session sess)
            throws IOException {

        if (depth <= 0) {
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid depth\"\n}");
            return;
        }

        User user = sess.getUser();

        PathInfo pi = null;

        try {
            pi = PathInfo.getPathInfo(path, user);
        } catch (InvalidPathException e1) {
            resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
            return;
        }

        PrintWriter out = resp.getWriter();

        JSONObject jsDir = new JSONObject();

        try {
            JSONArray files = new JSONArray();
            jsDir.put("files", files);
            jsDir.put("status", "OK");

            if (pi.getTarget() == PathTarget.ROOT) {
                jsDir.put("path", "/");

                Path udir = BackendConfig.getUserDirPath(user);

                if (depth > 1 && Files.exists(udir)) {
                    listPath(new FileNode(udir), "/" + USER_VIRT_DIR, USER_VIRT_DIR, files, showArch, depth - 1);
                } else {
                    JSONObject jsUsr = new JSONObject();
                    jsUsr.put("name", USER_VIRT_DIR);
                    jsUsr.put("type", "DIR");
                    jsUsr.put("path", "/" + USER_VIRT_DIR);

                    if (depth > 1) {
                        jsUsr.put("files", new JSONArray());
                    }

                    files.put(jsUsr);
                }

                JSONObject jsGrp = new JSONObject();
                jsGrp.put("name", GROUP_VIRT_DIR);
                jsGrp.put("type", "DIR");
                jsGrp.put("path", "/" + GROUP_VIRT_DIR);

                if (depth > 1) {
                    JSONArray jsGrps = new JSONArray();

                    jsGrp.put("files", jsGrps);

                    Collection<UserGroup> grps = new ArrayList<>();

                    for (UserGroup g : user.getGroups()) {
                        if (g.isProject() && BackendConfig.getServiceManager().getSecurityManager()
                                .mayUserReadGroupFiles(user, g)) {
                            grps.add(g);
                        }
                    }

                    listGroups(grps, jsGrps, showArch, depth - 1);
                }

                files.put(jsGrp);
            } else if (pi.getTarget() == PathTarget.GROUPS) {
                listGroups(pi.getGroups(), files, showArch, depth);
            } else if ((pi.getTarget() != PathTarget.GROUP && pi.getTarget() != PathTarget.USER) || Files.exists(pi
                    .getRealPath())) //if( pi.getTarget() == DirTarget.GROUPREL || pi.getTarget() == DirTarget.USERREL )
            {
                StringBuilder sb = new StringBuilder();

                Path virtPath = pi.getVirtPath();

                for (int i = 0; i < virtPath.getNameCount(); i++) {
                    sb.append('/').append(virtPath.getName(i).toString());
                }

                if (Files.exists(pi.getRealPath())) {
                    listPath(new FileNode(pi.getRealPath()), sb.toString(),
                            virtPath.getName(virtPath.getNameCount() - 1).toString(), files, showArch, depth);
                } else {
                    Node nd = null;

                    Path basepath = pi.getRealBasePath();
                    Path realRelPath = pi.getRelPath();

                    for (int i = 0; i < realRelPath.getNameCount(); i++) {
                        String fname = realRelPath.getName(i).toString();
                        basepath = basepath.resolve(fname);

                        if (nd != null) {
                            boolean found = false;
                            for (Node snd : nd.getSubnodes()) {
                                if (snd.getName().equals(fname)) {
                                    nd = snd;
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                nd = null;
                                break;
                            }

                        } else if (Files.exists(basepath) && !Files.isDirectory(basepath) && fname.length() > 4 && fname
                                .substring(fname.length() - 4).equalsIgnoreCase(".zip")) {
                            nd = listZipArchive(basepath.toFile(), null);
                        }

                    }

                    if (nd == null) {
                        resp.getWriter().print("{\n\"status\": \"FAIL\",\n\"message\": \"Invalid path\"\n}");
                        return;
                    }

                    listPath(nd, sb.toString(), virtPath.getName(virtPath.getNameCount() - 1).toString(), files,
                            showArch, depth);

                }

                jsDir = jsDir.getJSONArray("files").getJSONObject(0);
                jsDir.put("status", "OK");

            }

            out.println(jsDir.toString());

            return;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    private void listGroups(Collection<UserGroup> grps, JSONArray jsGrps, boolean showArch, int dp)
            throws JSONException, IOException {
        if (dp <= 0) {
            return;
        }

        for (UserGroup g : grps) {
            String path = "/" + GROUP_VIRT_DIR + "/" + g.getName();

            Path gPath = BackendConfig.getGroupDirPath(g);

            if (Files.exists(gPath)) {
                if (dp >= 1) {
                    listPath(new FileNode(BackendConfig.getGroupDirPath(g)), path, g.getName(), jsGrps, showArch,
                            dp - 1);
                }
            } else {
                JSONObject jsf = new JSONObject();

                jsGrps.put(jsf);

                jsf.put("name", g.getName());
                jsf.put("path", path);
                jsf.put("type", "DIR");

                if (dp > 1) {
                    jsf.put("files", new JSONArray());
                }
            }


        }

    }

    private void listPath(Node node, String relPath, String fname, JSONArray filesAcc, boolean showArch, int dp)
            throws JSONException, IOException {
        if (dp < 0) {
            return;
        }

        boolean arch = false;

        if (!node.inArchive() && !node.isDirectory() && fname.length() > 4 && fname.substring(fname.length() - 4)
                .equalsIgnoreCase(".zip")) {
            arch = true;
        }

        JSONObject jsf = new JSONObject();

        filesAcc.put(jsf);

        String decFN = fname;

//  if( BackendConfig.isEncodeFileNames() && ! node.inArchive() )
//   decFN = FileNameUtil.decodeString(fname);

        jsf.put("name", decFN);
        jsf.put("path", relPath);

        if (node.isDirectory()) {
            jsf.put("type", "DIR");

            if (dp >= 1) {
                JSONArray acc = new JSONArray();

                jsf.put("files", acc);

                Collection<Node> list = node.getSubnodes();

                for (Node f : list) {
                    String fn = f.getName();

                    if (!node.inArchive()) {
                        fn = FileNameUtil.decode(fn);
                    }

                    listPath(f, relPath + "/" + fn, fn, acc, showArch, dp - 1);
                }
            }
        } else if (arch) {
            jsf.put("type", "ARCHIVE");
            jsf.put("size", String.valueOf(node.getSize()));

            if (showArch && dp >= 1) {
                JSONArray acc = new JSONArray();

                jsf.put("files", acc);

                Collection<Node> list = listZipArchive(node.getFile().toFile(), null).getSubnodes();

                for (Node f : list) {
                    listPath(f, relPath + "/" + f.getName(), f.getName(), acc, showArch, dp - 1);
                }
            }
        } else {
            jsf.put("size", String.valueOf(node.getSize()));
            jsf.put("type", "FILE");
        }

    }

    private ZDir listZipArchive(File f, String npath) throws IOException {
        ZDir root = new ZDir();

        try (ZipFile zipFile = new ZipFile(f)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                List<String> parts = StringUtils.splitString(entry.getName(), '/');

                ZDir cDir = root;

//    int n = entry.isDirectory()?parts.size()-1 : parts.size();

                for (int i = 0; i < parts.size() - 1; i++) {
                    ZDir d = cDir.dirs.get(parts.get(i));

                    if (d == null) {
                        d = new ZDir();

                        d.name = parts.get(i);

                        cDir.dirs.put(d.name, d);
                    }

                    cDir = d;
                }

                if (!entry.isDirectory()) {
                    String fName = parts.get(parts.size() - 1);

                    ZFile zf = new ZFile();

                    zf.name = fName;
                    zf.size = entry.getSize();

                    cDir.files.add(zf);
                }
            }

        } catch (Exception e) {
        }

        return root;
    }

    interface Node {

        String getName();

        boolean isDirectory();

        long getSize() throws IOException;

        Collection<Node> getSubnodes() throws IOException;

        Path getFile();

        boolean inArchive();
    }
 
 /*
 private void listDirectory( Node dir, String path, boolean showArch, Appendable out ) throws IOException
 {
  out.append("[");
  
  Collection<Node> list = dir.getSubnodes();
  
  boolean first = true;
  
  for( Node f : list )
  {
   if( ! first )
    out.append(",\n");
   else
   {
    out.append("\n");
    first = false;
   }
   
   String fname = f.getName();
   String npath =  path+"/"+fname;
   
   out.append("{\n\"name\": \"");
   StringUtils.appendAsCStr(out, fname );
   out.append("\",\n\"path\": \"");
   StringUtils.appendAsCStr(out, npath );
   out.append("\",\n\"size\": ");
   out.append(String.valueOf(f.getSize()));
   out.append(",\n\"type\": \"");
   
   if( f.isDirectory() )
   {
    out.append("DIR\",\n\"files\":");
    
    listDirectory(f, npath, showArch, out);
   }
   else if( f.getFile() != null && fname.length() > 4 && fname.substring(fname.length()-4).equalsIgnoreCase(".zip") )
   {
    out.append("ARCHIVE\"");
    
    if( showArch )
    {
     out.append(",\n\"files\":");
   
     listDirectory( listZipArchive(f.getFile().toFile(), npath), npath, showArch, out);
    }
   }
   else
    out.append("FILE\"");
  
   out.append("\n}");
  }
  
  out.append("\n]\n");

 }
*/

    private static class FileNode implements Node {

        Path file;

        FileNode(Path f) {
            file = f;
        }

        @Override
        public String getName() {
            return file.getFileName().toString();
        }

        @Override
        public boolean isDirectory() {
            return Files.isDirectory(file);
        }

        @Override
        public long getSize() throws IOException {
            return Files.size(file);
        }

        @Override
        public Collection<Node> getSubnodes() throws IOException {

            try (Stream<Path> list = Files.list(file)) {
                ArrayList<Node> sbn = new ArrayList<>();

                list.forEach(f -> sbn.add(new FileNode(f)));

                return sbn;
            }

        }

        @Override
        public boolean inArchive() {
            return false;
        }

        @Override
        public Path getFile() {
            return file;
        }
    }

    private static class ZFile implements Node {

        long size;
        String name;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public Collection<Node> getSubnodes() {
            return null;
        }

        @Override
        public Path getFile() {
            return null;
        }

        @Override
        public boolean inArchive() {
            return true;
        }
    }

    private static class ZDir implements Node {

        String name;
        List<ZFile> files = new ArrayList<>();
        Map<String, ZDir> dirs = new HashMap<>();

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public boolean inArchive() {
            return true;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public Path getFile() {
            return null;
        }

        @Override
        public Collection<Node> getSubnodes() {
            ArrayList<Node> sbn = new ArrayList<>(files.size() + dirs.size());

            sbn.addAll(dirs.values());
            sbn.addAll(files);

            return sbn;
        }
    }


}
