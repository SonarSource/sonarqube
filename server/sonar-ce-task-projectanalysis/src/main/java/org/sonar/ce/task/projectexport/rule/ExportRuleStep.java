/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;

import static java.lang.String.format;

public class ExportRuleStep implements ComputationStep {
  private final RuleRepository ruleRepository;
  private final DumpWriter dumpWriter;

  public ExportRuleStep(RuleRepository ruleRepository, DumpWriter dumpWriter) {
    this.ruleRepository = ruleRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public String getDescription() {
    return "Export rules";
  }

  @Override
  public void execute(Context context) {
    long count = 0;
    try (StreamWriter<ProjectDump.Rule> writer = dumpWriter.newStreamWriter(DumpElement.RULES)) {
      ProjectDump.Rule.Builder ruleBuilder = ProjectDump.Rule.newBuilder();
      for (Rule rule : ruleRepository.getAll()) {
        ProjectDump.Rule ruleMessage = toRuleMessage(ruleBuilder, rule);
        writer.write(ruleMessage);
        count++;
      }
      Loggers.get(getClass()).debug("{} rules exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Rule Export failed after processing %d rules successfully", count), e);
    }
  }

  private static ProjectDump.Rule toRuleMessage(ProjectDump.Rule.Builder ruleBuilder, Rule rule) {
    ruleBuilder.clear();
    return ruleBuilder
      .setRef(rule.ref())
      .setKey(rule.key())
      .setRepository(rule.repository())
      .build();
  }
}
