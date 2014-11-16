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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessConstants;
import org.sonar.server.issue.index.IssueAuthorizationNormalizer;
import org.sonar.server.issue.index.IssueNormalizer;
import org.sonar.server.search.BaseNormalizer;

/**
 * Definition of ES index "issues", including settings and fields.
 */
public class IssueIndexDefinition implements IndexDefinition {

  public static final String INDEX_ISSUES = "issues";
  public static final String TYPE_ISSUE_AUTHORIZATION = "issueAuthorization";
  public static final String TYPE_ISSUE = "issue";

  private final Settings settings;

  public IssueIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_ISSUES);

    // shards
    boolean clusterMode = settings.getBoolean(ProcessConstants.CLUSTER_ACTIVATE);
    if (clusterMode) {
      index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 4);
      index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1);
      // else keep defaults (one shard)
    }

    // type "issueAuthorization"
    NewIndex.NewMapping authorizationMapping = index.createMapping(TYPE_ISSUE_AUTHORIZATION);
    authorizationMapping.setAttribute("_id", ImmutableMap.of("path", IssueAuthorizationNormalizer.IssueAuthorizationField.PROJECT.field()));
    authorizationMapping.createDateTimeField(BaseNormalizer.UPDATED_AT_FIELD);
    authorizationMapping.stringFieldBuilder("project").build();
    authorizationMapping.stringFieldBuilder("groups").build();
    authorizationMapping.stringFieldBuilder("users").build();

    // type "issue"
    NewIndex.NewMapping issueMapping = index.createMapping(TYPE_ISSUE);
    issueMapping.setAttribute("_id", ImmutableMap.of("path", IssueNormalizer.IssueField.KEY.field()));
    issueMapping.setAttribute("_parent", ImmutableMap.of("type", TYPE_ISSUE_AUTHORIZATION));
    issueMapping.setAttribute("_routing", ImmutableMap.of("required", true, "path", IssueNormalizer.IssueField.PROJECT.field()));
    issueMapping.stringFieldBuilder("component").build();
    issueMapping.stringFieldBuilder("actionPlan").build();
    // TODO do we really sort by assignee ?
    issueMapping.stringFieldBuilder("assignee").enableSorting().build();
    issueMapping.stringFieldBuilder("attributes").build();
    issueMapping.stringFieldBuilder("authorLogin").build();
    issueMapping.createDateTimeField("createdAt");
    issueMapping.createDoubleField("debt");
    issueMapping.createDoubleField("effort");
    issueMapping.stringFieldBuilder("filePath").enableSorting().build();
    issueMapping.createDateTimeField("issueCreatedAt");
    issueMapping.createDateTimeField("issueUpdatedAt");
    issueMapping.createDateTimeField("issueClosedAt");
    issueMapping.stringFieldBuilder("key").enableSorting().build();
    issueMapping.stringFieldBuilder("language").build();
    issueMapping.createIntegerField("line");
    issueMapping.stringFieldBuilder("message").build();
    issueMapping.stringFieldBuilder("module").build();
    issueMapping.createUuidPathField("modulePath");
    // TODO do we need to sort by project ?
    issueMapping.stringFieldBuilder("project").enableSorting().build();
    issueMapping.stringFieldBuilder("reporter").build();
    issueMapping.stringFieldBuilder("resolution").build();
    issueMapping.stringFieldBuilder("ruleKey").build();
    issueMapping.stringFieldBuilder("severity").build();
    // TODO do we need to sort by severity ?
    issueMapping.createByteField("severityValue");
    // TODO do we really sort by status ? If yes, then we should sort by "int value", but not by string key
    issueMapping.stringFieldBuilder("status").enableSorting().build();
    issueMapping.createDateTimeField("updatedAt");
  }
}
