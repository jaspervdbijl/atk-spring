package com.acutus.atk.spring.thirdparty;

import com.acutus.atk.util.Assert;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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

    public Response sendEmailPlainTxt(String address, String subject, String txt, List<Attachments> attachments) {
        Personalization personalization = new Personalization();
        Arrays.stream(address.split("[,|;]")).forEach(a -> {
            personalization.addTo(new Email(a));
        });
        personalization.setSubject(subject);

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setSubject(subject);
        mail.addContent(new Content("text/plain", txt));

        mail.addPersonalization(personalization);
        attachments.stream().forEach(a -> mail.addAttachments(a));
        Response response = send(mail);
        Assert.isTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Mail failed with error " + response.getBody());
        return response;
    }

    public void postEmail(Boolean maySend, String templateId, List<Personalization> personalizationList) {
        Assert.notNull(templateId, "Template Id must be supplied");
        if (personalizationList.isEmpty() || !maySend) {
            return;
        }

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setTemplateId(templateId);
        mail.personalization = personalizationList;
        try {
            sendGridRestTemplate.postForEntity(sendGridUrl, new HttpEntity<>(mail, sendGridRestTemplateHeaders), String.class);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
        }
    }
}
