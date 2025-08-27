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
package org.sonar.ce.task.projectanalysis.util.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultImpact;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;
import org.sonar.db.protobuf.DbIssues;

import static java.util.Optional.ofNullable;

public class ProtobufIssueDiskCache implements DiskCache<DefaultIssue> {
  private static final String TAGS_SEPARATOR = ",";
  private static final Splitter STRING_LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final File file;
  private final System2 system2;

  public ProtobufIssueDiskCache(File file, System2 system2) {
    this.file = file;
    this.system2 = system2;
  }

  @Override
  public long fileSize() {
    return file.length();
  }

  @Override
  public CacheAppender<DefaultIssue> newAppender() {
    try {
      return new ProtoCacheAppender();
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public CloseableIterator<DefaultIssue> traverse() {
    CloseableIterator<IssueCache.Issue> protoIterator = Protobuf.readStream(file, IssueCache.Issue.parser());
    return new CloseableIterator<>() {
      @CheckForNull
      @Override
      protected DefaultIssue doNext() {
        if (protoIterator.hasNext()) {
          return toDefaultIssue(protoIterator.next());
        }
        return null;
      }

      @Override
      protected void doClose() {
        protoIterator.close();
      }
    };
  }

  @VisibleForTesting
  static DefaultIssue toDefaultIssue(IssueCache.Issue next) {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setKey(next.getKey());
    defaultIssue.setType(RuleType.fromDbConstant(next.getRuleType()));
    defaultIssue.setComponentUuid(next.hasComponentUuid() ? next.getComponentUuid() : null);
    defaultIssue.setComponentKey(next.getComponentKey());
    defaultIssue.setProjectUuid(next.getProjectUuid());
    defaultIssue.setProjectKey(next.getProjectKey());
    defaultIssue.setRuleKey(RuleKey.parse(next.getRuleKey()));
    defaultIssue.setLanguage(next.hasLanguage() ? next.getLanguage() : null);
    defaultIssue.setSeverity(next.hasSeverity() ? next.getSeverity() : null);
    defaultIssue.setManualSeverity(next.getManualSeverity());
    defaultIssue.setMessage(next.hasMessage() ? next.getMessage() : null);
    defaultIssue.setMessageFormattings(next.hasMessageFormattings() ? next.getMessageFormattings() : null);
    defaultIssue.setLine(next.hasLine() ? next.getLine() : null);
    defaultIssue.setGap(next.hasGap() ? next.getGap() : null);
    defaultIssue.setEffort(next.hasEffort() ? Duration.create(next.getEffort()) : null);
    defaultIssue.setStatus(next.getStatus());
    defaultIssue.setResolution(next.hasResolution() ? next.getResolution() : null);
    defaultIssue.setAssigneeUuid(next.hasAssigneeUuid() ? next.getAssigneeUuid() : null);
    defaultIssue.setAssigneeLogin(next.hasAssigneeLogin() ? next.getAssigneeLogin() : null);
    defaultIssue.setChecksum(next.hasChecksum() ? next.getChecksum() : null);
    defaultIssue.setAuthorLogin(next.hasAuthorLogin() ? next.getAuthorLogin() : null);
    next.getCommentsList().forEach(c -> defaultIssue.addComment(toDefaultIssueComment(c)));
    defaultIssue.setTags(ImmutableSet.copyOf(STRING_LIST_SPLITTER.split(next.getTags())));
    defaultIssue.setCodeVariants(ImmutableSet.copyOf(STRING_LIST_SPLITTER.split(next.getCodeVariants())));
    defaultIssue.setRuleDescriptionContextKey(next.hasRuleDescriptionContextKey() ? next.getRuleDescriptionContextKey() : null);
    defaultIssue.setLocations(next.hasLocations() ? next.getLocations() : null);
    defaultIssue.setIsFromExternalRuleEngine(next.getIsFromExternalRuleEngine());
    defaultIssue.setCreationDate(new Date(next.getCreationDate()));
    defaultIssue.setUpdateDate(next.hasUpdateDate() ? new Date(next.getUpdateDate()) : null);
    defaultIssue.setCloseDate(next.hasCloseDate() ? new Date(next.getCloseDate()) : null);
    defaultIssue.setCurrentChangeWithoutAddChange(next.hasCurrentChanges() ? toDefaultIssueChanges(next.getCurrentChanges()) : null);
    defaultIssue.setNew(next.getIsNew());
    defaultIssue.setIsOnChangedLine(next.getIsOnChangedLine());
    defaultIssue.setIsNewCodeReferenceIssue(next.getIsNewCodeReferenceIssue());
    defaultIssue.setCopied(next.getIsCopied());
    defaultIssue.setBeingClosed(next.getBeingClosed());
    defaultIssue.setOnDisabledRule(next.getOnDisabledRule());
    defaultIssue.setChanged(next.getIsChanged());
    defaultIssue.setSendNotifications(next.getSendNotifications());
    defaultIssue.setSelectedAt(next.hasSelectedAt() ? next.getSelectedAt() : null);
    defaultIssue.setQuickFixAvailable(next.getQuickFixAvailable());
    defaultIssue.setPrioritizedRule(next.getIsPrioritizedRule());
    defaultIssue.setFromSonarQubeUpdate(next.getIsFromSonarqubeUpdate());
    defaultIssue.setIsNoLongerNewCodeReferenceIssue(next.getIsNoLongerNewCodeReferenceIssue());
    defaultIssue.setCleanCodeAttribute(next.hasCleanCodeAttribute() ? CleanCodeAttribute.valueOf(next.getCleanCodeAttribute()) : null);
    if (next.hasAnticipatedTransitionUuid()) {
      defaultIssue.setAnticipatedTransitionUuid(next.getAnticipatedTransitionUuid());
    }

    for (IssueCache.Impact impact : next.getImpactsList()) {
      defaultIssue.addImpact(SoftwareQuality.valueOf(impact.getSoftwareQuality()), Severity.valueOf(impact.getSeverity()), impact.getManualSeverity());
    }
    for (IssueCache.FieldDiffs protoFieldDiffs : next.getChangesList()) {
      defaultIssue.addChange(toDefaultIssueChanges(protoFieldDiffs));
    }
    return defaultIssue;
  }

  @VisibleForTesting
  static IssueCache.Issue toProto(IssueCache.Issue.Builder builder, DefaultIssue defaultIssue) {
    builder.clear();
    builder.setKey(defaultIssue.key());
    builder.setRuleType(defaultIssue.type().getDbConstant());
    ofNullable(defaultIssue.getCleanCodeAttribute()).ifPresent(value -> builder.setCleanCodeAttribute(value.name()));
    ofNullable(defaultIssue.componentUuid()).ifPresent(builder::setComponentUuid);
    builder.setComponentKey(defaultIssue.componentKey());
    builder.setProjectUuid(defaultIssue.projectUuid());
    builder.setProjectKey(defaultIssue.projectKey());
    builder.setRuleKey(defaultIssue.ruleKey().toString());
    ofNullable(defaultIssue.language()).ifPresent(builder::setLanguage);
    ofNullable(defaultIssue.severity()).ifPresent(builder::setSeverity);
    builder.setManualSeverity(defaultIssue.manualSeverity());
    ofNullable(defaultIssue.message()).ifPresent(builder::setMessage);
    ofNullable(defaultIssue.getMessageFormattings()).ifPresent(m -> builder.setMessageFormattings((DbIssues.MessageFormattings) m));
    ofNullable(defaultIssue.line()).ifPresent(builder::setLine);
    ofNullable(defaultIssue.gap()).ifPresent(builder::setGap);
    ofNullable(defaultIssue.effort()).map(Duration::toMinutes).ifPresent(builder::setEffort);
    builder.setStatus(defaultIssue.status());
    ofNullable(defaultIssue.resolution()).ifPresent(builder::setResolution);
    ofNullable(defaultIssue.assignee()).ifPresent(builder::setAssigneeUuid);
    ofNullable(defaultIssue.assigneeLogin()).ifPresent(builder::setAssigneeLogin);
    ofNullable(defaultIssue.checksum()).ifPresent(builder::setChecksum);
    ofNullable(defaultIssue.authorLogin()).ifPresent(builder::setAuthorLogin);
    defaultIssue.defaultIssueComments().forEach(c -> builder.addComments(toProtoComment(c)));
    ofNullable(defaultIssue.tags()).ifPresent(t -> builder.setTags(String.join(TAGS_SEPARATOR, t)));
    ofNullable(defaultIssue.codeVariants()).ifPresent(codeVariant -> builder.setCodeVariants(String.join(TAGS_SEPARATOR, codeVariant)));
    ofNullable(defaultIssue.getLocations()).ifPresent(l -> builder.setLocations((DbIssues.Locations) l));
    defaultIssue.getRuleDescriptionContextKey().ifPresent(builder::setRuleDescriptionContextKey);
    builder.setIsFromExternalRuleEngine(defaultIssue.isFromExternalRuleEngine());
    builder.setCreationDate(defaultIssue.creationDate().getTime());
    ofNullable(defaultIssue.updateDate()).map(Date::getTime).ifPresent(builder::setUpdateDate);
    ofNullable(defaultIssue.closeDate()).map(Date::getTime).ifPresent(builder::setCloseDate);
    ofNullable(defaultIssue.currentChange()).ifPresent(c -> builder.setCurrentChanges(toProtoIssueChanges(c)));
    builder.setIsNew(defaultIssue.isNew());
    builder.setIsOnChangedLine(defaultIssue.isOnChangedLine());
    builder.setIsPrioritizedRule(defaultIssue.isPrioritizedRule());
    builder.setIsFromSonarqubeUpdate(defaultIssue.isFromSonarQubeUpdate());
    builder.setIsNewCodeReferenceIssue(defaultIssue.isNewCodeReferenceIssue());
    builder.setIsCopied(defaultIssue.isCopied());
    builder.setBeingClosed(defaultIssue.isBeingClosed());
    builder.setOnDisabledRule(defaultIssue.isOnDisabledRule());
    builder.setIsChanged(defaultIssue.isChanged());
    builder.setSendNotifications(defaultIssue.mustSendNotifications());
    ofNullable(defaultIssue.selectedAt()).ifPresent(builder::setSelectedAt);
    builder.setQuickFixAvailable(defaultIssue.isQuickFixAvailable());
    builder.setIsNoLongerNewCodeReferenceIssue(defaultIssue.isNoLongerNewCodeReferenceIssue());
    defaultIssue.getAnticipatedTransitionUuid().ifPresent(builder::setAnticipatedTransitionUuid);

    for (DefaultImpact impact : defaultIssue.getImpacts()) {
      builder.addImpacts(IssueCache.Impact.newBuilder()
        .setSoftwareQuality(impact.softwareQuality().name())
        .setSeverity(impact.severity().name())
        .setManualSeverity(impact.manualSeverity())
        .build());
    }
    for (FieldDiffs fieldDiffs : defaultIssue.changes()) {
      builder.addChanges(toProtoIssueChanges(fieldDiffs));
    }
    return builder.build();
  }

  private static DefaultIssueComment toDefaultIssueComment(IssueCache.Comment comment) {
    DefaultIssueComment issueComment = new DefaultIssueComment()
      .setCreatedAt(new Date(comment.getCreatedAt()))
      .setUpdatedAt(new Date(comment.getUpdatedAt()))
      .setNew(comment.getIsNew())
      .setKey(comment.getKey())
      .setIssueKey(comment.getIssueKey())
      .setMarkdownText(comment.getMarkdownText());

    if (comment.hasUserUuid()) {
      issueComment.setUserUuid(comment.getUserUuid());
    }
    return issueComment;
  }

  private static IssueCache.Comment toProtoComment(DefaultIssueComment comment) {
    IssueCache.Comment.Builder builder = IssueCache.Comment.newBuilder()
      .setCreatedAt(comment.createdAt().getTime())
      .setUpdatedAt(comment.updatedAt().getTime())
      .setIsNew(comment.isNew())
      .setKey(comment.key())
      .setIssueKey(comment.issueKey())
      .setMarkdownText(comment.markdownText());

    if (comment.userUuid() != null) {
      builder.setUserUuid(comment.userUuid());
    }
    return builder.build();
  }

  private static FieldDiffs toDefaultIssueChanges(IssueCache.FieldDiffs fieldDiffs) {
    FieldDiffs defaultIssueFieldDiffs = new FieldDiffs()
      .setUserUuid(fieldDiffs.getUserUuid())
      .setCreationDate(new Date(fieldDiffs.getCreationDate()));

    if (fieldDiffs.hasIssueKey()) {
      defaultIssueFieldDiffs.setIssueKey(fieldDiffs.getIssueKey());
    }

    for (Map.Entry<String, IssueCache.Diff> e : fieldDiffs.getDiffsMap().entrySet()) {
      defaultIssueFieldDiffs.setDiff(e.getKey(),
        e.getValue().hasOldValue() ? e.getValue().getOldValue() : null,
        e.getValue().hasNewValue() ? e.getValue().getNewValue() : null);
    }

    return defaultIssueFieldDiffs;
  }

  private static IssueCache.FieldDiffs toProtoIssueChanges(FieldDiffs fieldDiffs) {
    IssueCache.FieldDiffs.Builder builder = IssueCache.FieldDiffs.newBuilder()
      .setCreationDate(fieldDiffs.creationDate().getTime());

    fieldDiffs.issueKey().ifPresent(builder::setIssueKey);
    fieldDiffs.userUuid().ifPresent(builder::setUserUuid);

    for (Map.Entry<String, FieldDiffs.Diff> e : fieldDiffs.diffs().entrySet()) {
      IssueCache.Diff.Builder diffBuilder = IssueCache.Diff.newBuilder();
      Serializable oldValue = e.getValue().oldValue();
      if (oldValue != null) {
        diffBuilder.setOldValue(oldValue.toString());
      }
      Serializable newValue = e.getValue().newValue();
      if (newValue != null) {
        diffBuilder.setNewValue(newValue.toString());

      }
      builder.putDiffs(e.getKey(), diffBuilder.build());
    }

    return builder.build();
  }

  private class ProtoCacheAppender implements CacheAppender<DefaultIssue> {
    private final OutputStream out;
    private final IssueCache.Issue.Builder builder;

    private ProtoCacheAppender() throws FileNotFoundException {
      this.out = new BufferedOutputStream(new FileOutputStream(file, true));
      this.builder = IssueCache.Issue.newBuilder();
    }

    @Override
    public CacheAppender append(DefaultIssue object) {
      Protobuf.writeStream(Collections.singleton(toProto(builder, object)), out);
      return this;
    }

    @Override
    public void close() {
      system2.close(out);
    }
  }
}
