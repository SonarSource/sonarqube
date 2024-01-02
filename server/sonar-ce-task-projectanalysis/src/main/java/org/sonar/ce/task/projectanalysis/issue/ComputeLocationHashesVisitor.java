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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

/**
 * This visitor will update the locations field of issues, by filling the hashes for all locations.
 * It only applies to issues that are taint vulnerabilities and that are new or were changed.
 * For performance reasons, it will read each source code file once and feed the lines to all locations in that file.
 */
public class ComputeLocationHashesVisitor extends IssueVisitor {
  private static final Pattern MATCH_ALL_WHITESPACES = Pattern.compile("\\s");
  private final List<DefaultIssue> issues = new LinkedList<>();
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
    issues.clear();
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (taintChecker.isTaintVulnerability(issue) && !issue.isFromExternalRuleEngine() && (issue.isNew() || issue.locationsChanged())) {
      issues.add(issue);
    }
  }

  @Override
  public void beforeCaching(Component component) {
    Map<Component, List<Location>> locationsByComponent = new HashMap<>();
    List<LocationToSet> locationsToSet = new LinkedList<>();

    for (DefaultIssue issue : issues) {
      if (issue.getLocations() == null) {
        continue;
      }

      DbIssues.Locations.Builder primaryLocationBuilder = ((DbIssues.Locations) issue.getLocations()).toBuilder();
      boolean hasTextRange = addLocations(component, issue, locationsByComponent, primaryLocationBuilder);

      // If any location was added (because it had a text range), we'll need to update the issue at the end with the new object containing the hashes
      if (hasTextRange) {
        locationsToSet.add(new LocationToSet(issue, primaryLocationBuilder));
      }
    }

    // Feed lines to locations, component by component
    locationsByComponent.forEach(this::updateLocationsInComponent);

    // Finalize by setting hashes 
    locationsByComponent.values().forEach(list -> list.forEach(Location::afterAllLines));

    // set new locations to issues
    locationsToSet.forEach(LocationToSet::set);

    issues.clear();
  }

  private boolean addLocations(Component component, DefaultIssue issue, Map<Component, List<Location>> locationsByComponent, DbIssues.Locations.Builder primaryLocationBuilder) {
    boolean hasTextRange = false;

    // Add primary location
    if (primaryLocationBuilder.hasTextRange()) {
      hasTextRange = true;
      PrimaryLocation primaryLocation = new PrimaryLocation(primaryLocationBuilder);
      locationsByComponent.computeIfAbsent(component, c -> new LinkedList<>()).add(primaryLocation);
    }

    // Add secondary locations
    for (DbIssues.Flow.Builder flowBuilder : primaryLocationBuilder.getFlowBuilderList()) {
      for (DbIssues.Location.Builder locationBuilder : flowBuilder.getLocationBuilderList()) {
        if (locationBuilder.hasTextRange()) {
          hasTextRange = true;
          var componentUuid = defaultIfEmpty(locationBuilder.getComponentId(), issue.componentUuid());
          Component locationComponent = treeRootHolder.getComponentByUuid(componentUuid);
          locationsByComponent.computeIfAbsent(locationComponent, c -> new LinkedList<>()).add(new SecondaryLocation(locationBuilder));
        }
      }
    }

    return hasTextRange;
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

      if (lineNumber == textRange.getStartLine() && lineNumber == textRange.getEndLine()) {
        hashBuilder.append(line, textRange.getStartOffset(), textRange.getEndOffset());
      } else if (lineNumber == textRange.getStartLine()) {
        hashBuilder.append(line, textRange.getStartOffset(), line.length());
      } else if (lineNumber < textRange.getEndLine()) {
        hashBuilder.append(line);
      } else {
        hashBuilder.append(line, 0, textRange.getEndOffset());
      }
    }

    void afterAllLines() {
      String issueContentWithoutWhitespaces = MATCH_ALL_WHITESPACES.matcher(hashBuilder.toString()).replaceAll("");
      String hash = DigestUtils.md5Hex(issueContentWithoutWhitespaces);
      setHash(hash);
    }
  }
}
