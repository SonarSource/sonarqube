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
package org.sonar.server.debt;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.ValidationMessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

public class DebtRulesXMLImporterTest {

  ValidationMessages validationMessages = ValidationMessages.create();
  DebtRulesXMLImporter underTest = new DebtRulesXMLImporter();

  @Test
  public void import_rules() throws Exception {
    String xml = getFileContent("import_rules.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);

    assertThat(results).extracting("ruleKey").containsOnly(RuleKey.of("javasquid", "rule1"), RuleKey.of("javasquid", "rule2"));
    assertThat(validationMessages.getErrors()).isEmpty();
    assertThat(validationMessages.getWarnings()).isEmpty();
  }

  @Test
  public void import_rules_with_deprecated_quality_model_format() throws Exception {
    String xml = getFileContent("import_rules_with_deprecated_quality_model_format.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);

    assertThat(results).extracting("ruleKey").containsOnly(RuleKey.of("javasquid", "rule1"), RuleKey.of("javasquid", "rule2"));
    assertThat(validationMessages.getErrors()).isEmpty();
    assertThat(validationMessages.getWarnings()).isEmpty();
  }

  @Test
  public void import_linear() throws Exception {
    String xml = getFileContent("import_linear.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void import_linear_having_offset_to_zero() throws Exception {
    String xml = getFileContent("import_linear_having_offset_to_zero.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void import_linear_with_offset() throws Exception {
    String xml = getFileContent("import_linear_with_offset.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isEqualTo("1min");
  }

  @Test
  public void import_constant_issue() throws Exception {
    String xml = getFileContent("import_constant_issue.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(ruleDebt.coefficient()).isNull();
    assertThat(ruleDebt.offset()).isEqualTo("3d");
  }

  @Test
  public void use_default_unit_when_no_unit() throws Exception {
    String xml = getFileContent("use_default_unit_when_no_unit.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3d");
    assertThat(ruleDebt.offset()).isEqualTo("1d");
  }

  @Test
  public void replace_mn_by_min() throws Exception {
    String xml = getFileContent("replace_mn_by_min.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3min");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void read_integer() throws Exception {
    String xml = getFileContent("read_integer.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void convert_deprecated_linear_with_threshold_function_by_linear_function() throws Exception {
    String xml = getFileContent("convert_deprecated_linear_with_threshold_function_by_linear_function.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();

    assertThat(validationMessages.getWarnings()).isNotEmpty();
  }

  @Test
  public void convert_constant_per_issue_with_coefficient_by_constant_per_issue_with_offset() throws Exception {
    String xml = getFileContent("convert_constant_per_issue_with_coefficient_by_constant_per_issue_with_offset.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(ruleDebt.coefficient()).isNull();
    assertThat(ruleDebt.offset()).isEqualTo("3h");
  }

  @Test
  public void ignore_remediation_cost_having_zero_value() throws Exception {
    String xml = getFileContent("ignore_remediation_cost_having_zero_value.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).isEmpty();
  }

  @Test
  public void ignore_deprecated_constant_per_file_function() throws Exception {
    String xml = getFileContent("ignore_deprecated_constant_per_file_function.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).isEmpty();

    assertThat(validationMessages.getWarnings()).isNotEmpty();
  }

  @Test
  public void import_badly_formatted_xml() throws Exception {
    String xml = getFileContent("import_badly_formatted_xml.xml");

    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).hasSize(1);

    RuleDebt ruleDebt = results.get(0);
    assertThat(ruleDebt.ruleKey()).isEqualTo(RuleKey.of("checkstyle", "Regexp"));
    assertThat(ruleDebt.function()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(ruleDebt.coefficient()).isEqualTo("3h");
    assertThat(ruleDebt.offset()).isNull();
  }

  @Test
  public void ignore_invalid_value() throws Exception {
    String xml = getFileContent("ignore_invalid_value.xml");
    List<RuleDebt> results = underTest.importXML(xml, validationMessages);
    assertThat(results).isEmpty();

    assertThat(validationMessages.getErrors()).isNotEmpty();
  }

  @Test
  public void fail_on_bad_xml() throws Exception {
    String xml = getFileContent("fail_on_bad_xml.xml");

    try {
      underTest.importXML(xml, validationMessages);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  private String getFileContent(String file) throws Exception {
    return Resources.toString(Resources.getResource(getClass(), "DebtRulesXMLImporterTest/" + file),
      StandardCharsets.UTF_8);
  }
}
