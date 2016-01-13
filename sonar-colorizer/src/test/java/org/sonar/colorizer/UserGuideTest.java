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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class UserGuideTest {

  @Test
  public void javaToHtml() throws IOException {
    Reader java = readFile("/org/sonar/colorizer/samples/Sample.java");

    String html = CodeColorizer.javaToHtml(java, HtmlOptions.DEFAULT);

    save(html, "Sample.java.html");
  }

  @Test
  public void groovyToHtml() throws IOException {
    Reader groovy = readFile("/org/sonar/colorizer/samples/Sample.groovy");

    String html = CodeColorizer.groovyToHtml(groovy, HtmlOptions.DEFAULT);

    save(html, "Sample.groovy.html");
  }

  @Test
  public void customizeOutput() throws IOException {
    Reader java = readFile("/org/sonar/colorizer/samples/Sample.java");

    // generate only the <table> element, without including the CSS and the HTML header
    HtmlOptions options = new HtmlOptions(true, "my-table-id", false);
    String html = CodeColorizer.javaToHtml(java, options);

    save(html, "CustomizeOutput.java.html");
  }

  @Test
  public void defineNewLanguage() {

  }

  private FileReader readFile(String path) throws FileNotFoundException {
    return new FileReader(FileUtils.toFile(getClass().getResource(path)));
  }

  private void save(String html, String filename) throws IOException {
    File output = new File("target/userguide/" + filename);
    FileUtils.writeStringToFile(output, html);
    System.out.println("HTML sample saved to: " + output.getAbsolutePath());
  }
}
