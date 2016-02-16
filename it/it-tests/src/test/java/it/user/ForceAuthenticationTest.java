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
package it.user;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;

@Category(QaOnly.class)
public class ForceAuthenticationTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  /**
   * SONAR-5542
   */
  @Test
  public void force_authentication_should_be_used_on_java_web_services_but_not_on_batch_index_and_file() throws IOException {
    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      // /batch/index should never need authentication
      String batchIndex = orchestrator.getServer().wsClient().get("/batch/index");
      assertThat(batchIndex).isNotEmpty();

      String jar = batchIndex.split("\\|")[0];

      // /batch/file should never need authentication
      HttpClient httpclient = new DefaultHttpClient();
      try {
        HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/batch/file?name=" + jar);
        HttpResponse response = httpclient.execute(get);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        EntityUtils.consume(response.getEntity());

        // As Sonar runner is still using /batch/key, we have to also verify it
        get = new HttpGet(orchestrator.getServer().getUrl() + "/batch/" + jar);
        response = httpclient.execute(get);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        EntityUtils.consume(response.getEntity());

      } finally {
        httpclient.getConnectionManager().shutdown();
      }

      // but other java web services should need authentication
      try {
        orchestrator.getServer().wsClient().get("/api");
      } catch (HttpException e) {
        assertThat(e.getMessage()).contains("401");
      }

    } finally {
      orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery("sonar.forceAuthentication"));
    }
  }

}
