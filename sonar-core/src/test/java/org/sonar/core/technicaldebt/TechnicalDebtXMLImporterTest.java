/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.technicaldebt;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class TechnicalDebtXMLImporterTest {

  @Test
  public void import_xml_with_linear_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_linear.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, messages);
  }

  @Test
  public void import_xml_with_linear_with_offset() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_linear_with_offset.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, 1.0, messages);
  }

  @Test
  public void convert_deprecated_linear_with_threshold_function_by_linear_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_deprecated_linear_with_threshold.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, 0.0, messages);
    assertThat(messages.getWarnings()).hasSize(1);
  }

  @Test
  public void ignore_deprecated_constant_per_file_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_deprecated_constant_per_file.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getWarnings()).hasSize(1);

    // characteristics
    assertThat(sqale.getRootCharacteristics()).hasSize(1);
    Characteristic efficiency = sqale.getCharacteristicByKey("EFFICIENCY");
    assertThat(efficiency.getChildren()).isEmpty();
  }

  @Test
  public void shouldBadlyFormattedImportXML() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();
    String xml = getFileContent("shouldImportXML_badly-formatted.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, messages);
  }

  @Test
  public void shouldLogWarningIfRuleNotFound() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();
    String xml = getFileContent("shouldLogWarningIfRuleNotFound.xml");
    ValidationMessages messages = ValidationMessages.create();

    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getWarnings()).hasSize(1);

    // characteristics
    assertThat(sqale.getRootCharacteristics()).hasSize(1);
    Characteristic efficiency = sqale.getCharacteristicByKey("EFFICIENCY");
    assertThat(efficiency.getChildren()).isEmpty();
    assertThat(messages.getWarnings().get(0)).contains("findbugs");
  }

  @Test
  public void shouldNotifyOnUnexpectedValueTypeInXml() throws Exception {

    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldRejectXML_with_invalid_value.xml");
    ValidationMessages messages = ValidationMessages.create();

    new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getErrors()).hasSize(1);
    assertThat(messages.getErrors().get(0)).isEqualTo("Cannot import value 'abc' for field factor - Expected a numeric value instead");
  }

  private TechnicalDebtRuleCache mockRuleCache() {
    RuleFinder finder = Mockito.mock(RuleFinder.class);
    Mockito.when(finder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Lists.newArrayList(Rule.create("checkstyle", "Regexp", "Regular expression")));
    return new TechnicalDebtRuleCache(finder);
  }

  private void checkXmlCorrectlyImported(Model sqale, ValidationMessages messages) {
    checkXmlCorrectlyImported(sqale, null, messages);
  }

  private void checkXmlCorrectlyImported(Model sqale, Double offset, ValidationMessages messages) {

    assertThat(messages.getErrors()).isEmpty();
    assertThat(sqale.getName()).isEqualTo(TechnicalDebtModel.MODEL_NAME);

    // characteristics
    assertThat(sqale.getRootCharacteristics()).hasSize(2);
    assertThat(sqale.getCharacteristicByKey("USABILITY").getDescription()).isEqualTo("Estimate usability");
    Characteristic efficiency = sqale.getCharacteristicByKey("EFFICIENCY");
    assertThat(efficiency.getName()).isEqualTo("Efficiency");

    // sub-characteristics
    assertThat(efficiency.getChildren()).hasSize(1);
    Characteristic requirement = efficiency.getChildren().get(0);
    assertThat(requirement.getRule().getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(requirement.getRule().getKey()).isEqualTo("Regexp");
    assertThat(requirement.getPropertyTextValue("remediationFunction", null)).isEqualTo("linear");
    assertThat(requirement.getPropertyValue("remediationFactor", null)).isEqualTo(3.2);
    assertThat(requirement.getPropertyValue("offset", null)).isEqualTo(offset);
  }

  private String getFileContent(String file) {
    try {
      return Resources.toString(Resources.getResource(TechnicalDebtXMLImporterTest.class, "TechnicalDebtXMLImporterTest/" + file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
