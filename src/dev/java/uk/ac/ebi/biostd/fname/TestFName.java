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

package uk.ac.ebi.biostd.fname;

import uk.ac.ebi.biostd.webapp.server.util.FileNameUtil;

public class TestFName {

    public static void main(String[] args) {
        System.out.println(FileNameUtil.encode("aaa"));
        System.out.println(FileNameUtil.encode("abc!cba"));
        System.out.println(FileNameUtil.encode("Привет"));

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 256; i++) {
            sb.append((char) i);
        }

        System.out.println(FileNameUtil.encode(sb.toString()));

        System.out.println(FileNameUtil.decode("abc!00x"));

        System.out.println(FileNameUtil.decode(FileNameUtil.encode("!aa!!a!")));
        System.out.println(FileNameUtil.decode(FileNameUtil.encode("abc!cba")));
        System.out.println(FileNameUtil.decode(FileNameUtil.encode("Привет")));


    }
}
