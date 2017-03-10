/*
 * Copyright (c) AppDynamics, Inc., and its affiliates
 * 2017
 * All Rights Reserved
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF APPDYNAMICS, INC.
 * The copyright notice above does not evidence any actual or intended publication of such source code
 */

package dto;

/**
 * Created by vijet.mahabaleshwar on 3/9/17.
 */
public class Test {
    private String summary;
    private String description;
    private TestType testType;
    private String id;

    public enum TestType {
        FUNCTIONALITY("Functionality"),
        SCALABILITY("Scalability"),
        LIMITS("Limits"),
        PERFORMANCE("Performance"),
        DEBUGGABILITY("Debuggability"),
        UPGRADE("Upgrade"),
        BACKWARD_COMPATIBILITY("Backward-Compatibility"),
        SECURITY("Security"),
        NEGATIVE_CASE("Negative-Case");

        private String type;

        TestType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static TestType getFromValue(String value) {
            TestType resp = null;
            TestType nodes[] = values();
            for(int i = 0; i < nodes.length; i++) {
                if(nodes[i].getType().equals(value)) {
                    resp = nodes[i];
                    break;
                }
            }
            return resp;
        }

    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public TestType getTestType() {
        return testType;
    }

    public String getId() {
        return id;
    }
}
