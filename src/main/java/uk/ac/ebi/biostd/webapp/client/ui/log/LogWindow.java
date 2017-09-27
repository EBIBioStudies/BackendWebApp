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


import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import uk.ac.ebi.biostd.treelog.LogNode;

public class LogWindow extends Window {

    public LogWindow(String title, LogNode rLn) {
        super();

        setWidth(1000);
        setHeight(600);
        centerInPage();
        setCanDragReposition(true);
        setCanDragResize(true);
        setKeepInParentRect(true);
        setTitle(title);

        final LogTree lgTree = new LogTree(rLn);

        addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClickEvent event) {
                destroy();
            }
        });

        addItem(lgTree);

    }
}
