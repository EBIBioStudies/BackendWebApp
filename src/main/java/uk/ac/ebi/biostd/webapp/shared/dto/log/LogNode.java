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

package uk.ac.ebi.biostd.webapp.shared.dto.log;

import java.util.List;

public interface LogNode {

    void success();

    void log(Level lvl, String msg);

    LogNode branch(String msg);

    String getMessage();

    Level getLevel();

    void setLevel(Level lvl);

    List<? extends LogNode> getSubNodes();

    void append(LogNode rootNode);

    enum Level {
        ERROR(5),
        WARN(4),
        SUCCESS(3),
        INFO(2),
        DEBUG(1);

        private int level;

        Level(int l) {
            level = l;
        }

        public static Level getMinLevel() {
            return DEBUG;
        }

        public int getPriority() {
            return level;
        }
    }
}
