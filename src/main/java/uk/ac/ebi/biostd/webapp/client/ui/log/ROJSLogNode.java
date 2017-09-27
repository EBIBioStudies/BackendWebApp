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

package uk.ac.ebi.biostd.webapp.client.ui.log;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import java.util.List;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.webapp.client.ui.util.ListOnJsArray;

public final class ROJSLogNode extends JavaScriptObject implements LogNode {

    protected ROJSLogNode() {
        super();
    }

    ;

    public static ROJSLogNode convert(String txt) {
        return JsonUtils.safeEval(txt);
    }

    public native String getLevelAsString() /*-{
      return this.level;
    }-*/;

    public native String setLevelAsString(String lvl) /*-{
      this.level = lvl;
    }-*/;

    @Override
    public native String getMessage() /*-{
      return this.message;
    }-*/;

// public static native ROJSLogNode convert( String txt ) /*-{ return eval(txt); }-*/ ;

    public native JsArray<ROJSLogNode> getSubnodes() /*-{
      return this.subnodes;
    }-*/;

    @Override
    public void log(Level lvl, String msg) {
    }

    @Override
    public LogNode branch(String msg) {
        return null;
    }

    @Override
    public void success() {
    }

    @Override
    public List<? extends LogNode> getSubNodes() {
        JsArray<ROJSLogNode> snds = getSubnodes();

        if (snds == null) {
            return null;
        }

        return new ListOnJsArray<ROJSLogNode>(snds);
    }

    @Override
    public void append(LogNode rootNode) {
    }

    @Override
    public Level getLevel() {
        return Level.valueOf(getLevelAsString());
    }

    @Override
    public void setLevel(Level lvl) {
    }

    @Override
    public boolean remove(LogNode ln) {
        return false;
    }

    @Override
    public boolean move(LogNode oldPar, LogNode newPar) {
        return false;
    }

}
