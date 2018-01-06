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
package org.sonar.api.web.page;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * Defines the Javascript pages added to SonarQube.
 * <br>
 * This interface replaces the deprecated class {@link org.sonar.api.web.Page}. Moreover, the technology changed from Ruby to Javascript
 * <br>
 * <h3>How to define pages</h3>
 * <pre>
 * import org.sonar.api.web.page.Page.Qualifier;
 *
 * public class MyPluginPagesDefinition implements PagesDefinition {
 *  {@literal @Override}
 *  public void define(Context context) {
 *    context
 *      // Global page by default
 *      .addPage(Page.builder("my_plugin/global_page").setName("Global Page").build())
 *      // Global admin page
 *      .addPage(Page.builder("my_plugin/global_admin_page").setName("Admin Global Page").setAdmin(true).build())
 *      // Project page
 *      .addPage(Page.builder("my_plugin/project_page").setName("Project Page").setScope(Scope.COMPONENT).setQualifiers(Qualifier.PROJECT).build())
 *      // Admin project or module page
 *      .addPage(Page.builder("my_plugin/project_admin_page").setName("Admin Page for Project or Module").setAdmin(true)
 *        .setScope(Scope.COMPONENT).setQualifiers(Qualifier.PROJECT, Qualifier.MODULE).build())
 *      // Page on all components (see Qualifier class) supported
 *      .addPage(Page.builder("my_plugin/component_page").setName("Component Page").setScope(Scope.COMPONENT).build());
 *      // Organization page (when organizations are enabled)
 *      .addPage(Page.builder("my_plugin/org_page").setName("Organization Page").setScope(Scope.ORGANIZATION).build());
 *  }
 * }
 * </pre>
 * <h3>How to test a page definition</h3>
 * <pre>
 *   public class PageDefinitionTest {
 *     {@literal @Test}
 *     public void test_page_definition() {
 *       PageDefinition underTest = context -> context.addPage(Page.builder("my_plugin/my_page").setName("My Page").build());
 *       Context context = new Context();
 *
 *       underTest.define(context);
 *
 *       assertThat(context.getPages()).extracting(Page::getKey).contains("my_plugin/my_page");
 *     }
 * </pre>
 *
 * @since 6.3
 */

@ServerSide
@ExtensionPoint
public interface PageDefinition {
  /**
   * This method is executed when server is started
   */
  void define(Context context);
}
