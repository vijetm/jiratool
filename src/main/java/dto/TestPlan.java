/*
 * Copyright (c) AppDynamics, Inc., and its affiliates
 * 2017
 * All Rights Reserved
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF APPDYNAMICS, INC.
 * The copyright notice above does not evidence any actual or intended publication of such source code
 */

package dto;

import java.util.List;

/**
 * Created by vijet.mahabaleshwar on 3/9/17.
 */
public class TestPlan {
    private String summary;
    private String description;
    private List<Test> tests;

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTests(List<Test> tests) {
        this.tests = tests;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public List<Test> getTests() {
        return tests;
    }
}


