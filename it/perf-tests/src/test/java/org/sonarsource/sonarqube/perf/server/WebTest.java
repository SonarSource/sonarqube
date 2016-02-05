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
package org.sonarsource.sonarqube.perf.server;

import com.github.kevinsawicki.http.HttpRequest;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Ignore;
import org.sonarsource.sonarqube.perf.PerfTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.fail;

@Ignore("Temporarily disabled as it requires access to IT_SOURCES")
public class WebTest extends PerfTestCase {

  static final int DEFAULT_PAGE_TIMEOUT_MS = 1000;

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .restoreProfileAtStartup(FileLocation.ofClasspath("/java-quality-profile.xml"))
    .build();

  @BeforeClass
  public static void scan_struts() throws Exception {
    FileLocation strutsHome = orchestrator.getFileLocationOfShared("it-sonar-performancing/struts-1.3.9/pom.xml");
    MavenBuild scan = MavenBuild.create(strutsHome.getFile());
    scan.setGoals("sonar:sonar -V");
    scan.setEnvironmentVariable("MAVEN_OPTS", "-Xmx512m -server");
    scan.setProperty("sonar.scm.disabled", "true");
    scan.setProperty("sonar.sourceEncoding", "UTF-8");
    orchestrator.executeBuild(scan);
  }

  @Test
  public void homepage() throws Exception {
    PageStats counters = request("/");
    assertDurationLessThan(counters.durationMs, 300);
  }

  @Test
  public void quality_profiles() throws Exception {
    PageStats counters = request("/profiles");
    assertDurationLessThan(counters.durationMs, DEFAULT_PAGE_TIMEOUT_MS);
  }

  @Test
  public void issues_search() throws Exception {
    PageStats counters = request("/issues/search");
    assertDurationLessThan(counters.durationMs, 300);
  }

  @Test
  public void measures_search() throws Exception {
    PageStats counters = request("/measures");
    assertDurationLessThan(counters.durationMs, 550);
  }

  @Test
  public void all_projects() throws Exception {
    PageStats counters = request("/all_projects?qualifier=TRK");
    assertDurationLessThan(counters.durationMs, 300);
  }

  @Test
  public void project_measures_search() throws Exception {
    PageStats counters = request("/measures/search?qualifiers[]=TRK");
    assertDurationLessThan(counters.durationMs, 300);
  }

  @Test
  public void file_measures_search() throws Exception {
    PageStats counters = request("/measures/search?qualifiers[]=FIL");
    assertDurationLessThan(counters.durationMs, 500);
  }

  @Test
  public void struts_dashboard() throws Exception {
    PageStats counters = request("/dashboard/index/org.apache.struts:struts-parent?name=Custom");
    assertDurationLessThan(counters.durationMs, 400);
  }

  @Test
  public void struts_issues() throws Exception {
    PageStats counters = request("/issues/search?componentRoots=org.apache.struts:struts-parent");
    assertDurationLessThan(counters.durationMs, 300);
  }

  @Test
  public void struts_issues_drilldown() throws Exception {
    PageStats counters = request("/drilldown/issues/org.apache.struts:struts-parent");
    assertDurationLessThan(counters.durationMs, 400);
  }

  @Test
  public void struts_measures_drilldown() throws Exception {
    PageStats counters = request("/drilldown/measures/org.apache.struts:struts-parent?metric=ncloc");
    // sounds too high !
    assertDurationLessThan(counters.durationMs, DEFAULT_PAGE_TIMEOUT_MS);
  }

  @Test
  public void struts_debt_overview() throws Exception {
    PageStats counters = request("/overview/issues?id=org.apache.struts:struts-parent");
    assertDurationLessThan(counters.durationMs, DEFAULT_PAGE_TIMEOUT_MS);
  }

  @Test
  public void stylesheet_file() throws Exception {
    PageStats counters = request("/css/sonar.css");
    assertDurationLessThan(counters.durationMs, 40);
  }

  @Test
  public void javascript_file() throws Exception {
    PageStats counters = request("/js/bundles/sonar.js");
    assertDurationLessThan(counters.durationMs, 40);
  }

  PageStats request(String path) {
    String url = orchestrator.getServer().getUrl() + path;

    // warm-up
    for (int i = 0; i < 5; i++) {
      newRequest(url).code();
    }

    long targetDuration = Long.MAX_VALUE;
    long targetSize = 0L;
    for (int i = 0; i < 10; i++) {
      HttpRequest request = newRequest(url);
      long start = System.currentTimeMillis();
      if (request.ok()) {
        long duration = System.currentTimeMillis() - start;
        int size = request.body().length();

        if (duration < targetDuration) {
          targetDuration = duration;
          targetSize = size;
        }
        System.out.printf("##### Page %50s %7d ms %7d bytes\n", path, duration, size);
      }
    }
    if (targetDuration == Long.MAX_VALUE) {
      fail(String.format("Failed to load page: %s", url));
    }
    return new PageStats(targetDuration, targetSize);
  }

  private HttpRequest newRequest(String url) {
    HttpRequest request = HttpRequest.get(url);
    request.followRedirects(false).acceptJson().acceptCharset(HttpRequest.CHARSET_UTF8);
    return request;
  }

  static class PageStats {
    long durationMs;
    long sizeBytes;

    PageStats(long durationMs, long sizeBytes) {
      this.durationMs = durationMs;
      this.sizeBytes = sizeBytes;
    }
  }
}
