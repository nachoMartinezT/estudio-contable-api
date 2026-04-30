package com.guidapixel.contable.afip.service;

import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AfipAuthService {

    @Value("${afip.cert.path}")
    private String p12Path;

    @Value("${afip.cert.password}")
    private String p12Password;

    @Value("${afip.wsaa.url}")
    private String wsaaUrl;

    private Map<String, String> currentCredentials = null;
    private long tokenExpirationTime = 0;

    public Map<String, String> getAfipToken() throws Exception {
        if (currentCredentials != null && System.currentTimeMillis() < tokenExpirationTime) {
            return currentCredentials;
        }

        byte[] loginTicketRequest = createLoginTicketRequest();
        byte[] signedCms = signRequest(loginTicketRequest);
        String responseXml = invokeWsaa(signedCms);

        currentCredentials = parseResponse(responseXml);
        tokenExpirationTime = System.currentTimeMillis() + (10 * 60 * 60 * 1000);

        return currentCredentials;
    }

    private byte[] createLoginTicketRequest() {
        String uniqueId = String.valueOf(System.currentTimeMillis() / 1000);
        OffsetDateTime now = OffsetDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        String generationTime = now.minusMinutes(10).format(formatter);
        String expirationTime = now.plusMinutes(10).format(formatter);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<uniqueId>" + uniqueId + "</uniqueId>" +
                "<generationTime>" + generationTime + "</generationTime>" +
                "<expirationTime>" + expirationTime + "</expirationTime>" +
                "</header>" +
                "<service>wsfe</service>" +
                "</loginTicketRequest>";

        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] signRequest(byte[] xmlBytes) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            ks.load(fis, p12Password.toCharArray());
        }

        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, p12Password.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert);
        org.bouncycastle.cert.jcajce.JcaCertStore certs = new org.bouncycastle.cert.jcajce.JcaCertStore(certList);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey);

        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(sha1Signer, cert));
        gen.addCertificates(certs);

        CMSTypedData msg = new CMSProcessableByteArray(xmlBytes);
        CMSSignedData signedData = gen.generate(msg, true);

        return signedData.getEncoded();
    }

    private String invokeWsaa(byte[] signedCms) throws Exception {
        String cmsBase64 = Base64.getEncoder().encodeToString(signedCms);

        String soapXml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://wsaa.view.sua.dirstra.afip.gov.ar/LoginCms\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ser:loginCms>" +
                "<in0>" + cmsBase64 + "</in0>" +
                "</ser:loginCms>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsaaUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error conectando a AFIP (WSAA): " + response.statusCode());
        }
        return response.body();
    }

    private Map<String, String> parseResponse(String xml) {
        String cleanXml = xml
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");

        Map<String, String> creds = new HashMap<>();

        Matcher mToken = Pattern.compile("(?s)<token>(.*?)</token>").matcher(cleanXml);
        Matcher mSign = Pattern.compile("(?s)<sign>(.*?)</sign>").matcher(cleanXml);

        if (mToken.find() && mSign.find()) {
            creds.put("token", mToken.group(1));
            creds.put("sign", mSign.group(1));
        } else {
            throw new RuntimeException("No se pudo leer el Token del XML. XML Limpio: " + cleanXml);
        }
        return creds;
    }
}
