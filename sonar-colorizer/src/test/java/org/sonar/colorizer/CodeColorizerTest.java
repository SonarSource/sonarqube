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
package org.sonar.colorizer;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeColorizerTest {

  @Test
  public void javaToHtml() throws IOException {
    Reader java = readFile("/org/sonar/colorizer/samples/Sample.java");

    String html = CodeColorizer.javaToHtml(java, HtmlOptions.DEFAULT);

    assertHtml(html);
    assertContains(html, "<pre><span class=\"k\">public</span> <span class=\"k\">class</span> Sample {</pre>");
  }

  @Test
  public void shouldSupportWindowsEndOfLines() throws IOException {
    Reader windowsFile = readFile("/org/sonar/colorizer/samples/Sample.java", IOUtils.LINE_SEPARATOR_WINDOWS);

    String html = CodeColorizer.javaToHtml(windowsFile, HtmlOptions.DEFAULT);

    assertHtml(html);
    assertContains(html, "<pre><span class=\"k\">public</span> <span class=\"k\">class</span> Sample {</pre>");
  }

  @Test
  public void useHtmlOptions() throws IOException {
    Reader java = readFile("/org/sonar/colorizer/samples/Sample.java");

    HtmlOptions options = new HtmlOptions(true, "my-table-id", false);
    String html = CodeColorizer.javaToHtml(java, options);

    assertThat(html).contains("<table class=\"code\" id=\"my-table-id\"");
    assertThat(html).doesNotContain("<style");
  }

  @Test
  public void groovyToHtml() throws IOException {
    Reader groovy = readFile("/org/sonar/colorizer/samples/Sample.groovy");

    String html = CodeColorizer.groovyToHtml(groovy, HtmlOptions.DEFAULT);

    assertHtml(html);
    assertContains(html, "<pre><span class=\"k\">class</span> Greet {</pre>");
  }

  @Test
  public void mustBeThreadsafe() throws InterruptedException, ExecutionException, IOException {
    final int taskCount = 50;
    final int threadCount = 5;

    class ColorizerTask implements Callable<String> {

      Reader java;

      ColorizerTask() throws IOException {
        this.java = readFile("/org/sonar/colorizer/samples/Sample.java");
      }

      public String call() throws Exception {
        return CodeColorizer.javaToHtml(java, HtmlOptions.ONLY_SYNTAX);
      }
    }

    Collection<Callable<String>> tasks = new ArrayList<>();
    for (int i = 0; i < taskCount; i++) {
      tasks.add(new ColorizerTask());
    }
    List<Future<String>> futures = Executors.newFixedThreadPool(threadCount).invokeAll(tasks);

    assertThat(futures).hasSize(taskCount);

    // all html must be the same
    String html = futures.get(0).get();
    for (Future<String> future : futures) {
      assertThat(html).isEqualTo(future.get());
    }
  }

  @Test
  public void shouldEscapeSpecialCharacters() throws Exception {

    Reader java = readFile("/org/sonar/colorizer/samples/SampleWithComments.java");

    String html = CodeColorizer.javaToHtml(java, HtmlOptions.DEFAULT);

    assertHtml(html);
    assertContains(html, "<pre>  <span class=\"cppd\">/*</span></pre>",
      "<pre><span class=\"cppd\">   * This method does &lt;b&gt;something&lt;/b&gt;</span></pre>",
      "<pre><span class=\"cppd\">   *</span></pre>",
      "<pre><span class=\"cppd\">   * &amp;lt;p&amp;gt;description&amp;lt;/p&amp;gt;</span></pre>",
      "<pre><span class=\"cppd\">   */</span></pre>");
  }

  /**
   * @return Reader for specified file with EOL normalized to specified one.
   */
  private Reader readFile(String path, String eol) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (String line : IOUtils.readLines(getClass().getResourceAsStream(path))) {
      sb.append(line).append(eol);
    }
    return new StringReader(sb.toString());
  }

  /**
   * @return Reader for specified file with EOL normalized to LF.
   */
  private Reader readFile(String path) throws IOException {
    return readFile(path, IOUtils.LINE_SEPARATOR_UNIX);
  }

  private void assertHtml(String html) {
    assertContains(html, "<table class=\"code\"", "</html>");
  }

  private void assertContains(String html, String... strings) {
    for (String string : strings) {
      assertThat(html).contains(string);
    }
  }
}
