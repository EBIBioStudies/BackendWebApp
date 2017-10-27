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

package uk.ac.ebi.biostd.webapp.server.vfs;

import static uk.ac.ebi.biostd.webapp.server.endpoint.dir.DirServlet.GROUP_VIRT_DIR;
import static uk.ac.ebi.biostd.webapp.server.endpoint.dir.DirServlet.USER_VIRT_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import uk.ac.ebi.biostd.authz.User;
import uk.ac.ebi.biostd.authz.UserGroup;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

public class PathInfo {

    private PathTarget target;
    private boolean absolute;
    private Path realPath;
    private Path virtPath;
    private Path realBasePath;
    private Path virtBasePath;
    private Path relPath;
    private UserGroup group;
    private Collection<UserGroup> groups;

    public static PathInfo getPathInfo(String path, User user) throws InvalidPathException {
        if (path == null) {
            PathInfo pi = new PathInfo();

            pi.setTarget(PathTarget.ROOT);
            pi.setVirtPath(Paths.get("/"));

            return pi;
        }

        path = path.trim();

        return getPathInfo(Paths.get(path), user);
    }

    public static PathInfo getPathInfo(Path pth, User user) throws InvalidPathException {
        PathInfo pi = new PathInfo();

        Path virtRelPath = null;

        boolean absolute = pth.getRoot() != null;

        virtRelPath = pth.normalize();

        String frstComp = virtRelPath.getNameCount() > 0 ? virtRelPath.getName(0).toString() : "";

        pi.setAbsolute(absolute);

        if (!absolute) {
            Path udir = BackendConfig.getUserDirPath(user);
            Path targetPath = udir.resolve(virtRelPath);

            if (!targetPath.startsWith(udir)) {
                throw new InvalidPathException();
            }

            if (frstComp.length() == 0) {
                pi.setTarget(PathTarget.USER);
            } else {
                pi.setTarget(PathTarget.USERREL);
            }

            pi.setRelPath(virtRelPath);
            pi.setRealPath(targetPath);
            pi.setRealBasePath(udir);
            pi.setVirtBasePath(Paths.get("/").resolve(USER_VIRT_DIR));
            pi.setVirtPath(pi.getVirtBasePath().resolve(virtRelPath));

            return pi;
        } else if (frstComp.length() == 0) {
            pi.setTarget(PathTarget.ROOT);
            pi.setVirtPath(virtRelPath);
            pi.setVirtBasePath(virtRelPath);

            return pi;
        } else {
            pi.setVirtPath(virtRelPath);
        }

        if (USER_VIRT_DIR.equals(frstComp)) {
            Path udir = BackendConfig.getUserDirPath(user);

            Path relPath = null;
            Path targetPath = null;

            if (virtRelPath.getNameCount() > 1) {
                relPath = virtRelPath.subpath(1, virtRelPath.getNameCount());
                targetPath = udir.resolve(relPath);

                if (!targetPath.startsWith(udir)) {
                    throw new InvalidPathException();
                }

                pi.setTarget(PathTarget.USERREL);
            } else {
                pi.setTarget(PathTarget.USER);
                relPath = Paths.get("");
                targetPath = udir;
            }

            pi.setRelPath(relPath);
            pi.setRealPath(targetPath);
            pi.setRealBasePath(udir);
            pi.setVirtBasePath(Paths.get(USER_VIRT_DIR));

        } else if (GROUP_VIRT_DIR.equals(frstComp)) {

            Collection<UserGroup> grps = new ArrayList<>();

            if (user.getGroups() != null) {
                for (UserGroup g : user.getGroups()) {
                    if (g.isProject() && BackendConfig.getServiceManager().getSecurityManager()
                            .mayUserReadGroupFiles(user, g)) {
                        grps.add(g);
                    }
                }
            }

            pi.setGroups(grps);

            if (virtRelPath.getNameCount() == 1) {
                pi.setTarget(PathTarget.GROUPS);

                return pi;
            } else {
                if (virtRelPath.getNameCount() == 2) {
                    pi.setTarget(PathTarget.GROUP);
                } else {
                    pi.setTarget(PathTarget.GROUPREL);
                }

                String gName = virtRelPath.getName(1).toString();

                Path gPath = null;

                for (UserGroup g : grps) {
                    if (gName.equals(g.getName())) {
                        gPath = BackendConfig.getGroupDirPath(g);
                        pi.setGroup(g);
                        break;
                    }
                }

                if (gPath == null) {
                    throw new InvalidPathException();
                }

                pi.setRealBasePath(gPath);
                pi.setVirtBasePath(virtRelPath.subpath(0, 2));

                if (virtRelPath.getNameCount() > 2) {
                    Path relPath = virtRelPath.subpath(2, virtRelPath.getNameCount());
                    Path targetPath = gPath.resolve(relPath);

                    if (!targetPath.startsWith(gPath)) {
                        throw new InvalidPathException();
                    }

                    pi.setRelPath(relPath);
                    pi.setRealPath(targetPath);
                } else {
                    pi.setRelPath(Paths.get(""));
                    pi.setRealPath(gPath);
                }

            }

        } else {
            throw new InvalidPathException();
        }

        return pi;

    }

    public PathTarget getTarget() {
        return target;
    }

    public void setTarget(PathTarget target) {
        this.target = target;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    public Path getRelPath() {
        return relPath;
    }

    public void setRelPath(Path relPath) {
        this.relPath = relPath;
    }

    public Path getRealPath() {
        return realPath;
    }

    public void setRealPath(Path realPath) {
        this.realPath = realPath;
    }

    public Path getRealBasePath() {
        return realBasePath;
    }

    public void setRealBasePath(Path realBasePath) {
        this.realBasePath = realBasePath;
    }

    public Path getVirtBasePath() {
        return virtBasePath;
    }

    public void setVirtBasePath(Path virtBasePath) {
        this.virtBasePath = virtBasePath;
    }

    public Collection<UserGroup> getGroups() {
        return groups;
    }

    public void setGroups(Collection<UserGroup> groups) {
        this.groups = groups;
    }

    public Path getVirtPath() {
        return virtPath;
    }

    public void setVirtPath(Path virtPath) {
        this.virtPath = virtPath;
    }

    public UserGroup getGroup() {
        return group;
    }

    public void setGroup(UserGroup group) {
        this.group = group;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(target.name()).append("\n");
        sb.append("Virtual path: ").append(virtPath).append("\n");
        sb.append("Relative path: ").append(relPath).append("\n");
        sb.append("Real path: ").append(realPath).append("\n");
        sb.append("Real base path: ").append(realBasePath).append("\n");
        sb.append("Virtual base path: ").append(virtBasePath).append("\n");
        return sb.toString();
    }

}