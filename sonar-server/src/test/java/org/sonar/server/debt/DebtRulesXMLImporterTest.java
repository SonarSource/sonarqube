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
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.ValidationMessages;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

public class DebtRulesXMLImporterTest {

  ValidationMessages validationMessages = ValidationMessages.create();
  DebtRulesXMLImporter importer = new DebtRulesXMLImporter();

  @Test
  public void import_rules() {
    String xml = getFileContent("import_rules.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    
    assertThat(results).hasSize(2);
    assertThat(validationMessages.getErrors()).isEmpty();
    assertThat(validationMessages.getWarnings()).isEmpty();
  }

  @Test
  public void import_linear() {
    String xml = getFileContent("import_linear.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void import_linear_having_offset_to_zero() {
    String xml = getFileContent("import_linear_having_offset_to_zero.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void import_linear_with_offset() {
    String xml = getFileContent("import_linear_with_offset.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isEqualTo("1min");
  }

  @Test
  public void import_constant_issue() {
    String xml = getFileContent("import_constant_issue.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(ruleDebt.factor()).isNull();
    assertThat(ruleDebt.offset()).isEqualTo("3d");
  }

  @Test
  public void use_default_unit_when_no_unit() {
    String xml = getFileContent("use_default_unit_when_no_unit.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(ruleDebt.factor()).isEqualTo("3d");
    assertThat(ruleDebt.offset()).isEqualTo("1d");
  }

  @Test
  public void replace_mn_by_min() {
    String xml = getFileContent("replace_mn_by_min.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3min");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void read_integer() {
    String xml = getFileContent("read_integer.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void convert_deprecated_linear_with_threshold_function_by_linear_function() {
    String xml = getFileContent("convert_deprecated_linear_with_threshold_function_by_linear_function.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();

    assertThat(validationMessages.getWarnings()).isNotEmpty();
  }

  @Test
  public void convert_constant_per_issue_with_factor_by_constant_by_issue_with_offset() {
    String xml = getFileContent("convert_constant_per_issue_with_factor_by_constant_by_issue_with_offset.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(ruleDebt.factor()).isNull();
    assertThat(ruleDebt.offset()).isEqualTo("3h");
  }

  @Test
  public void ignore_deprecated_constant_per_file_function() {
    String xml = getFileContent("ignore_deprecated_constant_per_file_function.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).isEmpty();

    assertThat(validationMessages.getWarnings()).isNotEmpty();
  }

  @Test
  public void ignore_rule_on_root_characteristics() {
    String xml = getFileContent("ignore_rule_on_root_characteristics.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).isEmpty();

    assertThat(validationMessages.getWarnings()).isNotEmpty();
  }

  @Test
  public void import_badly_formatted_xml() {
    String xml = getFileContent("import_badly_formatted_xml.xml");

    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(ruleDebt.factor()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void ignore_invalid_value() throws Exception {
    String xml = getFileContent("ignore_invalid_value.xml");
    List<RuleDebt> results = importer.importXML(xml, validationMessages);
    assertThat(results).isEmpty();

    assertThat(validationMessages.getErrors()).isNotEmpty();
  }

  @Test
  public void fail_on_bad_xml() {
    String xml = getFileContent("fail_on_bad_xml.xml");

    try {
      new DebtCharacteristicsXMLImporter().importXML(xml);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  private String getFileContent(String file) {
    try {
      return Resources.toString(Resources.getResource(DebtRulesXMLImporterTest.class, "DebtRulesXMLImporterTest/" + file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
