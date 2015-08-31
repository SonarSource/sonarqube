/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package server.suite;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Favourite;
import org.sonar.wsclient.services.FavouriteCreateQuery;
import org.sonar.wsclient.services.FavouriteDeleteQuery;
import org.sonar.wsclient.services.FavouriteQuery;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class WebServiceTest {

  @ClassRule
  public static final Orchestrator orchestrator = ServerTestSuite.ORCHESTRATOR;

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));
  }

  @Test
  public void favourites_web_service() {
    Sonar adminWsClient = orchestrator.getServer().getAdminWsClient();

    // GET (nothing)
    List<Favourite> favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).isEmpty();

    // POST (create favourites)
    Favourite favourite = adminWsClient.create(new FavouriteCreateQuery("sample"));
    assertThat(favourite).isNotNull();
    assertThat(favourite.getKey()).isEqualTo("sample");
    adminWsClient.create(new FavouriteCreateQuery("sample:src/main/xoo/sample/Sample.xoo"));

    // GET (created favourites)
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).hasSize(2);
    List<String> keys = newArrayList(Iterables.transform(favourites, new Function<Favourite, String>() {
      @Override
      public String apply(Favourite input) {
        return input.getKey();
      }
    }));
    assertThat(keys).containsOnly("sample", "sample:src/main/xoo/sample/Sample.xoo");

    // DELETE (a favourite)
    adminWsClient.delete(new FavouriteDeleteQuery("sample"));
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).hasSize(1);
    assertThat(favourites.get(0).getKey()).isEqualTo("sample:src/main/xoo/sample/Sample.xoo");
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
