package org.example;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.example.ai.Answer;
import org.example.ai.Choices;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ServerException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(getSslConnectionSocketFactory())
                .build()) {
            submit(httpClient, getHttpPost());
        }
        System.out.printf("hello world!");
        System.out.printf("hello world!1");
        System.out.printf("hello world!1");
        System.out.printf("hello world!1 ms-t");
    }

    private static void submit(CloseableHttpClient httpClient, HttpPost post) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String question;
        while (true) {
            System.out.print("You: ");
            question = scanner.nextLine();
            System.out.print("AI: ");
            StringEntity stringEntity = new StringEntity(getRequestJson(question), getContentType());
            post.setEntity(stringEntity);
            CloseableHttpResponse response;
            try {
                response = httpClient.execute(post);
            } catch (SocketTimeoutException e) {
                System.out.println("-- warning: Read timed out!");
                continue;
            } catch (SocketException e) {
                System.out.println("-- warning: Connection reset!");
                continue;
            } catch (Exception e) {
                System.out.println("-- warning: Please try again!");
                continue;
            }
            printAnswer(response);
        }
    }

    private static void printAnswer(CloseableHttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String responseJson = EntityUtils.toString(response.getEntity());
            Answer answer = JSON.parseObject(responseJson, Answer.class);
            StringBuilder answers = new StringBuilder();
            List<Choices> choices = answer.getChoices();
            for (Choices choice : choices) {
                answers.append(choice.getText());
            }
            System.out.println(answers.substring(2, answers.length()));
        } else if (response.getStatusLine().getStatusCode() == 429) {
            System.out.println("-- warning: Too Many Requests!");
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            throw new ServerException("------ Server error, program terminated! ------");
        } else {
            System.out.println("-- warning: Error, please try again!");
        }
    }

    private static ContentType getContentType() {
        return ContentType.create("text/json", "UTF-8");
    }

    private static String getRequestJson(String question) {
        return "{\"model\": \"text-davinci-003\", \"prompt\": \"" + question + "\", \"temperature\": 0, \"max_tokens\": 1024}";
    }

    private static HttpPost getHttpPost() throws IOException {
        Properties prop =new Properties();
        InputStream inputStream = Files.newInputStream(Paths.get("resources/application.properties"));
        prop.load(inputStream);
        String openAiKey = prop.getProperty("SECRET_KEY");
        String connectTimeout = prop.getProperty("connectTimeout");
        String connectionRequestTimeout = prop.getProperty("connectionRequestTimeout");
        String socketTimeout = prop.getProperty("socketTimeout");
        HttpPost post = new HttpPost("https://api.openai.com/v1/completions");
        post.addHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Bearer " + openAiKey);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Integer.parseInt(connectTimeout)).setConnectionRequestTimeout(Integer.parseInt(connectionRequestTimeout))
                .setSocketTimeout(Integer.parseInt(socketTimeout)).build();
        post.setConfig(requestConfig);
        return post;
    }

    private static SSLConnectionSocketFactory getSslConnectionSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }
}