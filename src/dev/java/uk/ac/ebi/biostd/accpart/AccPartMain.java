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

package uk.ac.ebi.biostd.accpart;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import uk.ac.ebi.biostd.webapp.server.util.AccNoUtil;

public class AccPartMain {

    public static void main(String[] args) {
        String acc;

        acc = "ABC";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "ABC1";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "ABC01";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "ABC1234";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "ABC1Sfx";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "ABC1234Sfx";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "1234";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        acc = "АБВ1234";
        System.out.println(acc + " -> " + join(AccNoUtil.partition(acc)));

        Path pth = FileSystems.getDefault().getPath("c:/dev");

        System.out.println(pth.resolve(AccNoUtil.getPartitionedPath("ABC1234Sfx")));

    }

    static String join(String[] parts) {
        StringBuilder sb = new StringBuilder();

        for (String s : parts) {
            sb.append(s).append('/');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

}
