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
package org.sonar.server.debt;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.SystemUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonar.server.debt.DebtModelXMLExporter.DebtModel;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

public class DebtModelXMLExporterTest {

  private DebtModelXMLExporter xmlExporter;

  @Before
  public void setup() {
    xmlExporter = new DebtModelXMLExporter();
  }

  @Test
  public void export_empty() {
    assertThat(xmlExporter.export(new DebtModel(), Collections.<RuleDebt>emptyList())).isEqualTo("<sqale/>" + SystemUtils.LINE_SEPARATOR);
  }

  @Test
  public void export_xml() throws Exception {
    DebtModel debtModel = new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setId(1).setKey("USABILITY").setName("Usability").setOrder(1))
      .addRootCharacteristic(new DefaultDebtCharacteristic().setId(2).setKey("EFFICIENCY").setName("Efficiency").setOrder(2))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setId(3).setKey("MEMORY_USE").setName("Memory use").setParentId(2), "EFFICIENCY");

    List<RuleDebt> rules = newArrayList(
      new RuleDebt().setRuleKey(RuleKey.of("checkstyle", "Regexp"))
        .setSubCharacteristicKey("MEMORY_USE").setFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name()).setCoefficient("3d").setOffset("15min")
    );

    assertSimilarXml(getFileContent("export_xml.xml"), xmlExporter.export(debtModel, rules));
  }

  public static void assertSimilarXml(String expectedXml, String xml) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    Diff diff = XMLUnit.compareXML(xml, expectedXml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    assertTrue(message, diff.similar());
  }

  @Test
  public void sort_root_characteristics_by_order_and_sub_characteristics_by_name() throws Exception {
    DebtModel debtModel = new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("EFFICIENCY").setName("Efficiency").setOrder(4))
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("USABILITY").setName("Usability").setOrder(3))
      .addRootCharacteristic(new DefaultDebtCharacteristic().setKey("PORTABILITY").setName("Portability").setOrder(2))

      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("RAM_EFFICIENCY").setName("RAM Efficiency"), "EFFICIENCY")
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("CPU_EFFICIENCY").setName("CPU Efficiency"), "EFFICIENCY")
      .addSubCharacteristic(new DefaultDebtCharacteristic().setKey("OTHER_EFFICIENCY").setName("Other Efficiency"), "EFFICIENCY");

    String xml = xmlExporter.export(debtModel, Collections.<RuleDebt>emptyList());

    // root characteristics are sorted by the column "characteristic_order"
    Pattern regex = Pattern.compile(".*USABILITY.*PORTABILITY.*EFFICIENCY.*", Pattern.DOTALL);
    assertThat(regex.matcher(xml).matches());

    // sub characteristics are sorted by name
    regex = Pattern.compile(".*CPU Efficiency.*Other Efficiency.*RAM Efficiency.*", Pattern.DOTALL);
    assertThat(regex.matcher(xml).matches());
  }

  @Test
  public void pretty_print_exported_xml() throws Exception {
    DebtModel debtModel = new DebtModel()
      .addRootCharacteristic(new DefaultDebtCharacteristic().setId(1).setKey("USABILITY").setName("Usability").setOrder(1))
      .addRootCharacteristic(new DefaultDebtCharacteristic().setId(2).setKey("EFFICIENCY").setName("Efficiency").setOrder(2))
      .addSubCharacteristic(new DefaultDebtCharacteristic().setId(3).setKey("MEMORY_USE").setName("Memory use").setParentId(2), "EFFICIENCY");

    List<RuleDebt> rules = newArrayList(
      new RuleDebt().setRuleKey(RuleKey.of("checkstyle", "Regexp"))
        .setSubCharacteristicKey("MEMORY_USE").setFunction(DebtRemediationFunction.Type.LINEAR.name()).setCoefficient("3d")
    );
    assertThat(xmlExporter.export(debtModel, rules)).isEqualTo(
      "<sqale>" + SystemUtils.LINE_SEPARATOR +
        "  <chc>" + SystemUtils.LINE_SEPARATOR +
        "    <key>USABILITY</key>" + SystemUtils.LINE_SEPARATOR +
        "    <name>Usability</name>" + SystemUtils.LINE_SEPARATOR +
        "  </chc>" + SystemUtils.LINE_SEPARATOR +
        "  <chc>" + SystemUtils.LINE_SEPARATOR +
        "    <key>EFFICIENCY</key>" + SystemUtils.LINE_SEPARATOR +
        "    <name>Efficiency</name>" + SystemUtils.LINE_SEPARATOR +
        "    <chc>" + SystemUtils.LINE_SEPARATOR +
        "      <key>MEMORY_USE</key>" + SystemUtils.LINE_SEPARATOR +
        "      <name>Memory use</name>" + SystemUtils.LINE_SEPARATOR +
        "      <chc>" + SystemUtils.LINE_SEPARATOR +
        "        <rule-repo>checkstyle</rule-repo>" + SystemUtils.LINE_SEPARATOR +
        "        <rule-key>Regexp</rule-key>" + SystemUtils.LINE_SEPARATOR +
        "        <prop>" + SystemUtils.LINE_SEPARATOR +
        "          <key>remediationFunction</key>" + SystemUtils.LINE_SEPARATOR +
        "          <txt>LINEAR</txt>" + SystemUtils.LINE_SEPARATOR +
        "        </prop>" + SystemUtils.LINE_SEPARATOR +
        "        <prop>" + SystemUtils.LINE_SEPARATOR +
        "          <key>remediationFactor</key>" + SystemUtils.LINE_SEPARATOR +
        "          <val>3</val>" + SystemUtils.LINE_SEPARATOR +
        "          <txt>d</txt>" + SystemUtils.LINE_SEPARATOR +
        "        </prop>" + SystemUtils.LINE_SEPARATOR +
        "      </chc>" + SystemUtils.LINE_SEPARATOR +
        "    </chc>" + SystemUtils.LINE_SEPARATOR +
        "  </chc>" + SystemUtils.LINE_SEPARATOR +
        "</sqale>" + SystemUtils.LINE_SEPARATOR
    );
  }

  private String getFileContent(String file) throws Exception {
    return Resources.toString(Resources.getResource(getClass(), "DebtModelXMLExporterTest/" + file), Charsets.UTF_8);
  }
}
