/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.rule;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.Uuids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExportRuleStepTest {
  private static final String REPOSITORY = "repository";

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private SimpleRuleRepository ruleRepository = new SimpleRuleRepository();
  private ExportRuleStep underTest = new ExportRuleStep(ruleRepository, dumpWriter);

  @Test
  public void getDescription_is_set() {
    assertThat(underTest.getDescription()).isEqualTo("Export rules");
  }

  @Test
  public void execute_writes_no_rules_when_repository_is_empty() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.RULES)).isEmpty();
  }

  @Test
  public void execute_writes_all_rules_in_order_returned_by_repository() {
    String[] keys = new String[10];
    for (int i = 0; i < 10; i++) {
      String key = "key_" + i;
      ruleRepository.add(key);
      keys[i] = key;
    }

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Rule> rules = dumpWriter.getWrittenMessagesOf(DumpElement.RULES);
    assertThat(rules).extracting(ProjectDump.Rule::getKey).containsExactly(keys);
    assertThat(rules).extracting(ProjectDump.Rule::getRepository).containsOnly(REPOSITORY);
  }

  @Test
  public void execute_logs_number_total_exported_rules_count_when_successful() {
    logTester.setLevel(Level.DEBUG);
    ruleRepository.add("A").add("B").add("C").add("D");

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).containsExactly("4 rules exported");
  }

  @Test
  public void excuse_throws_ISE_exception_with_number_of_successfully_exported_rules() {
    ruleRepository.add("A").add("B").add("C")
      // will cause NPE
      .addNull();

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Rule Export failed after processing 3 rules successfully");
  }

  private static class SimpleRuleRepository implements RuleRepository {

    private List<Rule> rules = new ArrayList<>();

    @Override
    public Rule register(String uuid, RuleKey ruleKey) {
      throw new UnsupportedOperationException("getByRuleKey not implemented");
    }

    public SimpleRuleRepository add(String key) {
      this.rules.add(new Rule(Uuids.createFast(), REPOSITORY, key));
      return this;
    }

    public SimpleRuleRepository addNull() {
      this.rules.add(null);
      return this;
    }

    @Override
    public Collection<Rule> getAll() {
      return rules;
    }
  }
}
