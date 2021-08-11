/*
 * Copyright (C) 2016-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.db.portfolio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

public class PortfolioService {
  private final DbClient dbClient;
  private final PortfolioDao portfolioDao;
  private final ProjectDao projectDao;

  public PortfolioService(DbClient dbClient, PortfolioDao portfolioDao, ProjectDao projectDao) {
    this.dbClient = dbClient;
    this.portfolioDao = portfolioDao;
    this.projectDao = projectDao;
  }

  public void deleteReferencersTo(String referenceKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<PortfolioDto> referencers = portfolioDao.selectReferencersByKey(dbSession, referenceKey);
    }
  }

  public List<PortfolioDto> getRoots() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return portfolioDao.selectAllRoots(dbSession);
    }
  }

  public List<PortfolioDto> getReferencersByKey(String referenceKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return portfolioDao.selectReferencersByKey(dbSession, referenceKey);
    }
  }

  public void addReferenceToPortfolio(String portfolioKey, PortfolioDto refPortfolio) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PortfolioDto portfolio = selectByKeyOrFail(dbSession, portfolioKey);
      Set<String> treeReferenceConnections = getTreeReferenceConnections(dbSession, portfolio.getRootUuid());
      if (treeReferenceConnections.contains(refPortfolio.getRootUuid())) {
        throw new IllegalArgumentException(format("Can't reference portfolio '%s' in portfolio '%s' - hierarchy would be inconsistent",
          refPortfolio.getKey(), portfolio.getKey()));
      }
      portfolioDao.addReference(dbSession, portfolio.getUuid(), refPortfolio.getUuid());
    }
  }

  public void addReferenceToApplication(String portfolioKey, ProjectDto app) {
    // TODO
  }

  public void refresh(List<PortfolioDto> portfolios) {
    Set<String> rootUuids = portfolios.stream().map(PortfolioDto::getRootUuid).collect(Collectors.toSet());
    // TODO eliminate duplicates based on references


  }

  public void appendProject(String portfolioKey, String projectKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PortfolioDto portfolio = selectByKeyOrFail(dbSession, portfolioKey);
      ProjectDto project = projectDao.selectProjectByKey(dbSession, projectKey).orElseThrow(() -> new IllegalArgumentException(format("Project '%s' not found", projectKey)));

      String rootUuid = portfolio.getRootUuid();
      Set<String> allProjectUuids = portfolioDao.getAllProjectsInHierarchy(dbSession, rootUuid);
      if (allProjectUuids.contains(project.getUuid())) {
        // TODO specify which portfolio?
        throw new IllegalArgumentException("The project with key %s is already selected in a portfolio in the hierarchy");
      }
      portfolioDao.addProject(dbSession, portfolio.getUuid(), project.getUuid());
    }
  }

  public void removeProject(String portfolioKey, String projectKey) {

  }

  public void updateSelectionMode(String portfolioKey, String selectionMode, @Nullable String selectionExpression) {
    PortfolioDto.SelectionMode mode = PortfolioDto.SelectionMode.valueOf(selectionMode);
    try (DbSession dbSession = dbClient.openSession(false)) {
      PortfolioDto portfolio = selectByKeyOrFail(dbSession, portfolioKey);

      if (mode.equals(PortfolioDto.SelectionMode.REST)) {
        for (PortfolioDto p : portfolioDao.selectTree(dbSession, portfolio.getUuid())) {
          if (!p.getUuid().equals(portfolio.getUuid()) && mode.name().equals(portfolio.getSelectionMode())) {
            p.setSelectionMode(PortfolioDto.SelectionMode.NONE.name());
            p.setSelectionExpression(null);
            portfolioDao.update(dbSession, p);
          }
        }
      }

      portfolio.setSelectionMode(selectionMode);
      portfolio.setSelectionExpression(selectionExpression);

      if (!mode.equals(PortfolioDto.SelectionMode.MANUAL)) {
        portfolioDao.deleteProjects(dbSession, portfolio.getUuid());
      }

      portfolioDao.update(dbSession, portfolio);
    }
  }

  /**
   * Deletes a portfolio and all of it's children.
   * Also deletes references from/to the deleted portfolios and the projects manually assigned to them.
   */
  public void delete(String portfolioKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PortfolioDto portfolio = selectByKeyOrFail(dbSession, portfolioKey);
      Set<String> subTree = getChildrenRecursively(dbSession, portfolio.getUuid()).stream().map(PortfolioDto::getUuid).collect(Collectors.toSet());
      portfolioDao.deleteByUuids(dbSession, subTree);
      // TODO trigger refresh
    }
  }

  private PortfolioDto selectByKeyOrFail(DbSession dbSession, String portfolioKey) {
    return portfolioDao.selectByKey(dbSession, portfolioKey).orElseThrow(() -> new IllegalArgumentException(format("Portfolio '%s' not found", portfolioKey)));
  }

  /**
   * Gets all portfolios belonging to a subtree, given its root
   */
  private List<PortfolioDto> getChildrenRecursively(DbSession dbSession, String portfolioUuid) {
    Map<String, PortfolioDto> tree = portfolioDao.selectTree(dbSession, portfolioUuid).stream().collect(Collectors.toMap(PortfolioDto::getUuid, x -> x));
    Map<String, Set<String>> childrenMap = new HashMap<>();

    for (PortfolioDto dto : tree.values()) {
      if (dto.getParentUuid() != null) {
        childrenMap.computeIfAbsent(dto.getParentUuid(), x -> new HashSet<>()).add(dto.getUuid());
      }
    }

    List<PortfolioDto> subTree = new ArrayList<>();
    LinkedList<String> stack = new LinkedList<>();
    stack.add(portfolioUuid);

    while (!stack.isEmpty()) {
      Set<String> children = childrenMap.get(stack.removeFirst());
      for (String child : children) {
        subTree.add(tree.get(child));
        stack.add(child);
      }
    }

    return subTree;
  }

  /**
   * Returns the root UUIDs of all trees with which the tree with the given root uuid is connected through a reference.
   * The connection can be incoming or outgoing, and it can be direct or indirect (through other trees).
   *
   * As an example, let's consider we have the following hierarchies of portfolios:
   *
   *  A             D - - -> F
   *  |\           |        |\
   *  B C - - - > E       G  H
   *
   *  Where C references E and D references F.
   *  All 3 tree roots are connected to all the other roots.
   *
   *  getTreeReferenceConnections(A) would return [D,F]
   *  getTreeReferenceConnections(D) would return [A,F]
   *  getTreeReferenceConnections(F) would return [A,D]
   */
  private Set<String> getTreeReferenceConnections(DbSession dbSession, String treeRootUuid) {
    List<ReferenceDto> references = portfolioDao.selectAllReferencesToPortfolios(dbSession);

    Map<String, List<String>> rootConnections = new HashMap<>();

    for (ReferenceDto ref : references) {
      rootConnections.computeIfAbsent(ref.getSourceRootUuid(), x -> new ArrayList<>()).add(ref.getTargetRootUuid());
      rootConnections.computeIfAbsent(ref.getTargetRootUuid(), x -> new ArrayList<>()).add(ref.getSourceRootUuid());
    }

    LinkedList<String> queue = new LinkedList<>();
    Set<String> transitiveReferences = new HashSet<>();

    // add all direct references
    queue.addAll(rootConnections.getOrDefault(treeRootUuid, emptyList()));

    // resolve all transitive references
    while (!queue.isEmpty()) {
      String uuid = queue.remove();
      if (!transitiveReferences.contains(uuid)) {
        queue.addAll(rootConnections.getOrDefault(treeRootUuid, emptyList()));
        transitiveReferences.add(uuid);
      }
    }

    return transitiveReferences;
  }

}
