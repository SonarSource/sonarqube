/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package util.selenium;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.sonarqube.qa.util.Tester;

/**
 * Selenium HTML tests, generally written with Selenium IDE
 * @deprecated replaced by {@link Tester}
 */
@Deprecated
public final class Selenese {

  private File[] htmlTests;

  public Selenese(Builder builder) {
    this.htmlTests = builder.htmlTests;
  }

  public File[] getHtmlTests() {
    return htmlTests;
  }

  /**
   * Replaced by Selenide
   */
  @Deprecated
  public static void runSelenese(Orchestrator orchestrator, String... htmlFiles) {
    Selenese selenese = new Builder()
      .setHtmlTests(htmlFiles)
      .build();
    new SeleneseRunner().runOn(selenese, orchestrator);
  }

  private static final class Builder {
    private File[] htmlTests;

    private Builder() {
    }

    public Builder setHtmlTests(File... htmlTests) {
      this.htmlTests = htmlTests;
      return this;
    }

    public Builder setHtmlTests(String... htmlTestPaths) {
      this.htmlTests = new File[htmlTestPaths.length];
      for (int index = 0; index < htmlTestPaths.length; index++) {
        htmlTests[index] = FileUtils.toFile(getClass().getResource(htmlTestPaths[index]));
      }
      return this;
    }

    public Selenese build() {
      if (htmlTests == null || htmlTests.length == 0) {
        throw new IllegalArgumentException("HTML suite or tests are missing");
      }
      for (File htmlTest : htmlTests) {
        checkPresence(htmlTest);
      }
      return new Selenese(this);
    }

    private static void checkPresence(@Nullable File file) {
      if (file == null || !file.isFile() || !file.exists()) {
        throw new IllegalArgumentException("HTML file does not exist: " + file);
      }
    }
  }
}
