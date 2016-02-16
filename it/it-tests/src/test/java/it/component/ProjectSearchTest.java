/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.component;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category4Suite;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class ProjectSearchTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));
  }

  /**
   * SONAR-3105
   */
  @Test
  public void projects_web_service() throws IOException {
    SonarRunner build = SonarRunner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);

    String url = orchestrator.getServer().getUrl() + "/api/projects?key=sample&versions=true";
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(url);
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      String content = IOUtils.toString(response.getEntity().getContent());
      assertThat(content).doesNotContain("error");
      assertThat(content).contains("sample");
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }
}
