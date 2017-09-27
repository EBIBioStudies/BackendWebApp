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

package uk.ac.ebi.biostd.webapp.server.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import uk.ac.ebi.biostd.webapp.server.util.ParamPool;

public class EmailService {

    public static final String SMTPHostParam = "SMTPHost";
    public static final String recipientParam = "to";
    public static final String errorRecipientParam = "errorsTo";
    public static final String fromParam = "from";

    private static EmailService defaultInstance;
    private final InternetAddress fromAddr;
    private final Properties properties;
    private InternetAddress toAddr;
    private InternetAddress errorsToAddr;

    public EmailService(ParamPool prms, String pfx) throws EmailInitException {
        String str = prms.getParameter(pfx + SMTPHostParam);

        if (str == null) {
            throw new EmailInitException("Parameter " + pfx + SMTPHostParam + " is not defined");
        }

        properties = new Properties();

        properties.setProperty("mail.smtp.host", str);

        str = prms.getParameter(pfx + fromParam);

        if (str == null) {
            throw new EmailInitException("Parameter " + pfx + fromParam + " is not defined");
        }

        try {
            fromAddr = new InternetAddress(str);
        } catch (AddressException e) {
            throw new EmailInitException("Invalid 'From' address: " + str);
        }

        str = prms.getParameter(pfx + recipientParam);

        if (str != null) {
            try {
                toAddr = new InternetAddress(str);
            } catch (AddressException e) {
                throw new EmailInitException("Invalid 'To' address: " + str);
            }
        }

        str = prms.getParameter(pfx + errorRecipientParam);

        if (str == null) {
            errorsToAddr = toAddr;
        } else {
            try {
                errorsToAddr = new InternetAddress(str);
            } catch (AddressException e) {
                throw new EmailInitException(
                        "Invalid 'To' address for error messages (" + pfx + errorRecipientParam + "): " + str);
            }
        }

        if (errorsToAddr == null && toAddr == null) {
            throw new EmailInitException(
                    "Parameter " + pfx + recipientParam + " or " + pfx + errorRecipientParam + " should be defined");
        }

    }

    public static EmailService getDefaultInstance() {
        return defaultInstance;
    }

    public static void setDefaultInstance(EmailService defaultInstance) {
        EmailService.defaultInstance = defaultInstance;
    }

    public boolean sendErrorAnnouncement(String subj, String msg, Throwable t) {

        if (errorsToAddr == null) {
            return false;
        }

        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);

            message.setFrom(fromAddr);
            message.addHeader("X-Priority", "1 (Highest)");
            message.addHeader("X-MSMail-Priority", "High");
            message.addHeader("Importance", "High");

            message.addRecipient(Message.RecipientType.TO, errorsToAddr);

            if (subj == null) {
                subj = "BioStd error message";
            }

            message.setSubject(subj);

            StringBuffer buf = new StringBuffer();

            buf.append("BioStd has encountered some problem:\n" + msg + "\n");

            if (t != null) {

                StringWriter stkOut = new StringWriter();

                t.printStackTrace(new PrintWriter(stkOut));

                buf.append("\n\n" + stkOut.toString());
            }

            message.setText(buf.toString());

            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean sendAnnouncement(String subj, String msg) {

        if (toAddr == null) {
            return false;
        }

        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);

            message.setFrom(fromAddr);

            message.addRecipient(Message.RecipientType.TO, toAddr);

            if (subj == null) {
                subj = "BioStd info message";
            }

            message.setSubject(subj);

            message.setText(msg);

            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean sendMultipartEmail(String toAddr, String subj, String textBody, String htmlBody) {
        Session session = Session.getDefaultInstance(properties);

        Message message = new MimeMessage(session);
        Multipart multiPart = new MimeMultipart("alternative");

        try {
            if (textBody != null) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(textBody, "utf-8");

                multiPart.addBodyPart(textPart);
            }

            if (htmlBody != null) {
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
                multiPart.addBodyPart(htmlPart);
            }

            message.setContent(multiPart);

            if (fromAddr != null) {
                message.setFrom(fromAddr);
            } else {
                message.setFrom();
            }

            InternetAddress[] toAddresses = {new InternetAddress(toAddr)};
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            message.setSubject(subj);
            message.setSentDate(new Date());

            Transport.send(message);

        } catch (AddressException e) {
            e.printStackTrace();
            return false;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
