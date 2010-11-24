/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package itests;

import itests.footer.SampleFooter;
import itests.languages.LanguageWithoutRulesEngine;
import itests.page.GwtSamplePage;
import itests.page.RubyApiTestsPage;
import itests.resourcetab.SampleResourceTab;
import itests.ws.RubyWebService;
import org.sonar.api.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ITestsPlugin implements Plugin {

  public String getKey() {
    return "it";
  }

  public String getName() {
    return "Integration tests";
  }

  public String getDescription() {
    return "Integration tests";
  }

  public List getExtensions() {
    List extensions = new ArrayList();

    extensions.add(SampleSensor.class);
    extensions.add(LanguageWithoutRulesEngine.class);
    extensions.add(ServerSideExtensionUsingExternalDependency.class);

    // web
    extensions.add(SampleResourceTab.class);
    extensions.add(SampleFooter.class);
    extensions.add(GwtSamplePage.class);
    extensions.add(RubyApiTestsPage.class);
    
    // web service
    extensions.add(RubyWebService.class);

    return extensions;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
