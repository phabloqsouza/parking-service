package com.estapar.parking.component;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("component/features")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports,cucumber.json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.estapar.parking.component.steps,com.estapar.parking.component.config")
@ConfigurationParameter(key = Constants.EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
public class CucumberComponentTestRunner {
}
