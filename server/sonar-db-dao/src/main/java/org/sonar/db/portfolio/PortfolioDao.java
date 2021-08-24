/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singleton;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PortfolioDao implements Dao {
  private final System2 system2;
  private final UuidFactory uuidFactory;

  public PortfolioDao(System2 system2, UuidFactory uuidFactory) {
    // TODO: Audits missing
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public List<PortfolioDto> selectAllRoots(DbSession dbSession) {
    return mapper(dbSession).selectAllRoots();
  }

  public List<PortfolioDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public Optional<PortfolioDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key));
  }

  public Optional<PortfolioDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public void insert(DbSession dbSession, PortfolioDto portfolio) {
    checkArgument(portfolio.isRoot() == (portfolio.getUuid().equals(portfolio.getRootUuid())));
    mapper(dbSession).insert(portfolio);
  }

  public void delete(DbSession dbSession, String portfolioUuid) {
    mapper(dbSession).deletePortfoliosByUuids(singleton(portfolioUuid));
    mapper(dbSession).deleteReferencesByPortfolioOrReferenceUuids(singleton(portfolioUuid));
    mapper(dbSession).deleteProjectsByPortfolioUuids(singleton(portfolioUuid));
  }

  public void deleteByUuids(DbSession dbSession, Set<String> portfolioUuids) {
    if (portfolioUuids.isEmpty()) {
      return;
    }
    mapper(dbSession).deleteByUuids(portfolioUuids);
  }

  public List<PortfolioDto> selectTree(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectTree(portfolioUuid);
  }

  public void update(DbSession dbSession, PortfolioDto portfolio) {
    checkArgument(portfolio.isRoot() == (portfolio.getUuid().equals(portfolio.getRootUuid())));
    portfolio.setUpdatedAt(system2.now());
    mapper(dbSession).update(portfolio);
  }

  public Map<String, String> selectKeysByUuids(DbSession dbSession, Collection<String> uuids) {
    return executeLargeInputs(uuids, uuids1 -> mapper(dbSession).selectByUuids(uuids1)).stream()
      .collect(Collectors.toMap(PortfolioDto::getUuid, PortfolioDto::getKey));
  }

  public void deleteAllDescendantPortfolios(DbSession dbSession, String rootUuid) {
    mapper(dbSession).deleteAllDescendantPortfolios(rootUuid);
  }

  public void addReference(DbSession dbSession, String portfolioUuid, String referenceUuid) {
    mapper(dbSession).insertReference(new PortfolioReferenceDto()
      .setUuid(uuidFactory.create())
      .setPortfolioUuid(portfolioUuid)
      .setReferenceUuid(referenceUuid)
      .setCreatedAt(system2.now()));
  }

  public List<ReferenceDto> selectAllReferencesToPortfolios(DbSession dbSession) {
    return mapper(dbSession).selectAllReferencesToPortfolios();
  }

  public List<ReferenceDto> selectAllReferencesToApplications(DbSession dbSession) {
    return mapper(dbSession).selectAllReferencesToApplications();
  }

  public Set<String> getReferences(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectReferences(portfolioUuid);
  }

  public List<PortfolioDto> selectReferencersByKey(DbSession dbSession, String referenceKey) {
    return mapper(dbSession).selectReferencersByKey(referenceKey);
  }

  public List<PortfolioDto> selectRootOfReferencers(DbSession dbSession, String referenceUuid) {
    return mapper(dbSession).selectRootOfReferencers(referenceUuid);
  }

  public void deleteReferencesTo(DbSession dbSession, String referenceUuid) {
    mapper(dbSession).deleteReferencesTo(referenceUuid);
  }

  public void deleteAllReferences(DbSession dbSession) {
    mapper(dbSession).deleteAllReferences();
  }

  public int deleteReference(DbSession dbSession, String portfolioUuid, String referenceUuid) {
    return mapper(dbSession).deleteReference(portfolioUuid, referenceUuid);
  }

  public ReferenceDto selectReference(DbSession dbSession, String portfolioUuid, String referenceKey) {
    return mapper(dbSession).selectReference(portfolioUuid, referenceKey);
  }

  public List<ProjectDto> getProjects(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectProjects(portfolioUuid);
  }

  public List<PortfolioProjectDto> getAllProjectsInHierarchy(DbSession dbSession, String rootUuid) {
    return mapper(dbSession).selectAllProjectsInHierarchy(rootUuid);
  }

  public void addProject(DbSession dbSession, String portfolioUuid, String projectUuid) {
    mapper(dbSession).insertProject(new PortfolioProjectDto()
      .setUuid(uuidFactory.create())
      .setPortfolioUuid(portfolioUuid)
      .setProjectUuid(projectUuid)
      .setCreatedAt(system2.now()));
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

  public List<PortfolioProjectDto> selectAllProjectsOfPortfolios(DbSession dbSession) {
    return mapper(dbSession).selectAllProjectsOfPortfolios();
  }

  public List<ReferenceDto> selectAllReferencesInHierarchy(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectAllReferencesInHierarchy(uuid);
  }

  public List<PortfolioDto> selectByUuids(DbSession dbSession, Set<String> uuidsToRefresh) {
    return mapper(dbSession).selectByUuids(uuidsToRefresh);
  }

  private static PortfolioMapper mapper(DbSession session) {
    return session.getMapper(PortfolioMapper.class);
  }
}
