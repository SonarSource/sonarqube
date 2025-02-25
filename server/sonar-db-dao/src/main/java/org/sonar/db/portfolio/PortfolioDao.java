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
package org.sonar.db.portfolio;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.project.ApplicationProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PortfolioDao implements Dao {
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public PortfolioDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  /*
   * Select portfolios
   */
  public List<PortfolioDto> selectAllRoots(DbSession dbSession) {
    return mapper(dbSession).selectAllRoots();
  }

  /**
   * select all application projects belong to the hierarchy of a portfolio
   */
  public List<ApplicationProjectDto> selectAllApplicationProjects(DbSession dbSession, String rootPortfolioUuid) {
    return mapper(dbSession).selectAllApplicationProjects(rootPortfolioUuid);
  }

  public List<PortfolioDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public List<PortfolioDto> selectTree(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectTree(portfolioUuid);
  }

  public Optional<PortfolioDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key));
  }

  public List<PortfolioDto> selectByKeys(DbSession dbSession, Set<String> portfolioDbKeys) {
    return executeLargeInputs(portfolioDbKeys, input -> mapper(dbSession).selectByKeys(input));
  }

  public Optional<PortfolioDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<PortfolioDto> selectByUuids(DbSession dbSession, Set<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return mapper(dbSession).selectByUuids(uuids);
  }

  /*
   * Modify portfolios
   */
  public void insert(DbSession dbSession, PortfolioDto portfolio) {
    checkArgument(portfolio.isRoot() == (portfolio.getUuid().equals(portfolio.getRootUuid())));
    mapper(dbSession).insert(portfolio);
    auditPersister.addComponent(dbSession, toComponentNewValue(portfolio));
  }

  public void delete(DbSession dbSession, PortfolioDto portfolio) {
    mapper(dbSession).deleteReferencesByPortfolioOrReferenceUuids(singleton(portfolio.getUuid()));
    mapper(dbSession).deletePortfolio(portfolio.getUuid());
    auditPersister.deleteComponent(dbSession, toComponentNewValue(portfolio));
  }

  /**
   * Does NOT delete related references and project/branch selections!
   */
  public void deleteAllDescendantPortfolios(DbSession dbSession, String rootUuid) {
    // not audited but it's part of DefineWs
    mapper(dbSession).deleteAllDescendantPortfolios(rootUuid);
  }

  public void update(DbSession dbSession, PortfolioDto portfolio) {
    checkArgument(portfolio.isRoot() == (portfolio.getUuid().equals(portfolio.getRootUuid())));
    portfolio.setUpdatedAt(system2.now());
    mapper(dbSession).update(portfolio);
    auditPersister.updateComponent(dbSession, toComponentNewValue(portfolio));
  }

  /*
   * Portfolio references
   */
  public void addReferenceBranch(DbSession dbSession, String portfolioUuid, String referenceUuid, String branchUuid) {
    mapper(dbSession).insertReference(uuidFactory.create(), portfolioUuid, referenceUuid, branchUuid, system2.now());
  }

  public void addReference(DbSession dbSession, String portfolioUuid, String referenceUuid) {
    PortfolioDto portfolio = mapper(dbSession).selectByUuid(referenceUuid);
    if (portfolio == null) {
      mapper(dbSession).insertReference(uuidFactory.create(), portfolioUuid, referenceUuid, referenceUuid, system2.now());
    } else {
      mapper(dbSession).insertReference(uuidFactory.create(), portfolioUuid, referenceUuid, null, system2.now());
    }
  }

  public List<ReferenceDto> selectAllReferencesToPortfolios(DbSession dbSession) {
    return mapper(dbSession).selectAllReferencesToPortfolios();
  }

  public List<ReferenceDto> selectAllReferencesToApplications(DbSession dbSession) {
    return mapper(dbSession).selectAllReferencesToApplications();
  }

  public List<ReferenceDto> selectAllReferencesToPortfoliosInHierarchy(DbSession dbSession, String rootUuid) {
    return mapper(dbSession).selectAllReferencesToPortfoliosInHierarchy(rootUuid);
  }

  public List<ReferenceDto> selectAllReferencesToApplicationsInHierarchy(DbSession dbSession, String rootUuid) {
    return mapper(dbSession).selectAllReferencesToApplicationsInHierarchy(rootUuid);
  }

  public List<String> selectApplicationReferenceUuids(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectApplicationReferenceUuids(portfolioUuid);
  }

  public Set<String> selectReferenceUuids(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectReferenceUuids(portfolioUuid);
  }

  public List<PortfolioDto> selectReferencers(DbSession dbSession, String referenceUuid) {
    return mapper(dbSession).selectReferencers(referenceUuid);
  }

  public List<PortfolioDto> selectRootOfReferencers(DbSession dbSession, String referenceUuid) {
    return mapper(dbSession).selectRootOfReferencers(referenceUuid);
  }

  public List<PortfolioDto> selectRootOfReferencersToMainBranch(DbSession dbSession, String referenceUuid) {
    return mapper(dbSession).selectRootOfReferencersToMainBranch(referenceUuid);
  }

  public List<PortfolioDto> selectRootOfReferencersToAppBranch(DbSession dbSession, String appUuid, String appBranchKey) {
    return mapper(dbSession).selectRootOfReferencersToAppBranch(appUuid, appBranchKey);
  }

  public void deleteReferencesTo(DbSession dbSession, String referenceUuid) {
    mapper(dbSession).deleteReferencesTo(referenceUuid);
  }

  public void deleteAllReferences(DbSession dbSession) {
    mapper(dbSession).deleteAllReferences();
  }

  public int deleteReferenceBranch(DbSession dbSession, String portfolioUuid, String referenceUuid, String branchUuid) {
    return mapper(dbSession).deleteReferenceBranch(portfolioUuid, referenceUuid, branchUuid);
  }

  public int deleteReference(DbSession dbSession, String portfolioUuid, String referenceUuid) {
    return mapper(dbSession).deleteReference(portfolioUuid, referenceUuid);
  }

  @CheckForNull
  public ReferenceDto selectReference(DbSession dbSession, String portfolioUuid, String referenceKey) {
    return selectReferenceToApp(dbSession, portfolioUuid, referenceKey)
      .or(() -> selectReferenceToPortfolio(dbSession, portfolioUuid, referenceKey))
      .orElse(null);
  }

  public Optional<ReferenceDto> selectReferenceToApp(DbSession dbSession, String portfolioUuid, String referenceKey) {
    return Optional.ofNullable(mapper(dbSession).selectReferenceToApplication(portfolioUuid, referenceKey));
  }

  public Optional<ReferenceDto> selectReferenceToPortfolio(DbSession dbSession, String portfolioUuid, String referenceKey) {
    return Optional.ofNullable(mapper(dbSession).selectReferenceToPortfolio(portfolioUuid, referenceKey));
  }

  /*
   * Manual selection of projects
   */
  public List<PortfolioProjectDto> selectPortfolioProjects(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectPortfolioProjects(portfolioUuid);
  }

  public List<PortfolioProjectDto> selectAllProjectsInHierarchy(DbSession dbSession, String rootUuid) {
    return mapper(dbSession).selectAllProjectsInHierarchy(rootUuid);
  }

  public List<PortfolioProjectDto> selectAllPortfolioProjects(DbSession dbSession) {
    return mapper(dbSession).selectAllPortfolioProjects();
  }

  public PortfolioProjectDto selectPortfolioProjectOrFail(DbSession dbSession, String portfolioUuid, String projectUuid) {
    return Optional.ofNullable(mapper(dbSession).selectPortfolioProject(portfolioUuid, projectUuid)).orElseThrow(() ->
      new IllegalArgumentException(format("Project '%s' not selected in portfolio '%s'", projectUuid, portfolioUuid)));
  }

  public String addProject(DbSession dbSession, String portfolioUuid, String projectUuid) {
    String uuid = uuidFactory.create();
    mapper(dbSession).insertProject(uuid, portfolioUuid, projectUuid, system2.now());
    return uuid;
  }

  public void deleteProjects(DbSession dbSession, String portfolioUuid) {
    mapper(dbSession).deleteProjects(portfolioUuid);
  }

  public void deleteProject(DbSession dbSession, String portfolioUuid, String projectUuid) {
    mapper(dbSession).deleteProject(portfolioUuid, projectUuid);
  }

  public void deleteAllProjects(DbSession dbSession) {
    mapper(dbSession).deleteAllProjects();
  }

  public void addBranch(DbSession dbSession, String portfolioProjectUuid, String branchUuid) {
    mapper(dbSession).insertBranch(uuidFactory.create(), portfolioProjectUuid, branchUuid, system2.now());
  }

  public void deleteBranch(DbSession dbSession, String portfolioUuid, String projectUuid, String branchUuid) {
    mapper(dbSession).deleteBranch(portfolioUuid, projectUuid, branchUuid);
  }

  public long updateMeasuresMigrated(DbSession dbSession, String branchUuid, boolean measuresMigrated) {
    long now = system2.now();
    return mapper(dbSession).updateMeasuresMigrated(branchUuid, measuresMigrated, now);
  }

  public List<String> selectUuidsWithMeasuresMigratedFalse(DbSession session, int limit) {
    return mapper(session).selectUuidsWithMeasuresMigratedFalse(limit);
  }

  public int countByMeasuresMigratedFalse(DbSession session) {
    return mapper(session).countByMeasuresMigratedFalse();
  }

  public void updateMeasuresMigratedToFalse(DbSession dbSession) {
    mapper(dbSession).updateMeasuresMigratedToFalse();
  }

  @VisibleForTesting
  boolean isMeasuresMigrated(DbSession dbSession, String uuid) {
    return mapper(dbSession).isMeasuresMigrated(uuid);
  }

  /*
   * Utils
   */
  private static PortfolioMapper mapper(DbSession session) {
    return session.getMapper(PortfolioMapper.class);
  }

  private static ComponentNewValue toComponentNewValue(PortfolioDto portfolio) {
    return new ComponentNewValue(portfolio.getUuid(), portfolio.isPrivate(), portfolio.getName(), portfolio.getKey(), portfolio.getDescription(), qualifier(portfolio));
  }

  private static String qualifier(PortfolioDto portfolioDto) {
    return portfolioDto.isRoot() ? Qualifiers.VIEW : Qualifiers.SUBVIEW;
  }

}
