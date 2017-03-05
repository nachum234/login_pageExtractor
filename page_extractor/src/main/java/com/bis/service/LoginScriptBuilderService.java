package com.bis.service;

import com.beust.jcommander.internal.Lists;
import com.bis.conf.CrawlingConfiguration;
import com.bis.model.JsonModel.Input;
import com.bis.model.JsonModel.PublisherScenario;
import com.bis.model.JsonModel.Submit;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Created by Anna Kuranda on 2/23/2017.
 */

//This class wrote in quick .Need think how to improve the code
public class LoginScriptBuilderService {
    public static final String loginFillRegex = "#loginFill#";
    public static final String validationRegex = "#loginValidation#";
    private static Logger logger = Logger.getLogger(LoginScriptBuilderService.class);

    public boolean buildLoginScript(CrawlingConfiguration crawlingConfiguration, PublisherScenario publisherScenario) {

        if(!StringUtils.isEmpty(publisherScenario.getAdditionalLoginJava())){
            return createLoginFile(publisherScenario.getAdditionalLoginJava(), crawlingConfiguration.getCasperPublishersLoginScriptName());
        }


        Path pathToFile = Paths.get(crawlingConfiguration.getCasperTemplateLoginScript());
        if (!pathToFile.toFile().exists()) {
           return false;

        }



        StringBuilder sb = new StringBuilder();
        String loginTemplate = crawlingConfiguration.getCasperTemplateLoginStr();
        if (StringUtils.isEmpty(loginTemplate)) {
            logger.error("No login tempalate found .Cant create login script");
            return false;
        }

        //replace in fill form section


        String injects = buildInjects(publisherScenario.getInjects());
        if (StringUtils.isEmpty(injects) && publisherScenario.getInjects()!=null &&!publisherScenario.getInjects().isEmpty()) return false;
        sb.append(injects).append("\n");


        String fillFormFunc = buildFillFormFunction(publisherScenario.getInputs(), publisherScenario.getSubmit());
        if (StringUtils.isEmpty(fillFormFunc)) {
            return false;
        }
        loginTemplate = loginTemplate.replaceAll(loginFillRegex, fillFormFunc);


        String checkForm = buildCheckFormFunc(publisherScenario.getValidation());
        if (StringUtils.isEmpty(checkForm)) return false;
        loginTemplate = loginTemplate.replaceAll(validationRegex, checkForm);
        sb.append(loginTemplate);
        sb.append("\n");

        return createLoginFile(sb.toString(), crawlingConfiguration.getCasperPublishersLoginScriptName());


    }

    private boolean createLoginFile(String loginTemplate, String casperPublishersLoginScriptName) {
        if (StringUtils.isEmpty(loginTemplate) || StringUtils.isEmpty(casperPublishersLoginScriptName)) return false;
        BufferedWriter output = null;
        Path pathToFile = Paths.get(casperPublishersLoginScriptName);

        try {
            Files.createDirectories(pathToFile.getParent());
            Files.createFile(pathToFile);
            output = new BufferedWriter(new FileWriter(pathToFile.toFile()));
            output.write(loginTemplate);
        } catch (IOException e) {
            logger.error("Failed create login file " + casperPublishersLoginScriptName + " . Error  " + e);
            return false;

        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.warn("Failed close login file after write content" + casperPublishersLoginScriptName + " . Warn  " + e);

                }
            }
        }
        return true;
    }


    private String buildValidationLine(String validationStr) {
        return " return document.querySelectorAll(\"" + validationStr + "\").length > 0;";
    }

    private String buildCheckFormFunc(String validationStr) {
        if (!validValidationSelector(validationStr)) return null;
        return buildValidationLine(validationStr);


    }


    private String buildFillFormFunction(List<Input> inputs, Submit submit) {
        StringBuilder sb = new StringBuilder();
        if (inputs != null && !inputs.isEmpty() && submit != null) {
            for (Input input : inputs) {
                if (!validInput(input))
                    return null;
                else {
                    String str = "document.querySelector(\"" + input.getSelector() + "\").value = \"" + input.getValue() + "\";";
                    sb.append(str);
                    sb.append("\n");
                }

            }
            if (!validSubmit(submit)) {
                return null;
            } else {
                sb.append("document.querySelector(\"" + submit.getTarget() + "\")." + submit.getType() + "();");
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private boolean validValidationSelector(String selector) {
        return !StringUtils.isEmpty(selector);
    }

    private boolean validSubmit(Submit submit) {
        return !StringUtils.isEmpty(submit.getTarget()) && !StringUtils.isEmpty(submit.getType());
    }

    private boolean validInput(Input input) {
        return input != null && !StringUtils.isEmpty(input.getSelector()) && !StringUtils.isEmpty(input.getValue());

    }


    private String buildInjects(List<String> injects) {
        injects = Optional.ofNullable(injects).orElse(Lists.newArrayList());
        StringBuilder sb = new StringBuilder();
        //sb.append("var Q = require('C:/opt/bis/casper_scripts/node_modules/q/q');").append("\n");
        injects.forEach(inject -> {

            sb.append("phantom.injectJs(" + inject + ");").append("\n");

        });

        return sb.toString();

    }




}
