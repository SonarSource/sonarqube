/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protobuf.utils.CloseableIterator;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * This service will update the locations field of issues, by filling hashes for their locations:
 * - Primary location hash: for all issues, when needed (ie. is missing or the issue is new/updated)
 * - Secondary location hash: only for taint vulnerabilities and security hotspots, when needed (the issue is new/updated)
 * For performance reasons, it will read each source code file once and feed the lines to all locations in that file.
 */
public class LocationHashesService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LocationHashesService.class);

  private static final Pattern MATCH_ALL_WHITESPACES = Pattern.compile("\\s");

  private static final Predicate<DefaultIssue> issueNeedsLocationHashes = issue -> {
    DbIssues.Locations locations = issue.getLocations();
    return !issue.isFromExternalRuleEngine()
      && !issue.isBeingClosed()
      && locations != null;
  };

  private static final Predicate<DefaultIssue> shouldComputePrimaryHashesForIssues = issue -> {
    DbIssues.Locations locations = issue.getLocations();
    return locations.hasTextRange() && !locations.hasChecksum();
  };

  private final SourceLinesRepository sourceLinesRepository;
  private final TreeRootHolder treeRootHolder;
  private final TaintChecker taintChecker;

  public LocationHashesService(TaintChecker taintChecker, SourceLinesRepository sourceLinesRepository, TreeRootHolder treeRootHolder) {
    this.taintChecker = taintChecker;
    this.sourceLinesRepository = sourceLinesRepository;
    this.treeRootHolder = treeRootHolder;
  }

  public void computeHashesAndUpdateIssues(Collection<DefaultIssue> newIssuesOrLocationsUpdated, Collection<DefaultIssue> otherIssues, Component component) {
    if (newIssuesOrLocationsUpdated.isEmpty() && otherIssues.isEmpty()) {
      return;
    }

    List<DefaultIssue> issuesForAllLocations = new ArrayList<>();
    List<DefaultIssue> issuesForPrimaryLocation = new ArrayList<>();

    newIssuesOrLocationsUpdated.stream().filter(issueNeedsLocationHashes).forEach(issue -> {
      if (taintChecker.isTaintVulnerability(issue)) {
        issuesForAllLocations.add(issue);
      } else {
        issuesForPrimaryLocation.add(issue);
      }
    });

    issuesForPrimaryLocation.addAll(otherIssues.stream().filter(issueNeedsLocationHashes).filter(shouldComputePrimaryHashesForIssues)
      // Issues in this situation are not necessarily marked as changed, so we do it to ensure persistence
      .map(issue -> issue.setChanged(true))
      .toList());

    computeAndUpdateLocationHashes(component, issuesForAllLocations, issuesForPrimaryLocation);
  }

  private void computeAndUpdateLocationHashes(Component component, List<DefaultIssue> issuesForAllLocations, List<DefaultIssue> issuesForPrimaryLocation) {
    Map<Component, List<Location>> locationsByComponent = new HashMap<>();
    List<LocationToSet> locationsToSet = new LinkedList<>();

    // Issues that needs both primary and secondary locations hashes
    extractForAllLocations(component, locationsByComponent, locationsToSet, issuesForAllLocations);
    // Then issues that needs only primary locations
    extractForPrimaryLocation(component, locationsByComponent, locationsToSet, issuesForPrimaryLocation);

    // Feed lines to locations, component by component
    locationsByComponent.forEach(this::updateLocationsInComponent);

    // Finalize by setting hashes 
    locationsByComponent.values().forEach(list -> list.forEach(Location::afterAllLines));

    // set new locations to issues
    locationsToSet.forEach(LocationToSet::set);
  }

  private void extractForAllLocations(Component component, Map<Component, List<Location>> locationsByComponent, List<LocationToSet> locationsToSet,
    List<DefaultIssue> issuesForAllLocations) {
    for (DefaultIssue issue : issuesForAllLocations) {
      DbIssues.Locations.Builder locationsBuilder = ((DbIssues.Locations) issue.getLocations()).toBuilder();
      addPrimaryLocation(component, locationsByComponent, locationsBuilder);
      addSecondaryLocations(issue, locationsByComponent, locationsBuilder);
      locationsToSet.add(new LocationToSet(issue, locationsBuilder));
    }
  }

  private static void extractForPrimaryLocation(Component component, Map<Component, List<Location>> locationsByComponent, List<LocationToSet> locationsToSet,
    List<DefaultIssue> issuesForPrimaryLocation) {
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
    private static final int MAX_DIGEST_CHUNK_CHARS = 8 * 1024;

    /**
     * MD5 digest fed incrementally as source-range segments are read, so that we never retain the full location content.
     */
    private final MessageDigest digest = DigestUtils.getMd5Digest();
    /**
     * Bounded buffer of non-whitespace characters awaiting a flush to {@link #digest}. Its trailing character is kept
     * when it is a lone high surrogate, so a surrogate pair split across chunks or segments is encoded as a single code
     * point - exactly as the legacy "concatenate everything, then getBytes(UTF_8)" path did.
     */
    private final StringBuilder chunk = new StringBuilder(MAX_DIGEST_CHUNK_CHARS + 1);

    abstract DbCommons.TextRange getTextRange();

    abstract void setHash(String hash);

    public void processLine(int lineNumber, String line) {
      DbCommons.TextRange textRange = getTextRange();
      if (lineNumber > textRange.getEndLine() || lineNumber < textRange.getStartLine()) {
        return;
      }

      int start;
      int end;
      if (lineNumber == textRange.getStartLine() && lineNumber == textRange.getEndLine()) {
        start = textRange.getStartOffset();
        end = textRange.getEndOffset();
      } else if (lineNumber == textRange.getStartLine()) {
        start = textRange.getStartOffset();
        end = line.length();
      } else if (lineNumber < textRange.getEndLine()) {
        start = 0;
        end = line.length();
      } else {
        start = 0;
        end = textRange.getEndOffset();
      }

      // Same bounds validation as the legacy StringBuilder.append(line, start, end) calls: on an invalid range we skip
      // the whole segment (digest nothing) and log, preserving the previous behavior byte-for-byte.
      if (start < 0 || start > end || end > line.length()) {
        LOGGER.debug("Try to compute issue location hash from {} to {} on line ({} chars): {}",
          textRange.getStartOffset(), textRange.getEndOffset(), line.length(), line);
        return;
      }

      updateDigestWithoutWhitespace(line, start, end);
    }

    /**
     * Feeds {@code line[start, end)} into the digest, discarding exactly the characters matched by
     * {@link #MATCH_ALL_WHITESPACES} (the compatibility definition of whitespace) and buffering the rest in a
     * fixed-size chunk. Uses the pattern directly over a matcher region so no per-segment copy of the source is made.
     */
    private void updateDigestWithoutWhitespace(String line, int start, int end) {
      Matcher whitespaceMatcher = MATCH_ALL_WHITESPACES.matcher(line).region(start, end);
      int cursor = start;
      while (whitespaceMatcher.find()) {
        appendToChunk(line, cursor, whitespaceMatcher.start());
        cursor = whitespaceMatcher.end();
      }
      appendToChunk(line, cursor, end);
    }

    private void appendToChunk(String line, int from, int to) {
      while (from < to) {
        int space = MAX_DIGEST_CHUNK_CHARS - chunk.length();
        int take = Math.min(to - from, space);
        chunk.append(line, from, from + take);
        from += take;
        if (chunk.length() >= MAX_DIGEST_CHUNK_CHARS) {
          flushChunk(false);
        }
      }
    }

    private void flushChunk(boolean flushAll) {
      int length = chunk.length();
      if (length == 0) {
        return;
      }
      // Never flush a trailing high surrogate on its own - keep it for the next chunk so its low surrogate can join it.
      if (!flushAll && Character.isHighSurrogate(chunk.charAt(length - 1))) {
        length--;
        if (length == 0) {
          return;
        }
      }
      digest.update(chunk.substring(0, length).getBytes(StandardCharsets.UTF_8));
      chunk.delete(0, length);
    }

    void afterAllLines() {
      flushChunk(true);
      setHash(Hex.encodeHexString(digest.digest()));
    }
  }
}
