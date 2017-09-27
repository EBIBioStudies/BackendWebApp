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
import uk.ac.ebi.biostd.webapp.server.export.ControlMessage.Type;


public class OutputTask implements Runnable {

    private final Appendable out;
    private final BlockingQueue<Object> inQueue;
    private final BlockingQueue<ControlMessage> controlQueue;
    private final String name;
    private String separator;
    private int outCount = 0;

    public OutputTask(String name, Appendable out, String sep, BlockingQueue<Object> inQueue,
            BlockingQueue<ControlMessage> controlQueue) {
        this.out = out;
        this.inQueue = inQueue;
        this.controlQueue = controlQueue;
        this.name = name;
        separator = sep;
    }


    public String getName() {
        return name;
    }

    @Override
    public void run() {

        Thread.currentThread().setName(name);

        boolean terminate = false;

        while (true) {
            Object o = null;

            while (true) {
                try {
                    o = inQueue.take();
                    break;
                } catch (InterruptedException e) {
                }
            }

            String str = o.toString();

            if (str == null) {
                terminate = true;
            }

            if (terminate && inQueue.size() == 0) {
                putIntoQueue(new ControlMessage(Type.OUTPUT_FINISH, this));
                return;
            }

            if (str == null) {
                continue;
            }

            try {
                if (separator != null && outCount > 0) {
                    out.append(separator);
                }

                out.append(str);
                outCount++;
            } catch (Exception e) {
                e.printStackTrace();

                putIntoQueue(new ControlMessage(Type.OUTPUT_ERROR, this, e));
                return;
            }

        }

    }

    public BlockingQueue<Object> getIncomingQueue() {
        return inQueue;
    }

    void putIntoQueue(ControlMessage o) {

        while (true) {
            try {
                controlQueue.put(o);
                return;
            } catch (InterruptedException e) {
            }
        }

    }


    public int getOutCount() {
        return outCount;
    }

}
