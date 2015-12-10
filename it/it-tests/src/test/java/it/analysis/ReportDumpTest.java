/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import it.Category3Suite;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportDumpTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  /**
   * SONAR-6905
   */
  @Test
  public void dump_metadata_of_uploaded_report() throws Exception {
    File projectDir = ItUtils.projectDir("shared/xoo-sample");
    orchestrator.executeBuild(SonarRunner.create(projectDir, "sonar.projectKey", "dump_metadata_of_uploaded_report", "sonar.projectName", "dump_metadata_of_uploaded_report"));

    File metadata = new File(projectDir, ".sonar/report-task.txt");
    assertThat(metadata).exists().isFile();

    // verify properties
    Properties props = new Properties();
    props.load(new StringReader(FileUtils.readFileToString(metadata, StandardCharsets.UTF_8)));
    assertThat(props).hasSize(5);
    assertThat(props.getProperty("projectKey")).isEqualTo("dump_metadata_of_uploaded_report");
    assertThat(props.getProperty("ceTaskId")).isNotEmpty();
    verifyUrl(props.getProperty("serverUrl"));
    verifyUrl(props.getProperty("dashboardUrl"));
    verifyUrl(props.getProperty("ceTaskUrl"));
  }

  private void verifyUrl(String url) throws IOException {
    HttpUrl httpUrl = HttpUrl.parse(url);
    Request request = new Request.Builder()
      .url(httpUrl)
      .get()
      .build();
    Response response = new OkHttpClient().newCall(request).execute();
    assertThat(response.isSuccessful()).as(httpUrl.toString()).isTrue();
    assertThat(response.body().string()).as(httpUrl.toString()).isNotEmpty();
  }

}
