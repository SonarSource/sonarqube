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
package org.sonar.server.qualityprofile.ws;

import java.io.Reader;
import org.junit.Test;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.ws.WsActionTester;

import static org.sonar.test.JsonAssert.assertJson;

public class ImportersActionTest {

  private WsActionTester ws = new WsActionTester(new ImportersAction(createImporters()));

  @Test
  public void json_example() {
    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void empty_importers() {
    ws = new WsActionTester(new ImportersAction());

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("{ \"importers\": [] }");
  }

  private ProfileImporter[] createImporters() {
    class NoopImporter extends ProfileImporter {
      private NoopImporter(String key, String name, String... languages) {
        super(key, name);
        setSupportedLanguages(languages);
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        return RulesProfile.create();
      }

    }

    return new ProfileImporter[] {
      new NoopImporter("pmd", "PMD", "java"),
      new NoopImporter("checkstyle", "Checkstyle", "java"),
      new NoopImporter("js-lint", "JS Lint", "js"),
      new NoopImporter("android-lint", "Android Lint", "xml", "java")
    };
  }
}
