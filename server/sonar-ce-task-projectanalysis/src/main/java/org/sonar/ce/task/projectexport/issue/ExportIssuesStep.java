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
package org.sonar.ce.task.projectexport.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hazelcast.internal.util.MutableLong;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.projectexport.rule.Rule;
import org.sonar.ce.task.projectexport.rule.RuleRepository;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpElement.IssueDumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;

import static java.lang.String.format;

public class ExportIssuesStep implements ComputationStep {

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;
  private final RuleRegistrar ruleRegistrar;
  private final ComponentRepository componentRepository;

  public ExportIssuesStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter, RuleRepository ruleRepository,
    ComponentRepository componentRepository) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
    this.componentRepository = componentRepository;
    this.ruleRegistrar = new RuleRegistrar(ruleRepository);
  }

  @Override
  public String getDescription() {
    return "Export issues";
  }

  @Override
  public void execute(Context context) {
    MutableLong count = MutableLong.valueOf(0);
    try (
      StreamWriter<ProjectDump.Issue> output = dumpWriter.newStreamWriter(DumpElement.ISSUES);
      DbSession dbSession = dbClient.openSession(false);
      Cursor<IssueDto> issueDtoCursor = dbClient.projectExportDao()
        .scrollIssueForExport(dbSession, projectHolder.projectDto().getUuid())) {
      ProjectDump.Issue.Builder issueBuilder = ProjectDump.Issue.newBuilder();
      issueDtoCursor
        .forEach(issueDto -> {
          ProjectDump.Issue issue = convertToIssue(issueDto, issueBuilder);
          output.write(issue);
          count.getAndInc();
        });
      LoggerFactory.getLogger(getClass()).debug("{} issues exported", count.value);
    } catch (Exception e) {
      throw new IllegalStateException(format("Issue export failed after processing %d issues successfully", count.value), e);
    }
  }

  private ProjectDump.Issue convertToIssue(IssueDto issueDto, ProjectDump.Issue.Builder builder) {

    String ruleRef = registerRule(issueDto);
    builder
      .clear()
      .setRuleRef(ruleRef)
      .setUuid(issueDto.getKee())
      .setComponentRef(componentRepository.getRef(issueDto.getComponentUuid()))
      .setType(issueDto.getType())
      .setMessage(Optional.of(issueDto).map(IssueDto::getMessage).orElse(""))
      .setLine(Optional.of(issueDto).map(IssueDto::getLine).orElse(0))
      .setChecksum(Optional.of(issueDto).map(IssueDto::getChecksum).orElse(""))
      .setStatus(Optional.of(issueDto).map(IssueDto::getStatus).orElse(""))
      .setResolution(Optional.of(issueDto).map(IssueDto::getResolution).orElse(""))
      .setSeverity(Optional.of(issueDto).map(IssueDto::getSeverity).orElse(""))
      .setManualSeverity(issueDto.isManualSeverity())
      .setGap(Optional.of(issueDto).map(IssueDto::getGap).orElse(IssueDumpElement.NO_GAP))
      .setEffort(Optional.of(issueDto).map(IssueDto::getEffort).orElse(IssueDumpElement.NO_EFFORT))
      .setAssignee(Optional.of(issueDto).map(IssueDto::getAssigneeUuid).orElse(""))
      .setAuthor(Optional.of(issueDto).map(IssueDto::getAuthorLogin).orElse(""))
      .setTags(Optional.of(issueDto).map(IssueDto::getTagsString).orElse(""))
      .setIssueCreatedAt(Optional.of(issueDto).map(IssueDto::getIssueCreationTime).orElse(0L))
      .setIssueUpdatedAt(Optional.of(issueDto).map(IssueDto::getIssueUpdateTime).orElse(0L))
      .setIssueClosedAt(Optional.of(issueDto).map(IssueDto::getIssueCloseTime).orElse(0L))
      .setProjectUuid(issueDto.getProjectUuid())
      .setCodeVariants(Optional.of(issueDto).map(IssueDto::getCodeVariantsString).orElse(""))
      .setPrioritizedRule(issueDto.isPrioritizedRule());
    setLocations(builder, issueDto);
    setMessageFormattings(builder, issueDto);
    mergeImpacts(builder, issueDto);

    return builder.build();
  }

  private static void mergeImpacts(ProjectDump.Issue.Builder builder, IssueDto issueDto) {
    issueDto.getImpacts()
      .stream()
      .map(impactDto -> ProjectDump.Impact.newBuilder()
        .setSoftwareQuality(ProjectDump.SoftwareQuality.valueOf(impactDto.getSoftwareQuality().name()))
        .setSeverity(ProjectDump.Severity.valueOf(impactDto.getSeverity().name()))
        .setManualSeverity(impactDto.isManualSeverity())
        .build())
      .forEach(builder::addImpacts);
  }

  private String registerRule(IssueDto issueDto) {
    String ruleUuid = issueDto.getRuleUuid();
    RuleKey ruleKey = issueDto.getRuleKey();
    return ruleRegistrar.register(ruleUuid, ruleKey).ref();
  }

  private static void setLocations(ProjectDump.Issue.Builder builder, IssueDto issueDto) {
    try {
      byte[] bytes = issueDto.getLocations();
      if (bytes != null) {
        // fail fast, ensure we can read data from DB
        DbIssues.Locations.parseFrom(bytes);
        builder.setLocations(ByteString.copyFrom(bytes));
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(format("Fail to read locations from DB for issue %s", issueDto.getKee()), e);
    }
  }

  private static void setMessageFormattings(ProjectDump.Issue.Builder builder, IssueDto issueDto) {
    try {
      byte[] bytes = issueDto.getMessageFormattings();
      if (bytes != null) {
        // fail fast, ensure we can read data from DB
        DbIssues.MessageFormattings messageFormattings = DbIssues.MessageFormattings.parseFrom(bytes);
        if (messageFormattings != null) {
          builder.addAllMessageFormattings(dbToDumpMessageFormatting(messageFormattings.getMessageFormattingList()));
        }
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(format("Fail to read message formattings from DB for issue %s", issueDto.getKee()), e);
    }
  }

  @VisibleForTesting
  static List<ProjectDump.MessageFormatting> dbToDumpMessageFormatting(List<DbIssues.MessageFormatting> messageFormattingList) {
    return messageFormattingList.stream()
      .map(e -> ProjectDump.MessageFormatting.newBuilder()
        .setStart(e.getStart())
        .setEnd(e.getEnd())
        .setType(ProjectDump.MessageFormattingType.valueOf(e.getType().name())).build())
      .toList();
  }

  private static class RuleRegistrar {
    private final RuleRepository ruleRepository;
    private Rule previousRule = null;
    private String previousRuleUuid = null;

    private RuleRegistrar(RuleRepository ruleRepository) {
      this.ruleRepository = ruleRepository;
    }

    public Rule register(String ruleUuid, RuleKey ruleKey) {
      if (Objects.equals(previousRuleUuid, ruleUuid)) {
        return previousRule;
      }
      return lookup(ruleUuid, ruleKey);
    }

    private Rule lookup(String ruleUuid, RuleKey ruleKey) {
      this.previousRule = ruleRepository.register(ruleUuid, ruleKey);
      this.previousRuleUuid = ruleUuid;
      return previousRule;
    }
  }

}
