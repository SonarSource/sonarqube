/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.hazelcast.internal.util.MutableLong;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.project.ProjectExportMapper;
import org.sonar.db.rule.RuleDto;

import static java.lang.String.format;

public class ExportAdHocRulesStep implements ComputationStep {

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;

  public ExportAdHocRulesStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    MutableLong count = MutableLong.valueOf(0L);
    try (
      StreamWriter<ProjectDump.AdHocRule> output = dumpWriter.newStreamWriter(DumpElement.AD_HOC_RULES);
      DbSession dbSession = dbClient.openSession(false);
      Cursor<RuleDto> ruleDtoCursor = dbSession.getMapper(ProjectExportMapper.class).scrollAdhocRulesForExport(projectHolder.projectDto().getUuid())) {
      ProjectDump.AdHocRule.Builder adHocRuleBuilder = ProjectDump.AdHocRule.newBuilder();
      ruleDtoCursor
        .forEach(ruleDto -> {
          ProjectDump.AdHocRule rule = convertToAdHocRule(ruleDto, adHocRuleBuilder);
          output.write(rule);
          count.getAndInc();
        });
      LoggerFactory.getLogger(getClass()).debug("{} ad-hoc rules exported", count.value);

    } catch (Exception e) {
      throw new IllegalStateException(format("Ad-hoc rules export failed after processing %d rules successfully", count.value), e);
    }
  }

  private static ProjectDump.AdHocRule convertToAdHocRule(RuleDto ruleDto, ProjectDump.AdHocRule.Builder builder) {
    CleanCodeAttribute cleanCodeAttribute = ruleDto.getCleanCodeAttribute();
    return builder
      .clear()
      .setRef(ruleDto.getUuid())
      .setPluginKey(Optional.of(ruleDto).map(RuleDto::getPluginKey).orElse(""))
      .setPluginRuleKey(ruleDto.getKey().rule())
      .setPluginName(ruleDto.getRepositoryKey())
      .setName(Optional.of(ruleDto).map(RuleDto::getName).orElse(""))
      .setStatus(Optional.of(ruleDto).map(RuleDto::getStatus).map(Enum::name).orElse(""))
      .setType(ruleDto.getType())
      .setScope(ruleDto.getScope().name())
      .setMetadata(buildMetadata(ruleDto))
      .setCleanCodeAttribute(cleanCodeAttribute != null ? cleanCodeAttribute.name() : null)
      .addAllImpacts(buildImpacts(ruleDto.getDefaultImpacts()))
      .build();
  }

  private static List<ProjectDump.Impact> buildImpacts(Set<ImpactDto> defaultImpacts) {
    return defaultImpacts
      .stream()
      .map(i -> ProjectDump.Impact.newBuilder()
        .setSoftwareQuality(ProjectDump.SoftwareQuality.valueOf(i.getSoftwareQuality().name()))
        .setSeverity(ProjectDump.Severity.valueOf(i.getSeverity().name())).build())
      .toList();
  }

  private static ProjectDump.AdHocRule.RuleMetadata buildMetadata(RuleDto ruleDto) {
    Optional<RuleDto> rule = Optional.of(ruleDto);
    return ProjectDump.AdHocRule.RuleMetadata.newBuilder()
      .setAdHocName(rule.map(RuleDto::getAdHocName).orElse(""))
      .setAdHocDescription(rule.map(RuleDto::getAdHocDescription).orElse(""))
      .setAdHocSeverity(rule.map(RuleDto::getAdHocSeverity).orElse(""))
      .setAdHocType(rule.map(RuleDto::getAdHocType).orElse(0))
      .build();
  }

  @Override
  public String getDescription() {
    return "Export ad-hoc rules";
  }
}
