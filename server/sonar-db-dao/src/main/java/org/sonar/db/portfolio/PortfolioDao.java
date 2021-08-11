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

import static java.util.Collections.singleton;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

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

  public Optional<PortfolioDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key));

  }

  public Optional<PortfolioDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));

  }

  public void insert(DbSession dbSession, PortfolioDto portfolio) {
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

  public List<ReferenceDto> selectAllReferencesToPortfolios(DbSession dbSession) {
    return mapper(dbSession).selectAllReferencesToPortfolios();
  }

  public List<PortfolioDto> selectTree(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectTree(portfolioUuid);
  }

  public void addReference(DbSession dbSession, String portfolioUuid, String referenceUuid) {
    mapper(dbSession).insertReference(new PortfolioReferenceDto()
      .setUuid(uuidFactory.create())
      .setPortfolioUuid(portfolioUuid)
      .setReferenceUuid(referenceUuid)
      .setCreatedAt(system2.now()));
  }

  public Set<String> getReferences(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectReferences(portfolioUuid);
  }

  public List<PortfolioDto> selectReferencersByKey(DbSession dbSession, String referenceKey) {
    return mapper(dbSession).selectReferencersByKey(referenceKey);
  }

  public Set<String> getProjects(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectProjects(portfolioUuid);
  }

  Set<String> getAllProjectsInHierarchy(DbSession dbSession, String rootUuid) {
    return mapper(dbSession).selectAllProjectsInHierarchy(rootUuid);
  }

  public void addProject(DbSession dbSession, String portfolioUuid, String projectUuid) {
    mapper(dbSession).insertProject(new PortfolioProjectDto()
      .setUuid(uuidFactory.create())
      .setPortfolioUuid(portfolioUuid)
      .setProjectUuid(projectUuid)
      .setCreatedAt(system2.now()));
  }

  public void update(DbSession dbSession, PortfolioDto portfolio) {
    portfolio.setUpdatedAt(system2.now());
    mapper(dbSession).update(portfolio);
  }

  private static PortfolioMapper mapper(DbSession session) {
    return session.getMapper(PortfolioMapper.class);
  }

  public void deleteProjects(DbSession dbSession, String portfolioUuid) {
    mapper(dbSession).deleteProjects(portfolioUuid);
  }
}
