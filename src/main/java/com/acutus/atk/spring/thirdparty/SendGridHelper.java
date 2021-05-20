package com.acutus.atk.spring.thirdparty;

import com.acutus.atk.spring.util.properties.FileResource;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

import static com.acutus.atk.util.StringUtils.isNotEmpty;

@Component
@Slf4j
public class SendGridHelper {

    @Autowired
    DataSource dataSource;

    @Value("${receipt.mail.from:support@kwebo.co.za}")
    private String fromAddress;

    @Value("${receipt.mail.receipt.subject:Kwebo receipt for job }")
    private String receiptSubject;

    @Value("${receipt.mail.eft_quote.subject:Kwebo EFT Banking details - }")
    private String eftQuoteSubject;

    @Value("${sendgrid.key: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}")
    String key;

    @FileResource("templates/mail/receiptCompressed.html")
    private String receiptHtml;

    @FileResource("templates/mail/receiptLine.html")
    private String receiptLineHtml;

    @FileResource("templates/mail/bankingDetailCompressed.html")
    private String bankingDetailHtml;

    @Value("${receipt.mail.noLogo::https://prod0storage0pub.blob.core.windows.net/images/icon-90x90.png}")
    private String noLogoPath;

    @Autowired
    @Qualifier("sendGridRestTemplate")
    private RestTemplate sendGridRestTemplate;

    @Autowired
    @Qualifier("sendGridRestTemplateHeaders")
    private HttpHeaders sendGridRestTemplateHeaders;

    @Value("${receipt.mail.sendgrid.url:https://api.sendgrid.com/v3/mail/send}")
    private String sendGridUrl;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getLines(List<String> invLines, List<Double> lineTotals, List<Integer> lineQtys) {
        return IntStream.range(0, invLines.size())
                .mapToObj((i) ->
                        receiptLineHtml.replace("{{Order_linedesc}}", invLines.get(i))
                                .replace("{{Order_linetotal}}", String.format("%.2f", (lineTotals.get(i) / (lineQtys.get(i) * 1.0))))
                                .replace("{{lineNo}}", "" + (lineQtys.get(i)))).
                        reduce((s1, s2) -> s1 + "\n" + s2).get();
    }

    @SneakyThrows
    private Response send(Mail mail) {
        SendGrid sg = new SendGrid(key);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response rs = sg.api(request);
        return rs;
    }

    public Response sendEmail(String address, String fromAddress, String subject, String html) {
        log.info("MAIL {} {} {}", address, fromAddress, subject);
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
    public void sendReceipt(String jobNo, String toAddress, String customerName, String businessName, double total,
                            List<String> invLines, List<Double> lineTotals, List<Integer> lineQtys,
                            double subTotal, double serviceFee, String cardLastNo, double lat, double lon, String address,
                            String firstName, String thumbnailUri) {

        Email from = new Email(fromAddress);

        Content content = new Content("text/html", receiptHtml
                .replace("{{Total}}", String.format("R%.2f", total))
                .replace("{{Cust_name}}", customerName)
                .replace("{{Date}}", LocalDate.now().format(formatter))
                .replace("{{Business_Name}}", businessName)
                .replace("{{order_lines}}", getLines(invLines, lineTotals, lineQtys))
                .replace("{{Sub_total}}", String.format("R%.2f", subTotal))
                .replace("{{Service_fee}}", String.format("R%.2f", serviceFee))
                .replace("{{Total}}", String.format("R%.2f", total))
                .replace("{{Card_last_four}}", cardLastNo != null ? cardLastNo : "N/A")
                .replace("{{AMT_charged}}", String.format("R%.2f", total))
                .replace("{{lat}}", "" + lat).replace("{{lon}}", "" + lon)
                .replace("{{address}}", "" + address)
                .replace("{{address}}", "" + address)
                .replace("{{Name}}", "" + firstName)
                .replace("{{thumbnail}}", thumbnailUri == null ? noLogoPath : thumbnailUri));
        Mail mail = new Mail(from, receiptSubject + jobNo, new Email(toAddress), content);
        send(mail);
    }

    public void sendBankingDetail(String address, String vendor, String quoteNo, double total) {
        if (isNotEmpty(address)) {
            Content content = new Content("text/html", bankingDetailHtml
                    .replace("{{QUOTE_TOTAL}}", String.format("R%.2f", total))
                    .replace("{{EFT_REF}}", quoteNo)
                    .replace("{{SERVICE_PROVIDER}}", vendor));
            Mail mail = new Mail(new Email(fromAddress), eftQuoteSubject + quoteNo, new Email(address), content);
            send(mail);
        }
    }

    public void postEmail(String templateId, List<Personalization> personalizationList) {
        Assert.notNull(templateId, "Template Id must be supplied");
        if (personalizationList.isEmpty()) {
            return;
        }

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setTemplateId(templateId);
        mail.personalization = personalizationList;
        try {
            sendGridRestTemplate.postForEntity(sendGridUrl, new HttpEntity<>(mail, sendGridRestTemplateHeaders), String.class);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception.getMessage());
        }
    }

    public void postEmailContent(String templateId, List<Content> contentList) {
        if (contentList.isEmpty()) {
            return;
        }

        Mail mail = new Mail();
        mail.setFrom(new Email(fromAddress));
        mail.setTemplateId(templateId);
        mail.content = contentList;
        try {
            sendGridRestTemplate.postForEntity(sendGridUrl, new HttpEntity<>(mail, sendGridRestTemplateHeaders),
                    String.class);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception.getMessage());
        }
    }
}
