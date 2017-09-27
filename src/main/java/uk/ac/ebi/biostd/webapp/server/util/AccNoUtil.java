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

public class AccNoUtil {

    private static boolean checkCharClass(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '.'
                || ch == '-' || ch == '@' || ch == '_';
    }

    public static String encode(String src) {
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

                sb.append('#').append(Integer.toHexString(ch)).append('#');
            }
        }

        if (sb != null) {
            return sb.toString();
        }

        return src;
    }

    public static boolean checkAccNoStr(String src) {
        int len = src.length();

        for (int i = 0; i < len; i++) {
            char ch = src.charAt(i);

            if (!checkCharClass(ch)) {
                return false;
            }
        }

        return true;
    }

    public static String getPartitionedPath(String acc) {
        StringBuilder sb = new StringBuilder();

        for (String pt : partition(acc)) {
            sb.append(pt).append('/');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    public static String[] partition(String acc) {
//  if( acc.startsWith("S-") )
//   acc = acc.substring(2);

        int len = acc.length();

        int nBegPos = -1, nEndPos = -1;

        for (int i = 0; i < len; i++) {
            char ch = acc.charAt(i);

            if (Character.isDigit(ch)) {
                if (nBegPos < 0) {
                    nBegPos = i;
                }
            } else if (nBegPos >= 0) {
                nEndPos = i;
                break;
            }

        }

        if (nBegPos >= 0 && nEndPos < 0) {
            nEndPos = len;
        }

        if (nBegPos < 0) {
            return new String[]{acc};
        }

        String part = null;

        if (nEndPos - nBegPos < 3) {
            part = "0-99";
        } else {
            part = "xxx" + acc.substring(nEndPos - 3, nEndPos);
        }

        String pfx = nBegPos == 0 ? "" : encode(acc.substring(0, nBegPos));

        String[] res = new String[nBegPos == 0 ? 2 : 3];

        String sfx = nEndPos < len ? encode(acc.substring(nEndPos)) : "";

        int n = 0;

        if (nBegPos > 0) {
            res[n++] = pfx;
        }

        res[n++] = pfx + part + sfx;

        res[n] = encode(acc);

        return res;
    }
}
