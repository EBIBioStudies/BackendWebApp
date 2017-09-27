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

package uk.ac.ebi.biostd.webapp.server.util;

import java.nio.file.Path;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;
import uk.ac.ebi.biostd.webapp.server.vfs.PathInfo;


public class FileNameUtil {

    public static String toUnixPath(Path p) {
        StringBuilder sb = new StringBuilder();

        if (p.getRoot() != null) {
            sb.append('/');
        }

        for (int i = 0; i < p.getNameCount(); i++) {
            if (i != 0) {
                sb.append('/');
            }

            sb.append(p.getName(i));
        }

        return sb.toString();
    }


    public static String encode(String src) {
        if (BackendConfig.isEncodeFileNames()) {
            return encodeString(src);
        }

        return src;
    }

    public static String decode(String src) {
        if (BackendConfig.isEncodeFileNames()) {
            return decodeString(src);
        }

        return src;
    }


    public static boolean checkUnicodeFN(String fn) {

        for (int i = 0; i < fn.length(); i++) {
            char ch = fn.charAt(i);

            if (!(Character.isLetterOrDigit(ch) || (ch < 127 && !Character.isISOControl(ch) && ch != '\t'
                    && ch != '\\'))) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkCharClass(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '.'
                || ch == '-' || ch == '_' || ch == ' ';
    }

    public static boolean encodeOrCheck(PathInfo pi) {
        Path rl = pi.getRelPath();
        int len = rl.getNameCount();

        if (BackendConfig.isEncodeFileNames()) {
            Path erl = null;

            for (int i = 0; i < len; i++) {
                String nm = rl.getName(i).toString();
                String enm = encodeString(nm);

                if (nm != enm && erl == null) {
                    erl = rl.subpath(0, i);
                }

                if (erl != null) {
                    erl = erl.resolve(enm);
                }
            }

            if (erl != null) {
                pi.setRelPath(erl);
                pi.setRealPath(pi.getRealBasePath().resolve(pi.getRelPath()));
            }
        } else {
            for (int i = 0; i < len; i++) {
                String nm = rl.getName(i).toString();

                if (!checkUnicodeFN(nm)) {
                    return false;
                }
            }
        }

        return true;
    }


    public static String encodeString(String src) {
        StringBuilder sb = null;

        int len = src.length();

        for (int i = 0; i < len; i++) {
            char ch = src.charAt(i);

            if (checkCharClass(ch)) {
                if (sb != null) {
                    sb.append(ch);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(len * 4);

                    if (i > 0) {
                        sb.append(src.substring(0, i));
                    }
                }

                sb.append('!');
                sb.append(convBits((ch >> 12) & 0x3F));
                sb.append(convBits((ch >> 6) & 0x3F));
                sb.append(convBits(ch & 0x3F));
            }
        }

        if (sb != null) {
            return sb.toString();
        }

        return src;
    }

    public static String decodeString(String s) {
        StringBuilder sb = null;

        int pos = 0;
        int len = s.length();

        while (pos < len) {
            int epos = s.indexOf('!', pos);

            if (epos < 0) {
                if (sb == null) {
                    return s;
                } else {
                    sb.append(s.substring(pos));
                    return sb.toString();
                }
            } else {
                if (epos + 4 > len) {
                    return s;
                }

                int acc = 0;

                int pt = convChar(s.charAt(epos + 1));

                if (pt < 0) {
                    return s;
                }

                acc += pt << 12;

                pt = convChar(s.charAt(epos + 2));

                if (pt < 0) {
                    return s;
                }

                acc += pt << 6;

                pt = convChar(s.charAt(epos + 3));

                if (pt < 0) {
                    return s;
                }

                acc += pt;

                if (sb == null) {
                    sb = new StringBuilder();
                }

                sb.append(s.substring(pos, epos));
                sb.append((char) acc);

                pos = epos + 4;
            }

        }

        if (sb != null) {
            return sb.toString();
        }

        return s;
    }

    private static int convChar(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }

        if (ch >= 'a' && ch <= 'z') {
            return ch - 'a' + 10;
        }

        if (ch >= 'A' && ch <= 'Z') {
            return ch - 'A' + 36;
        }

        if (ch == '~') {
            return 62;
        }

        if (ch == '!') {
            return 63;
        }

        return -1;
    }

    private static char convBits(int bits) {
        if (bits < 10) {
            return (char) ('0' + bits);
        }

        bits -= 10;

        if (bits < 26) {
            return (char) ('a' + bits);
        }

        bits -= 26;

        if (bits < 26) {
            return (char) ('A' + bits);
        }

        bits -= 26;

        if (bits == 0) {
            return '~';
        }

        return '!';
    }


}
