/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * This visitor will update the locations field of issues, by filling hashes for their locations:
 * - Primary location hash: for all issues, when needed (ie. is missing or the issue is new/updated)
 * - Secondary location hash: only for taint vulnerabilities and security hotspots, when needed (the issue is new/updated)
 * For performance reasons, it will read each source code file once and feed the lines to all locations in that file.
 */
public class ComputeLocationHashesVisitor extends IssueVisitor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeLocationHashesVisitor.class);

  private static final Pattern MATCH_ALL_WHITESPACES = Pattern.compile("\\s");
  private final List<DefaultIssue> issuesForAllLocations = new LinkedList<>();
  private final List<DefaultIssue> issuesForPrimaryLocation = new LinkedList<>();
  private final SourceLinesRepository sourceLinesRepository;
  private final TreeRootHolder treeRootHolder;
  private final TaintChecker taintChecker;

  public ComputeLocationHashesVisitor(TaintChecker taintChecker, SourceLinesRepository sourceLinesRepository, TreeRootHolder treeRootHolder) {
    this.taintChecker = taintChecker;
    this.sourceLinesRepository = sourceLinesRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void beforeComponent(Component component) {
    issuesForAllLocations.clear();
    issuesForPrimaryLocation.clear();
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issueNeedsLocationHashes(issue)) {
      if (shouldComputeAllLocationHashes(issue)) {
        issuesForAllLocations.add(issue);
      } else if (shouldComputePrimaryLocationHash(issue)) {
        // Issues in this situation are not necessarily marked as changed, so we do it to ensure persistence
        issue.setChanged(true);
        issuesForPrimaryLocation.add(issue);
      }
    }
  }

  private static boolean issueNeedsLocationHashes(DefaultIssue issue) {
    DbIssues.Locations locations = issue.getLocations();
    return !issue.isFromExternalRuleEngine()
      && !issue.isBeingClosed()
      && locations != null;
  }

  private boolean shouldComputeAllLocationHashes(DefaultIssue issue) {
    return taintChecker.isTaintVulnerability(issue)
      && isIssueUpdated(issue);
  }

  private static boolean shouldComputePrimaryLocationHash(DefaultIssue issue) {
    DbIssues.Locations locations = issue.getLocations();
    return (locations.hasTextRange() && !locations.hasChecksum())
      || isIssueUpdated(issue);
  }

  private static boolean isIssueUpdated(DefaultIssue issue) {
    return issue.isNew() || issue.locationsChanged();
  }

  @Override
  public void beforeCaching(Component component) {
    Map<Component, List<Location>> locationsByComponent = new HashMap<>();
    List<LocationToSet> locationsToSet = new LinkedList<>();

    // Issues that needs both primary and secondary locations hashes
    extractForAllLocations(component, locationsByComponent, locationsToSet);
    // Then issues that needs only primary locations
    extractForPrimaryLocation(component, locationsByComponent, locationsToSet);

    // Feed lines to locations, component by component
    locationsByComponent.forEach(this::updateLocationsInComponent);

    // Finalize by setting hashes 
    locationsByComponent.values().forEach(list -> list.forEach(Location::afterAllLines));

    // set new locations to issues
    locationsToSet.forEach(LocationToSet::set);

    issuesForAllLocations.clear();
    issuesForPrimaryLocation.clear();
  }

  private void extractForAllLocations(Component component, Map<Component, List<Location>> locationsByComponent, List<LocationToSet> locationsToSet) {
    for (DefaultIssue issue : issuesForAllLocations) {
      DbIssues.Locations.Builder locationsBuilder = ((DbIssues.Locations) issue.getLocations()).toBuilder();
      addPrimaryLocation(component, locationsByComponent, locationsBuilder);
      addSecondaryLocations(issue, locationsByComponent, locationsBuilder);
      locationsToSet.add(new LocationToSet(issue, locationsBuilder));
    }
  }

  private void extractForPrimaryLocation(Component component, Map<Component, List<Location>> locationsByComponent, List<LocationToSet> locationsToSet) {
    for (DefaultIssue issue : issuesForPrimaryLocation) {
      DbIssues.Locations.Builder locationsBuilder = ((DbIssues.Locations) issue.getLocations()).toBuilder();
      addPrimaryLocation(component, locationsByComponent, locationsBuilder);
      locationsToSet.add(new LocationToSet(issue, locationsBuilder));
    }
  }

  private static void addPrimaryLocation(Component component, Map<Component, List<Location>> locationsByComponent, DbIssues.Locations.Builder locationsBuilder) {
    if (locationsBuilder.hasTextRange()) {
      PrimaryLocation primaryLocation = new PrimaryLocation(locationsBuilder);
      locationsByComponent.computeIfAbsent(component, c -> new LinkedList<>()).add(primaryLocation);
    }
  }

  private void addSecondaryLocations(DefaultIssue issue, Map<Component, List<Location>> locationsByComponent, DbIssues.Locations.Builder locationsBuilder) {
    List<DbIssues.Location.Builder> locationBuilders = locationsBuilder.getFlowBuilderList().stream()
      .flatMap(flowBuilder -> flowBuilder.getLocationBuilderList().stream())
      .filter(DbIssues.Location.Builder::hasTextRange)
      .toList();

    locationBuilders.forEach(locationBuilder -> addSecondaryLocation(locationBuilder, issue, locationsByComponent));
  }

  private void addSecondaryLocation(DbIssues.Location.Builder locationBuilder, DefaultIssue issue, Map<Component, List<Location>> locationsByComponent) {
    String componentUuid = defaultIfEmpty(locationBuilder.getComponentId(), issue.componentUuid());
    Component locationComponent = treeRootHolder.getComponentByUuid(componentUuid);
    locationsByComponent.computeIfAbsent(locationComponent, c -> new LinkedList<>()).add(new SecondaryLocation(locationBuilder));
  }

  private void updateLocationsInComponent(Component component, List<Location> locations) {
    try (CloseableIterator<String> linesIterator = sourceLinesRepository.readLines(component)) {
      int lineNumber = 1;
      while (linesIterator.hasNext()) {
        String line = linesIterator.next();
        for (Location location : locations) {
          location.processLine(lineNumber, line);
        }
        lineNumber++;
      }
    }
  }

  private static class LocationToSet {
    private final DefaultIssue issue;
    private final DbIssues.Locations.Builder locationsBuilder;

    public LocationToSet(DefaultIssue issue, DbIssues.Locations.Builder locationsBuilder) {
      this.issue = issue;
      this.locationsBuilder = locationsBuilder;
    }

    void set() {
      issue.setLocations(locationsBuilder.build());
    }
  }

  private static class PrimaryLocation extends Location {
    private final DbIssues.Locations.Builder locationsBuilder;

    public PrimaryLocation(DbIssues.Locations.Builder locationsBuilder) {
      this.locationsBuilder = locationsBuilder;
    }

    @Override
    DbCommons.TextRange getTextRange() {
      return locationsBuilder.getTextRange();
    }

    @Override
    void setHash(String hash) {
      locationsBuilder.setChecksum(hash);
    }
  }

  private static class SecondaryLocation extends Location {
    private final DbIssues.Location.Builder locationBuilder;

    public SecondaryLocation(DbIssues.Location.Builder locationBuilder) {
      this.locationBuilder = locationBuilder;
    }

    @Override
    DbCommons.TextRange getTextRange() {
      return locationBuilder.getTextRange();
    }

    @Override
    void setHash(String hash) {
      locationBuilder.setChecksum(hash);
    }
  }

  private abstract static class Location {
    private final StringBuilder hashBuilder = new StringBuilder();

    abstract DbCommons.TextRange getTextRange();

    abstract void setHash(String hash);

    public void processLine(int lineNumber, String line) {
      DbCommons.TextRange textRange = getTextRange();
      if (lineNumber > textRange.getEndLine() || lineNumber < textRange.getStartLine()) {
        return;
      }
      try {
        if (lineNumber == textRange.getStartLine() && lineNumber == textRange.getEndLine()) {
          hashBuilder.append(line, textRange.getStartOffset(), textRange.getEndOffset());
        } else if (lineNumber == textRange.getStartLine()) {
          hashBuilder.append(line, textRange.getStartOffset(), line.length());
        } else if (lineNumber < textRange.getEndLine()) {
          hashBuilder.append(line);
        } else {
          hashBuilder.append(line, 0, textRange.getEndOffset());
        }
      } catch (IndexOutOfBoundsException e) {
        LOGGER.debug("Try to compute issue location hash from {} to {} on line ({} chars): {}",
          textRange.getStartOffset(), textRange.getEndOffset(), line.length(), line);
      }
    }

    void afterAllLines() {
      String issueContentWithoutWhitespaces = MATCH_ALL_WHITESPACES.matcher(hashBuilder.toString()).replaceAll("");
      String hash = DigestUtils.md5Hex(issueContentWithoutWhitespaces);
      setHash(hash);
    }
  }
}
