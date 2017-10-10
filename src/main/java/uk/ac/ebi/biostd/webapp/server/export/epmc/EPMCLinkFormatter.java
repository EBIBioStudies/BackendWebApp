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

package uk.ac.ebi.biostd.webapp.server.export.epmc;

import static uk.ac.ebi.biostd.util.StringUtils.xmlEscaped;
import static uk.ac.ebi.biostd.webapp.server.export.epmc.EPMCLinkElements.ROOT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uk.ac.ebi.biostd.model.Section;
import uk.ac.ebi.biostd.model.Submission;
import uk.ac.ebi.biostd.out.TextStreamFormatter;

public class EPMCLinkFormatter implements TextStreamFormatter {

    protected static final String shiftSym1 = " ";
    protected static final String shiftSym2 = shiftSym1 + shiftSym1;
    protected static final String shiftSym3 = shiftSym2 + shiftSym1;
    public static String ResURLParam = "resourceURL";
    public static String ProviderIdParam = "providerId";
    public static String AccNoPlaceHolder = "{accNo}";
    public static String PUBLICATION_SEC = "Publication";
    public static String EPMC_ID_PFX = "PMC";
    private Map<String, String> config;
    private String resUrlPfx;
    private String resUrlSfx;
    private String providerId;

    public EPMCLinkFormatter(Map<String, String> cfg) {
        config = cfg;

        String resUrl = cfg.get(ResURLParam);

        if (resUrl == null) {
            throw new RuntimeException("Can't initialize EPMCLinkFormatter. Parameter '" + ResURLParam + "' missing");
        }

        int pos = resUrl.indexOf(AccNoPlaceHolder);

        if (pos < 0) {
            throw new RuntimeException(
                    "Can't initialize EPMCLinkFormatter. Parameter '" + ResURLParam + "' has invalid value. Missing "
                            + AccNoPlaceHolder + " placeholder");
        }

        resUrlPfx = resUrl.substring(0, pos);
        resUrlSfx = resUrl.substring(pos + AccNoPlaceHolder.length());

        providerId = cfg.get(ProviderIdParam);

        if (providerId == null) {
            throw new RuntimeException(
                    "Can't initialize EPMCLinkFormatter. Parameter '" + ProviderIdParam + "' missing");
        }
    }

    @Override
    public void header(Map<String, List<String>> hdrs, Appendable out) throws IOException {
        out.append("<").append(ROOT.getElementName()).append(">\n");
    }

    @Override
    public void footer(Appendable out) throws IOException {
        out.append("</").append(ROOT.getElementName()).append(">\n");
    }


    @Override
    public void format(Submission s, Appendable out) throws IOException {
        List<Section> pubs = collectPMCPublications(s.getRootSection(), null);

        if (pubs.size() == 0) {
            return;
        }

        for (Section pub : pubs) {
            out.append(shiftSym1).append('<').append(EPMCLinkElements.LINK.getElementName())
                    .append(" ").append(EPMCLinkElements.PROVIDER_ID_ATTR.getElementName()).append("=\"");
            xmlEscaped(providerId, out);
            out.append("\">\n");

            out.append(shiftSym2).append('<').append(EPMCLinkElements.RESOURCE.getElementName()).append(">\n");

            out.append(shiftSym3).append('<').append(EPMCLinkElements.URL.getElementName()).append(">");
            xmlEscaped(resUrlPfx, out);
            xmlEscaped(s.getAccNo(), out);
            xmlEscaped(resUrlSfx, out);
            out.append("</").append(EPMCLinkElements.URL.getElementName()).append(">\n");

            out.append(shiftSym3).append('<').append(EPMCLinkElements.TITLE.getElementName()).append(">");

            String title = Submission.getNodeTitle(s.getRootSection());

            if (title == null) {
                title = s.getTitle();
            }

            xmlEscaped(title, out);
            out.append("</").append(EPMCLinkElements.TITLE.getElementName()).append(">\n");

            out.append(shiftSym2).append("</").append(EPMCLinkElements.RESOURCE.getElementName()).append(">\n");

            out.append(shiftSym2).append('<').append(EPMCLinkElements.RECORD.getElementName()).append(">\n");

            out.append(shiftSym3).append('<').append(EPMCLinkElements.SOURCE.getElementName()).append('>');
            out.append("PMC");
            out.append("</").append(EPMCLinkElements.SOURCE.getElementName()).append(">\n");

            out.append(shiftSym3).append('<').append(EPMCLinkElements.ID.getElementName()).append('>');
            xmlEscaped(pub.getAccNo(), out);
            out.append("</").append(EPMCLinkElements.ID.getElementName()).append(">\n");

            out.append(shiftSym2).append("</").append(EPMCLinkElements.RECORD.getElementName()).append(">\n");

            out.append(shiftSym1).append("</").append(EPMCLinkElements.LINK.getElementName()).append(">\n");

        }

    }

    private List<Section> collectPMCPublications(Section s, List<Section> coll) {
        if (coll == null) {
            coll = new ArrayList<>();
        }

        if (s.getType().equals(PUBLICATION_SEC) && s.getAccNo() != null && s.getAccNo().startsWith(EPMC_ID_PFX)) {
            coll.add(s);
        }

        if (s.getSections() != null) {
            for (Section ss : s.getSections()) {
                collectPMCPublications(ss, coll);
            }
        }

        return coll;
    }

    @Override
    public void separator(Appendable out) throws IOException {
    }

    @Override
    public void comment(String comment, Appendable out) throws IOException {
        out.append("<!-- ");
        xmlEscaped(comment);
        out.append(" -->\n");
    }

}
