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
import org.fest.assertions.Assertions;
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

public class TechnicalDebtXMLImporterTest {

  @Test
  public void shouldImportXML() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML.xml");

    ValidationMessages messages = ValidationMessages.create();
    Model sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, messages);
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

    Assertions.assertThat(messages.getWarnings()).hasSize(1);

    // characteristics
    Assertions.assertThat(sqale.getRootCharacteristics()).hasSize(1);
    Characteristic efficiency = sqale.getCharacteristicByKey("EFFICIENCY");
    Assertions.assertThat(efficiency.getChildren()).isEmpty();
    Assertions.assertThat(messages.getWarnings().get(0)).contains("findbugs");
  }

  @Test
  public void shouldNotifyOnUnexpectedValueTypeInXml() throws Exception {

    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldRejectXML_with_invalid_value.xml");
    ValidationMessages messages = ValidationMessages.create();

    new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    Assertions.assertThat(messages.getErrors()).hasSize(1);
    Assertions.assertThat(messages.getErrors().get(0)).isEqualTo("Cannot import value 'abc' for field factor - Expected a numeric value instead");
  }

  private TechnicalDebtRuleCache mockRuleCache() {
    RuleFinder finder = Mockito.mock(RuleFinder.class);
    Mockito.when(finder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Lists.newArrayList(Rule.create("checkstyle", "Regexp", "Regular expression")));
    return new TechnicalDebtRuleCache(finder);
  }

  private void checkXmlCorrectlyImported(Model sqale, ValidationMessages messages) {

    Assertions.assertThat(messages.getErrors()).isEmpty();
    Assertions.assertThat(sqale.getName()).isEqualTo(TechnicalDebtModel.MODEL_NAME);

    // characteristics
    Assertions.assertThat(sqale.getRootCharacteristics()).hasSize(2);
    Assertions.assertThat(sqale.getCharacteristicByKey("USABILITY").getDescription()).isEqualTo("Estimate usability");
    Characteristic efficiency = sqale.getCharacteristicByKey("EFFICIENCY");
    Assertions.assertThat(efficiency.getName()).isEqualTo("Efficiency");

    // sub-characteristics
    Assertions.assertThat(efficiency.getChildren()).hasSize(1);
    Characteristic requirement = efficiency.getChildren().get(0);
    Assertions.assertThat(requirement.getRule().getRepositoryKey()).isEqualTo("checkstyle");
    Assertions.assertThat(requirement.getRule().getKey()).isEqualTo("Regexp");
    Assertions.assertThat(requirement.getPropertyTextValue("function", null)).isEqualTo("linear");
    Assertions.assertThat(requirement.getPropertyValue("factor", null)).isEqualTo(3.2);
  }

  private String getFileContent(String file) {
    try {
      return Resources.toString(Resources.getResource(TechnicalDebtXMLImporterTest.class, "TechnicalDebtXMLImporterTest/" + file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
