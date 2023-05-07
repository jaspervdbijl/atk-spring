package com.acutus.atk.spring.thirdparty;

import com.acutus.atk.util.Assert;
import com.acutus.atk.util.IOUtil;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class SendGridHelper {

    @Autowired
    protected DataSource dataSource;

    @Value("${receipt.mail.from:noreply@cellstop.org}")
    protected String fromAddress;

    @Value("${sendgrid.key}")
    protected String key;

    @Autowired
    @Qualifier("sendGridRestTemplate")
    protected RestTemplate sendGridRestTemplate;

    @Autowired
    @Qualifier("sendGridRestTemplateHeaders")
    protected HttpHeaders sendGridRestTemplateHeaders;

    @Value("${receipt.mail.sendgrid.url:https://api.sendgrid.com/v3/mail/send}")
    protected String sendGridUrl;

    protected DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @SneakyThrows
    protected Response send(Mail mail) {
        SendGrid sg = new SendGrid(key);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response rs = sg.api(request);
        return rs;
    }

    public Response sendEmail(String address, String fromAddress, String subject, String html) {
        log.debug("MAIL {} {} {}", address, fromAddress, subject);
        Mail mail = new Mail(new Email(fromAddress), subject, new Email(address), new Content("text/html", html));
        return send(mail);
    }

    public Response sendEmail(String address, String subject, String html) {
        return sendEmail(address, fromAddress, subject, html);
    }

    public Response sendEmail(String address, String subject, String html, Attachments attachments) {
        Mail mail = new Mail(new Email(fromAddress), subject, new Email(address), new Content("text/html", html));
        mail.addAttachments(attachments);
        Response response = send(mail);
        Assert.isTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Mail failed with error " + response.getBody());
        return response;
    }

    @SneakyThrows
    public Attachments toAttachments(File csvFile, String filename) {
        Attachments attachments = new Attachments();
        attachments.setContent(Base64.getEncoder().encodeToString(IOUtil.readAvailable(new FileInputStream(csvFile))));
        attachments.setType("text/csv");
        attachments.setFilename(filename);
        attachments.setDisposition("attachment");
        attachments.setContentId("Reports");
        return attachments;
    }

    public Response sendEmailPlainTxt(String address, String subject, String txt, List<Attachments> attachments) {
        Personalization personalization = new Personalization();
        Arrays.stream(address.split("[,|;]")).forEach(a -> personalization.addTo(new Email(a)));
        personalization.setSubject(subject);

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setSubject(subject);
        mail.addContent(new Content("text/plain", txt));
        mail.addPersonalization(personalization);

        if (!CollectionUtils.isEmpty(attachments)) {
            attachments.stream().forEach(a -> mail.addAttachments(a));
        }

        Response response = send(mail);
        Assert.isTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Mail failed with error " + response.getBody());
        return response;
    }

    public ResponseEntity<String> sendTemplate(String templateId, Integer asmGroupId, List<Personalization> personalizationList, List<Attachments> attachments) {

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setTemplateId(templateId);
        ASM asm = new ASM();
        asm.setGroupId(asmGroupId);
        asm.setGroupsToDisplay(new int[]{asmGroupId});
        mail.setASM(asm);
        mail.personalization = personalizationList;

        if (!CollectionUtils.isEmpty(attachments)) {
            attachments.stream().forEach(a -> mail.addAttachments(a));
        }

        return sendGridRestTemplate.postForEntity(sendGridUrl, new HttpEntity<>(mail, sendGridRestTemplateHeaders), String.class);
    }

    public ResponseEntity<String> sendTemplate(String templateId, Integer asmGroupId, List<String> address, String subject, List<Attachments> attachments) {
        Personalization personalization = new Personalization();
        address.stream().forEach(a -> personalization.addTo(new Email(a)));
        personalization.setSubject(subject);
        return sendTemplate(templateId, asmGroupId,Arrays.asList(personalization),attachments);
    }
}
