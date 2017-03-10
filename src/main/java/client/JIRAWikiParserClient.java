/*
 * Copyright (c) AppDynamics, Inc., and its affiliates
 * 2017
 * All Rights Reserved
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF APPDYNAMICS, INC.
 * The copyright notice above does not evidence any actual or intended publication of such source code
 */

package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dto.Test;
import dto.TestPlan;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class JIRAWikiParserClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(JIRAWikiParserClient.class);
    private static final String JIRA_USER = "yourusername";
    private static final String JIRA_PASSWORD = "yourpassword";
    private static final String JIRA_BASE_PATH = "https://singularity.jira.com/wiki";
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").serializeNulls().create();
    private static final String AUTHORIZATION_STRING = getAuthorizationString();

    public static void main(String[] args) throws Exception {
        JIRAWikiParserClient client = new JIRAWikiParserClient();

        String pageName = "Zephyr Test Plan Template";
        pageName = pageName.replaceAll(" ", "+");

        String id = client.getPageIDFromPageName(pageName);
        TestPlan testPlan = client.getTestPlan(pageName);
    }

    public TestPlan getTestPlan(String pageName) throws Exception {
        String id = getPageIDFromPageName(pageName);
        String xml = getContentXML(id);
        TestPlan testPlan = getTestPlanFromXML(xml);
        return testPlan;
    }

    private String getContentXML(String pageId) throws ParserConfigurationException, IOException, SAXException {
        String xmlContentPageBase = JIRA_BASE_PATH + "/rest/prototype/1/content/" + pageId;
        Document contentDocument = null;
        try {
            URL urlObj = new URL(xmlContentPageBase);
            HttpURLConnection uc;
            uc = (HttpsURLConnection) urlObj.openConnection();
            uc.setUseCaches(false);
            uc.setRequestProperty("Authorization", AUTHORIZATION_STRING);
            uc.connect();
            final int responseCode = uc.getResponseCode();
            LOGGER.info("Got Response from jira server: Code: " + responseCode);
            System.out.println("Got Response from jira server: Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream stream = uc.getInputStream();
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                contentDocument = documentBuilder.parse(stream);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed readUrl to " + xmlContentPageBase);
            throw new RuntimeException(e);
        }

        // All the content we need from the wiki page is inside the "body" tag. Extract that.
        NodeList nodes = contentDocument.getElementsByTagName("body");
        Node data = nodes.item(0).getFirstChild();
        String dataString = data.getTextContent();

        // Remove unwanted tags and characters from the XML
        // TODO - This looks like a clear hack. Need to find a better way to handle these tags
        dataString = dataString.replaceAll(":", "");
        dataString = dataString.replaceAll("&nbsp;", "");
        dataString = dataString.replaceAll("<span>", "");
        dataString = dataString.replaceAll("</span>", "");
        dataString = dataString.replaceAll("<br />", "");
        dataString = dataString.replaceAll("<br/>", "");
        StringBuilder sb = new StringBuilder();
        sb.append("<body>").append(dataString).append("</body>");

        return sb.toString();
    }

    private TestPlan getTestPlanFromXML(String xml) throws ParserConfigurationException, IOException, SAXException  {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));

        TestPlan testPlan = new TestPlan();
        Document document = documentBuilder.parse(is);
        NodeList featureNodeList = document.getElementsByTagName("h2");
        String featureName = featureNodeList.item(0).getTextContent(); // Mapped to IssueType = "Test Plan" in Zephyr
        testPlan.setSummary(featureName);

        NodeList descriptionNodelist = document.getElementsByTagName("p");
        String featureDescription = descriptionNodelist.item(0).getTextContent(); // Mapped to Test Plan description
        testPlan.setDescription(featureDescription);

        NodeList testTypeList = document.getElementsByTagName("li");

        List<Test> tests = null;
        List<Test> linkedTests = new LinkedList<Test>();
        for (int i = 0; i < testTypeList.getLength(); i++) {
            Node testTypeNode = testTypeList.item(i);
            NodeList childNodes = testTypeNode.getChildNodes();

            String type;
            Test.TestType testType = null;
            for (int ci = 0; ci < childNodes.getLength(); ci++) {
                Node cNode = childNodes.item(ci);
                if (cNode.getNodeName().equals("h3")) {
                    type = cNode.getTextContent();
                    testType = Test.TestType.getFromValue(type);
                    System.out.println(testType);
                } else if (cNode.getNodeName().equals("actask-list")) {
                    tests = getTestsFromTaskList(cNode, testType);
                    linkedTests.addAll(tests);
                }
            }
        }

        if (linkedTests != null) {
            testPlan.setTests(linkedTests);
        } else {
            throw new RuntimeException("No linked tests were found for the Test Plan - " + testPlan.getSummary());
        }

        return testPlan;
    }

    private List<Test> getTestsFromTaskList(Node taskNode, Test.TestType testType) {
        NodeList childNodes = taskNode.getChildNodes();

        List<Test> tests = new LinkedList<Test>();
        String testName;
        for (int ci = 0; ci < childNodes.getLength(); ci++) {
            Node cNode = childNodes.item(ci);
            if (cNode.getNodeName().equals("actask")) {
                NodeList taskChildNodes = cNode.getChildNodes();

                for (int tci = 0; tci < taskChildNodes.getLength(); tci++) {
                    Node tcNode = taskChildNodes.item(tci);

                    if (tcNode.getNodeName().equals("actask-body")) {
                        testName = tcNode.getTextContent();
                        Test test = new Test();
                        test.setSummary(testName);
                        test.setTestType(testType);
                        tests.add(test);
                    }
                }
            }
        }
        return tests;
    }

    private String getPageIDFromPageName(String page) {
        String contentPageBase = JIRA_BASE_PATH + "/rest/api/content?title=" + page;
        try {
            URL urlObj = new URL(contentPageBase);
            HttpURLConnection uc;
            uc = (HttpsURLConnection) urlObj.openConnection();
            uc.setUseCaches(false);
            uc.setRequestProperty("Authorization", AUTHORIZATION_STRING);
            uc.connect();
            final int responseCode = uc.getResponseCode();
            LOGGER.info("Got Response from jira server: Code: " + responseCode);
            System.out.println("Got Response from jira server: Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream stream = uc.getInputStream();
                String response = IOUtils.toString(stream, "UTF-8");
                JsonObject mainObject = GSON.fromJson(response, JsonObject.class);
                JsonArray array = mainObject.getAsJsonArray("results");

                for (int i = 0; i < array.size(); i++) {
                    JsonObject idObject = array.get(i).getAsJsonObject();
                    String id = idObject.get("id").getAsString();
                    return id;
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Failed readUrl to " + contentPageBase);
            throw new RuntimeException(e);
        }
        return null;
    }

    private static String getAuthorizationString() {
        String unEncodedAuthString = JIRA_USER + ":" + JIRA_PASSWORD;
        String authorizationString =
                "Basic " + new String(Base64.getEncoder().encode(unEncodedAuthString.getBytes()));
        return authorizationString;
    }
}
