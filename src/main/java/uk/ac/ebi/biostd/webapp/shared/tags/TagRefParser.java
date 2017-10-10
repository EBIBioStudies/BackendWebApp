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

package uk.ac.ebi.biostd.webapp.shared.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.ebi.biostd.in.ParserException;


public class TagRefParser {

    public static List<TagRef> parseTags(String str) throws ParserException {

        str = str.trim();

        if (str.length() == 0) {
            return Collections.emptyList();
        }

        List<TagRef> res = new ArrayList<>();

        int bpos = 0;

        do {
            int pos = str.indexOf(':', bpos);

            if (pos < 0 || pos == bpos) {
                throw new ParserException(0, bpos, "Invalid string");
            }

            TagRef tr = new TagRef();
            res.add(tr);

            tr.setClassiferName(str.substring(bpos, pos).trim());

            pos++;

            int epos1 = str.indexOf(',', pos);
            int epos2 = str.indexOf('=', pos);

            if (epos1 < 0 && epos2 < 0) {
                tr.setTagName(str.substring(pos).trim());
                break;
            } else {
                if (epos1 < 0) {
                    tr.setTagName(str.substring(pos, epos2).trim());
                    tr.setTagValue(str.substring(epos2 + 1).trim());
                    break;
                } else {
                    if (epos2 >= 0 && epos2 < epos1) {
                        tr.setTagName(str.substring(pos, epos2).trim());
                        tr.setTagValue(str.substring(epos2 + 1, epos1).trim());
                    } else {
                        tr.setTagName(str.substring(pos, epos1).trim());
                    }

                    bpos = epos1 + 1;
                }
            }
        }
        while (bpos < str.length());

        return res;
    }
}
