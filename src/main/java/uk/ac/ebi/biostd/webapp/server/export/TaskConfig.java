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

import java.util.HashMap;
import java.util.Map;

public class TaskConfig {

    public static final String LimitParameter = "limit";
    public static final String SliceParameter = "slice";
    public static final String TTLParameter = "threadTTL";
    public static final String ThreadsParameter = "threads";
    public static final String SinceParameter = "since";
    public static final String TaskInvokeTimeParameter = "invokeTime";


    private final String taskName;

    private final Map<String, Map<String, String>> outputParameters = new HashMap<>();

    private Long limit;
    private Integer slice;
    private Integer threadTTL;
    private Long since;
    private Integer threads;
    private int hour = -1;
    private int min = 0;
    private int period = 24 * 60;


    public TaskConfig(String nm) {
        taskName = nm;
    }

    public int getPeriodHours() {
        return period;
    }

    public void setPeriodHours(int period) {
        this.period = period;
    }

    public void addOutputParameter(String mod, String nm, String val) {
        Map<String, String> mp = outputParameters.get(mod);

        if (mp == null) {
            outputParameters.put(mod, mp = new HashMap<>());
        }

        mp.put(nm, val);
    }

    public Map<String, Map<String, String>> getOutputModulesConfig() {
        return outputParameters;
    }

    public boolean readParameter(String pName, String pVal) throws TaskConfigException {
        if (LimitParameter.equals(pName)) {
            try {
                limit = Long.parseLong(pVal);
            } catch (Exception e) {
                throw new TaskConfigException("Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal);
            }
        } else if (SliceParameter.equals(pName)) {
            try {
                slice = Integer.parseInt(pVal);
            } catch (Exception e) {
                throw new TaskConfigException("Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal);
            }
        } else if (TTLParameter.equals(pName)) {
            try {
                threadTTL = Integer.parseInt(pVal);
            } catch (Exception e) {
                throw new TaskConfigException("Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal);
            }
        } else if (SinceParameter.equals(pName)) {
            try {
                since = Long.parseLong(pVal);
            } catch (Exception e) {
                throw new TaskConfigException("Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal);
            }
        } else if (ThreadsParameter.equals(pName)) {
            try {
                threads = Integer.parseInt(pVal);
            } catch (Exception e) {
                throw new TaskConfigException("Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal);
            }
        } else if (TaskInvokeTimeParameter.equals(pName)) {
            String tmStr = pVal;

            int pos = tmStr.lastIndexOf('/');

            String prdStr = null;

            if (pos >= 0) {
                prdStr = tmStr.substring(pos + 1).trim();
                tmStr = tmStr.substring(0, pos).trim();
            }

            if (!tmStr.equals("*")) {
                int colPos = tmStr.indexOf(':');

                String hourStr = tmStr;
                String minStr = null;

                if (colPos >= 0) {
                    hourStr = tmStr.substring(0, colPos);
                    minStr = tmStr.substring(colPos + 1);
                }

                try {
                    hour = Integer.parseInt(hourStr);
                } catch (Exception e) {
                    throw new TaskConfigException(
                            "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                    + " Hours must be a number");
                }

                if (hour < 0 || hour > 23) {
                    throw new TaskConfigException(
                            "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                    + " Hours must be a number between 0 and 23");
                }

                if (minStr != null) {
                    try {
                        min = Integer.parseInt(minStr);
                    } catch (Exception e) {
                        throw new TaskConfigException(
                                "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                        + " Minutes must be a number");
                    }

                    if (min < 0 || min > 59) {
                        throw new TaskConfigException(
                                "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                        + " Minutes must be a number between 0 and 59 ");
                    }
                }
            } else {
                min = -1;
            }

            if (prdStr != null && prdStr.length() > 0) {
                int mult = 1;

                char lastChar = prdStr.charAt(prdStr.length() - 1);

                if (!Character.isDigit(lastChar)) {
                    if (lastChar == 'm' || lastChar == 'M') {
                        mult = 1;
                    } else if (lastChar == 'h' || lastChar == 'H') {
                        mult = 60;
                    } else if (lastChar == 'd' || lastChar == 'D') {
                        mult = 60 * 24;
                    } else {
                        throw new TaskConfigException(
                                "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                        + " Period can ends with 'm','h' or 'd'");
                    }

                    prdStr = prdStr.substring(0, prdStr.length() - 1);
                }

                try {
                    period = Integer.parseInt(prdStr);
                    period *= mult;
                } catch (Exception e) {
                    throw new TaskConfigException(
                            "Task '" + taskName + "' Invalid parameter value: " + pName + "=" + pVal
                                    + " Period must be number (can ends with 'm','h' or 'd'");
                }

            }


        } else {
            return false;
        }

        return true;
    }


    public int getThreads(int def) {
        return threads != null ? threads : def;
    }

    public long getSince(long def) {
        return since != null ? since : def;
    }

    public long getLimit(long def) {
        return limit != null ? limit : def;
    }

    public int getSliceSize(int def) {
        return slice != null ? slice : def;
    }


    public String getName() {
        return taskName;
    }

    public int getInvokeHour() {
        return hour;
    }

    public int getInvokeMin() {
        return min;
    }

    public int getInvokePeriodMins() {
        return period;
    }

    public int getThreadTTL(int def) {
        return threadTTL != null ? threadTTL : def;
    }

}
