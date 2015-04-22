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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

public class ResourceUnmarshaller extends AbstractUnmarshaller<Resource> {

  @Override
  protected Resource parse(Object json) {
    Resource resource = new Resource();
    parseResourceFields(json, resource);
    parseMeasures(json, resource);
    return resource;
  }

  private void parseResourceFields(Object json, Resource resource) {
    WSUtils utils = WSUtils.getINSTANCE();
    resource.setId(utils.getInteger(json, "id"))
        .setKey(utils.getString(json, "key"))
        .setName(utils.getString(json, "name"))
        .setLongName(utils.getString(json, "lname"))
        .setCopy(utils.getInteger(json, "copy"))
        .setScope(utils.getString(json, "scope"))
        .setQualifier(utils.getString(json, "qualifier"))
        .setLanguage(utils.getString(json, "lang"))
        .setDescription(utils.getString(json, "description"))
        .setDate(utils.getDateTime(json, "date"))
        .setCreationDate(utils.getDateTime(json, "creationDate"))
        .setVersion(utils.getString(json, "version"))
        .setPeriod1Mode(utils.getString(json, "p1"))
        .setPeriod1Param(utils.getString(json, "p1p"))
        .setPeriod1Date(utils.getDateTime(json, "p1d"))
        .setPeriod2Mode(utils.getString(json, "p2"))
        .setPeriod2Param(utils.getString(json, "p2p"))
        .setPeriod2Date(utils.getDateTime(json, "p2d"))
        .setPeriod3Mode(utils.getString(json, "p3"))
        .setPeriod3Param(utils.getString(json, "p3p"))
        .setPeriod3Date(utils.getDateTime(json, "p3d"))
        .setPeriod4Mode(utils.getString(json, "p4"))
        .setPeriod4Param(utils.getString(json, "p4p"))
        .setPeriod4Date(utils.getDateTime(json, "p4d"))
        .setPeriod5Mode(utils.getString(json, "p5"))
        .setPeriod5Param(utils.getString(json, "p5p"))
        .setPeriod5Date(utils.getDateTime(json, "p5d"));
  }

  private void parseMeasures(Object json, Resource resource) {
    WSUtils utils = WSUtils.getINSTANCE();
    Object measuresJson = utils.getField(json, "msr");
    if (measuresJson != null) {
      resource.setMeasures(parseMeasures(measuresJson));
    }
  }

  private List<Measure> parseMeasures(Object measuresJson) {
    WSUtils utils = WSUtils.getINSTANCE();
    List<Measure> projectMeasures = new ArrayList<Measure>();
    int len = utils.getArraySize(measuresJson);
    for (int i = 0; i < len; i++) {
      Object measureJson = utils.getArrayElement(measuresJson, i);
      if (measureJson != null) {
        Measure measure = parseMeasure(measureJson);
        projectMeasures.add(measure);
      }
    }
    return projectMeasures;
  }

  private Measure parseMeasure(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();

    Measure measure = new Measure();
    measure
        .setMetricKey(utils.getString(json, "key"))
        .setMetricName(utils.getString(json, "name"))
        .setValue(utils.getDouble(json, "val"))
        .setFormattedValue(utils.getString(json, "frmt_val"))
        .setAlertStatus(utils.getString(json, "alert"))
        .setAlertText(utils.getString(json, "alert_text"))
        .setData(utils.getString(json, "data"))
        .setRuleKey(utils.getString(json, "rule_key"))
        .setRuleName(utils.getString(json, "rule_name"))
        .setRuleCategory(utils.getString(json, "rule_category"))
        .setRuleSeverity(utils.getString(json, "rule_priority"))
        .setCharacteristicKey(utils.getString(json, "ctic_key"))
        .setCharacteristicName(utils.getString(json, "ctic_name"))
        .setVariation1(utils.getDouble(json, "var1"))
        .setVariation2(utils.getDouble(json, "var2"))
        .setVariation3(utils.getDouble(json, "var3"))
        .setVariation4(utils.getDouble(json, "var4"))
        .setVariation5(utils.getDouble(json, "var5"));
    return measure;
  }
}
