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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import uk.ac.ebi.biostd.out.TextStreamFormatter;

public class FormattingModule {

    private final TextStreamFormatter formatter;
    private final BlockingQueue<Object> outQueue;

    private final AtomicLong maxCount;

    public FormattingModule(TextStreamFormatter formatter, BlockingQueue<Object> queue, long limit) {
        super();
        this.formatter = formatter;

        outQueue = queue;

        if (limit > 0) {
            maxCount = new AtomicLong(limit);
        } else {
            maxCount = null;
        }
    }


    public boolean confirmOutput() {
        if (maxCount == null) {
            return true;
        }

        long cnt = maxCount.decrementAndGet();

        return cnt >= 0;
    }

    public TextStreamFormatter getFormatter() {
        return formatter;
    }

    public BlockingQueue<Object> getOutQueue() {
        return outQueue;
    }

}
