package com.bis.model.JsonModel;

import java.util.List;

/**
 * Created by Anna Kuranda on 2/23/2017.
 */
public class PublisherScenario {
    private List<Input> inputs;
    private Submit submit;
    private List<String> injects;
    private String loginUrl;
    private String validation;
    private String additionalLoginJava;




    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }



    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }

    public Submit getSubmit() {
        return submit;
    }

    public void setSubmit(Submit submit) {
        this.submit = submit;
    }

    public List<String> getInjects() {
        return injects;
    }

    public void setInjects(List<String> injects) {
        this.injects = injects;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }



    public String getAdditionalLoginJava() {
        return additionalLoginJava;
    }

    public void setAdditionalLoginJava(String additionalLoginJava) {
        this.additionalLoginJava = additionalLoginJava;
    }
}
