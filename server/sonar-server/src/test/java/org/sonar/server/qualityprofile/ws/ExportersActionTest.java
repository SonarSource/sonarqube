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
package org.sonar.server.qualityprofile.ws;

import org.junit.Test;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.server.ws.WsTester;

import java.io.Writer;

import static org.mockito.Mockito.mock;

public class ExportersActionTest {

  @Test
  public void importers_nominal() throws Exception {
    WsTester wsTester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class), mock(BulkRuleActivationActions.class), mock(ProjectAssociationActions.class),
      new ExportersAction(createExporters())));

    wsTester.newGetRequest("api/qualityprofiles", "exporters").execute().assertJson(getClass(), "exporters.json");
  }

  private ProfileExporter[] createExporters() {
    class NoopImporter extends ProfileExporter {
      private NoopImporter(String key, String name, String... languages) {
        super(key, name);
        setSupportedLanguages(languages);
      }

      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
        // Nothing
      }

    }
    return new ProfileExporter[] {
      new NoopImporter("findbugs", "FindBugs", "java"),
      new NoopImporter("jslint", "JS Lint", "js"),
      new NoopImporter("vaadin", "Vaadin", "java", "js")
    };
  }
}
