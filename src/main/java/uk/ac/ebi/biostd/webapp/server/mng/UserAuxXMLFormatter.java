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

package uk.ac.ebi.biostd.webapp.server.mng;

import com.pri.util.StringUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


public class UserAuxXMLFormatter {

    public static final String AuxTag = "aux";
    public static final String ParameterTag = "param";
    public static final String NameTag = "name";
    public static final String ValueTag = "value";

    public static String buildXML(List<String[]> aux) {
        StringBuilder sb = new StringBuilder();

        sb.append("<").append(AuxTag).append(">\n");
        try {

            for (String[] tpl : aux) {
                sb.append("<").append(ParameterTag).append(">\n");

                sb.append("<").append(NameTag).append(">");
                StringUtils.xmlEscaped(tpl[0], sb);
                sb.append("</").append(NameTag).append(">\n");

                sb.append("<").append(ValueTag).append(">");
                if (tpl[1] != null) {
                    StringUtils.xmlEscaped(tpl[1], sb);
                }
                sb.append("</").append(ValueTag).append(">\n");

                sb.append("</").append(ParameterTag).append(">\n");
            }

        } catch (IOException e) //will never happen
        {
        }

        sb.append("</").append(AuxTag).append(">\n");

        return sb.toString();
    }

    public static List<String[]> readXML(String xml) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false);

        List<String[]> res = new ArrayList<>();

        SAXParser saxParser;
        try {
            saxParser = spf.newSAXParser();

            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(new SAXHandler(res));

            xmlReader.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
            res.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;

    }

    private static class SAXHandler extends DefaultHandler {

        private CEl cElem = CEl.OTHER;

        private String[] tuple;
        private StringBuilder sb = new StringBuilder();
        private List<String[]> collector;

        public SAXHandler(List<String[]> coll) {
            collector = coll;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (ParameterTag.equals(qName)) {
                tuple = new String[2];
            } else if (NameTag.equals(qName)) {
                cElem = CEl.NAME;
                sb.setLength(0);
            } else if (ValueTag.equals(qName)) {
                cElem = CEl.VAL;
                sb.setLength(0);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (ParameterTag.equals(qName)) {
                collector.add(tuple);
                tuple = null;
            } else if (NameTag.equals(qName)) {
                cElem = CEl.OTHER;
                tuple[0] = sb.toString();
            } else if (ValueTag.equals(qName)) {
                cElem = CEl.OTHER;
                tuple[1] = sb.toString();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (cElem != CEl.OTHER) {
                sb.append(ch, start, length);
            }
        }

        enum CEl {
            NAME,
            VAL,
            OTHER
        }

    }
}
