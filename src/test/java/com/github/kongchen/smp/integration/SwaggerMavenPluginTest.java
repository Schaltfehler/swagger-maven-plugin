package com.github.kongchen.smp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiDocumentMojo;
import net.javacrumbs.jsonunit.core.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.github.kongchen.smp.integration.utils.TestUtils.*;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * @author chekong on 8/15/14.
 */
public class SwaggerMavenPluginTest extends AbstractMojoTestCase {
  private File swaggerOutputDir = new File(getBasedir(), "generated/swagger-ui");
  private File docOutput = new File(getBasedir(), "generated/document.html");
  private ApiDocumentMojo mojo;
  private ObjectMapper mapper = new ObjectMapper();

  @BeforeMethod
  protected void setUp() throws Exception {
    super.setUp();

    try {
      FileUtils.deleteDirectory(swaggerOutputDir);
      FileUtils.forceDelete(docOutput);
    } catch (Exception e) {
      //ignore
    }

    File testPom = new File(getBasedir(), "target/test-classes/plugin-config.xml");
    mojo = (ApiDocumentMojo) lookupMojo("generate", testPom);
  }

  @Test
  public void testSwaggerAvansReaderJson() throws Exception {
    setCustomReader(mojo, "com.github.kongchen.swagger.docgen.reader.AvansReader");
    setLocations(mojo, Arrays.asList("com.wordnik.avans"));

    assertGeneratedSwaggerSpecJson("Processed with AvansReader", "/expectedOutput/swagger-avans.json");
  }

  @Test
  public void testSwaggerAvansReaderYaml() throws Exception {
    setCustomReader(mojo, "com.github.kongchen.swagger.docgen.reader.AvansReader");
    setLocations(mojo, Arrays.asList("com.wordnik.avans"));

    assertGeneratedSwaggerSpecYaml("Processed with AvansReader", "/expectedOutput/swagger-avans.yaml");
  }


  private void assertGeneratedSwaggerSpecJson(String description, String expectedOutput)
    throws MojoExecutionException, MojoFailureException, IOException {
    mojo.execute();

    JsonNode actualJson = mapper.readTree(new File(swaggerOutputDir, "swagger.json"));
    JsonNode expectJson = mapper.readTree(this.getClass().getResourceAsStream(expectedOutput));

    changeDescription(expectJson, description);
    assertJsonEquals(expectJson, actualJson, Configuration.empty().when(IGNORING_ARRAY_ORDER));
  }

  private void assertGeneratedSwaggerSpecYaml(String description, String expectedOutput)
    throws MojoExecutionException, MojoFailureException, IOException {
    mojo.getApiSources().get(0).setOutputFormats("yaml");
    mojo.execute();

    String actualYaml = io.swagger.util.Yaml.pretty().writeValueAsString(new Yaml().load(FileUtils.readFileToString(new File(swaggerOutputDir, "swagger.yaml"))));
    String expectYaml = io.swagger.util.Yaml.pretty().writeValueAsString(new Yaml().load(this.getClass().getResourceAsStream(expectedOutput)));

    JsonNode actualJson = mapper.readTree(YamlToJson(actualYaml));
    JsonNode expectJson = mapper.readTree(YamlToJson(expectYaml));

    changeDescription(expectJson, description);
    assertJsonEquals(expectJson, actualJson, Configuration.empty().when(IGNORING_ARRAY_ORDER));
  }
}
