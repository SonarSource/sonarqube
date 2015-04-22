/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ResourcesWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/resources")
      .setDescription("Former components web service")
      .setSince("2.10");

    defineIndexAction(controller);
    defineSearchAction(controller);

    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    NewAction action = controller.createAction("index")
      .setDescription("Get a list of components. Requires Browse permission on resource")
      .setSince("2.10")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "resources-example-index.json"));

    action.createParam("resource")
      .setDescription("id or key of the resource")
      .setExampleValue("org.codehaus.sonar:sonar");

    action.createParam("metrics")
      .setDescription("Comma-separated list of <a href=\"http://redirect.sonarsource.com/doc/metric-definitions.html\">metric keys/ids</a>. " +
        "Load measures on selected metrics. If only one metric is set, then measures are ordered by value")
      .setExampleValue("lines,blocker_violations");

    action.createParam("depth")
      .setDescription("Used only when resource is set:<br/>" +
        "<ul>" +
        "<li>0: only selected resource</li>" +
        "<li>-1: all children, including selected resource</li>" +
        "<li>>0: depth toward the selected resource</li>" +
        "</ul>")
      .setDefaultValue("0")
      .setExampleValue("-1");

    action.createParam("scopes")
      .setDescription("Comma-separated list of scopes:<br/>" +
        "<ul>" +
        "<li>PRJ: project/module</li>" +
        "<li>DIR: directory (like Java package)</li>" +
        "<li>FIL: file</li>" +
        "</ul>")
      .setExampleValue("PRJ,DIR");

    action.createParam("qualifiers")
      .setDescription("Comma-separated list of qualifiers:<br/>" +
        "<ul>" +
        "<li>VW: view</li>" +
        "<li>SVW: sub-view</li>" +
        "<li>TRK: project</li>" +
        "<li>BRC: module</li>" +
        "<li>UTS: unit test</li>" +
        "<li>DIR: directory</li>" +
        "<li>FIL: file</li>" +
        "<li>DEV: developer</li>" +
        "</ul>")
      .setExampleValue("TRK,BRC");

    action.createParam("verbose")
      .setDescription("Add some data to response")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");

    action.createParam("limit")
      .setDescription("Limit the number of results. Only used if one metric, and only one, is set")
      .setExampleValue("10");

    action.createParam("includetrends")
      .setDescription("Include period variations in response: add nodes &ltp*&gt for period variations")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");

    action.createParam("includealerts")
      .setDescription("Include alerts data: add nodes &ltalert&gt (ERROR, WARN, OK) and &ltalert_text&gt")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");

    action.createParam("rules")
      .setDescription("Filter on rules: setting it to true will return rules id and rule name for measure having such info " +
        "(such as 'blocker_violations', 'critical_violations', ..., 'new_blocker_violations', ...). Possible values: true | false | list of rule ids")
      .setDefaultValue("false")
      .setExampleValue("true");

    RailsHandler.addFormatParam(action);
  }

  private void defineSearchAction(NewController controller) {
    NewAction action = controller.createAction("search")
      .setDescription("Search for components")
      .setSince("3.3")
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "resources-example-search.json"));

    action.createParam("s")
      .setDescription("To filter on resources containing a specified text in their key")
      .setExampleValue("sonar");

    action.createParam("display_key")
      .setDescription("Return the resource key instead of the resource id")
      .setDefaultValue("false")
      .setPossibleValues("true", "false");

    action.createParam("q")
      .setDescription("Comma-separated list of qualifiers")
      .setExampleValue("TRK,BRC");

    action.createParam("qp")
      .setDescription("Resource Property")
      .setExampleValue("supportsMeasureFilters");

    action.createParam("f")
      .setDescription("If 's2', then it will return a select2 compatible format")
      .setExampleValue("s2");

    action.createParam("p")
      .setDescription("Page index")
      .setDefaultValue("1")
      .setExampleValue("2");

    action.createParam("ps")
      .setDescription("Page size")
      .setDefaultValue("10")
      .setExampleValue("15");

    RailsHandler.addJsonOnlyFormatParam(action);
  }

}
