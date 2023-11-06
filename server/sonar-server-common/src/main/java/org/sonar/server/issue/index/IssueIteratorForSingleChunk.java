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
package org.sonar.server.issue.index;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.cursor.Cursor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IndexedIssueDto;
import org.sonar.server.security.SecurityStandards;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.db.rule.RuleDto.deserializeSecurityStandardsString;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

/**
 * Scrolls over table ISSUES and reads documents to populate
 * the issues index
 */
class IssueIteratorForSingleChunk implements IssueIterator {

  static final Splitter STRING_LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final DbSession session;

  private final Iterator<IndexedIssueDto> iterator;

  IssueIteratorForSingleChunk(DbClient dbClient, @Nullable String branchUuid, @Nullable Collection<String> issueKeys) {
    checkArgument(issueKeys == null || issueKeys.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE,
      "Cannot search for more than " + DatabaseUtils.PARTITION_SIZE_FOR_ORACLE + " issue keys at once. Please provide the keys in smaller chunks.");
    this.session = dbClient.openSession(false);
    try {
      Cursor<IndexedIssueDto> indexCursor = dbClient.issueDao().scrollIssuesForIndexation(session, branchUuid, issueKeys);
      iterator = indexCursor.iterator();
    } catch (Exception e) {
      session.close();
      throw new IllegalStateException("Fail to prepare SQL request to select all issues", e);
    }
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public IssueDoc next() {
    return toIssueDoc(iterator.next());
  }

  private static IssueDoc toIssueDoc(IndexedIssueDto indexedIssueDto) {
    IssueDoc doc = new IssueDoc(new HashMap<>(30));

    String key = indexedIssueDto.getIssueKey();

    // all the fields must be present, even if value is null
    doc.setKey(key);
    doc.setAssigneeUuid(indexedIssueDto.getAssignee());
    doc.setLine(indexedIssueDto.getLine());
    doc.setResolution(indexedIssueDto.getResolution());
    doc.setSeverity(indexedIssueDto.getSeverity());
    String cleanCodeAttributeCategory = Optional.ofNullable(indexedIssueDto.getCleanCodeAttribute())
      .map(CleanCodeAttribute::valueOf)
      .map(CleanCodeAttribute::getAttributeCategory)
      .map(Enum::name)
      .orElse(null);
    doc.setCleanCodeAttributeCategory(cleanCodeAttributeCategory);
    doc.setStatus(indexedIssueDto.getStatus());
    doc.setIssueStatus(indexedIssueDto.getIssueStatus());
    doc.setEffort(indexedIssueDto.getEffort());
    doc.setAuthorLogin(indexedIssueDto.getAuthorLogin());

    doc.setFuncCloseDate(longToDate(indexedIssueDto.getIssueCloseDate()));
    doc.setFuncCreationDate(longToDate(indexedIssueDto.getIssueCreationDate()));
    doc.setFuncUpdateDate(longToDate(indexedIssueDto.getIssueUpdateDate()));

    doc.setRuleUuid(indexedIssueDto.getRuleUuid());
    doc.setLanguage(indexedIssueDto.getLanguage());
    doc.setComponentUuid(indexedIssueDto.getComponentUuid());
    String scope = indexedIssueDto.getScope();
    String filePath = extractFilePath(indexedIssueDto.getPath(), scope);
    doc.setFilePath(filePath);
    doc.setDirectoryPath(extractDirPath(doc.filePath(), scope));
    String branchUuid = indexedIssueDto.getBranchUuid();
    boolean isMainBranch = indexedIssueDto.isMain();
    String projectUuid = indexedIssueDto.getProjectUuid();
    doc.setBranchUuid(branchUuid);
    doc.setIsMainBranch(isMainBranch);
    doc.setProjectUuid(projectUuid);
    String tags = indexedIssueDto.getTags();
    doc.setTags(STRING_LIST_SPLITTER.splitToList(tags == null ? "" : tags));
    doc.setType(RuleType.valueOf(indexedIssueDto.getIssueType()));
    doc.setImpacts(indexedIssueDto.getEffectiveImpacts());
    SecurityStandards securityStandards = fromSecurityStandards(deserializeSecurityStandardsString(indexedIssueDto.getSecurityStandards()));
    SecurityStandards.SQCategory sqCategory = securityStandards.getSqCategory();
    doc.setOwaspTop10(securityStandards.getOwaspTop10());
    doc.setOwaspTop10For2021(securityStandards.getOwaspTop10For2021());
    doc.setPciDss32(securityStandards.getPciDss32());
    doc.setPciDss40(securityStandards.getPciDss40());
    doc.setOwaspAsvs40(securityStandards.getOwaspAsvs40());
    doc.setCwe(securityStandards.getCwe());
    doc.setSansTop25(securityStandards.getSansTop25());
    doc.setSonarSourceSecurityCategory(sqCategory);
    doc.setVulnerabilityProbability(sqCategory.getVulnerability());

    doc.setScope(Qualifiers.UNIT_TEST_FILE.equals(indexedIssueDto.getQualifier()) ? IssueScope.TEST : IssueScope.MAIN);
    doc.setIsNewCodeReference(indexedIssueDto.isNewCodeReferenceIssue());
    String codeVariants = indexedIssueDto.getCodeVariants();
    doc.setCodeVariants(STRING_LIST_SPLITTER.splitToList(codeVariants == null ? "" : codeVariants));
    return doc;

  }

  @CheckForNull
  private static String extractDirPath(@Nullable String filePath, String scope) {
    if (filePath != null) {
      if (Scopes.DIRECTORY.equals(scope)) {
        return filePath;
      }
      int lastSlashIndex = CharMatcher.anyOf("/").lastIndexIn(filePath);
      if (lastSlashIndex > 0) {
        return filePath.substring(0, lastSlashIndex);
      }
      return "/";
    }
    return null;
  }

  @CheckForNull
  private static String extractFilePath(@Nullable String filePath, String scope) {
    // On modules, the path contains the relative path of the module starting from its parent, and in E/S we're only interested in the
    // path
    // of files and directories.
    // That's why the file path should be null on modules and projects.
    if (filePath != null && !Scopes.PROJECT.equals(scope)) {
      return filePath;
    }
    return null;
  }

  @Override
  public void close() {
    session.close();
  }
}
