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
package org.sonar.scanner.externalissue.sarif;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.predicates.AbstractFilePredicate;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.ArtifactLocation;
import org.sonar.core.sarif.Location;
import org.sonar.core.sarif.PhysicalLocation;
import org.sonar.core.sarif.Result;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;

@ScannerSide
public class LocationMapper {

  private final SensorContext sensorContext;
  private final RegionMapper regionMapper;

  LocationMapper(SensorContext sensorContext, RegionMapper regionMapper) {
    this.sensorContext = sensorContext;
    this.regionMapper = regionMapper;
  }

  NewIssueLocation fillIssueInProjectLocation(Result result, NewIssueLocation newIssueLocation) {
    return newIssueLocation
      .message(getResultMessageOrThrow(result))
      .on(sensorContext.project());
  }

  @CheckForNull
  NewIssueLocation fillIssueInFileLocation(Result result, NewIssueLocation newIssueLocation, Location location) {
    newIssueLocation.message(getResultMessageOrThrow(result));
    PhysicalLocation physicalLocation = location.getPhysicalLocation();

    String fileUri = getFileUriOrThrow(location);
    InputFile file = findFile(sensorContext, fileUri);
    if (file == null) {
      return null;
    }
    newIssueLocation.on(file);
    regionMapper.mapRegion(physicalLocation.getRegion(), file).ifPresent(newIssueLocation::at);
    return newIssueLocation;
  }

  private static String getResultMessageOrThrow(Result result) {
    requireNonNull(result.getMessage(), "No messages found for issue thrown by rule " + result.getRuleId());
    return requireNonNull(result.getMessage().getText(), "No text found for messages in issue thrown by rule " + result.getRuleId());
  }

  private static String getFileUriOrThrow(Location location) {
    PhysicalLocation physicalLocation = location.getPhysicalLocation();
    checkArgument(hasUriFieldPopulated(physicalLocation), "The field location.physicalLocation.artifactLocation.uri is not set.");
    return physicalLocation.getArtifactLocation().getUri();
  }

  private static boolean hasUriFieldPopulated(@Nullable PhysicalLocation location) {
    return Optional.ofNullable(location).map(PhysicalLocation::getArtifactLocation).map(ArtifactLocation::getUri).isPresent();
  }

  @CheckForNull
  private static InputFile findFile(SensorContext context, String filePath) {
    // we use a custom predicate (which is not optimized) because fileSystem().predicates().is() doesn't handle symlinks correctly
    return context.fileSystem().inputFile(new IsPredicate(getFileFromAbsoluteUriOrPath(filePath).toPath()));
  }

  private static File getFileFromAbsoluteUriOrPath(String filePath) {
    URI uri = URI.create(filePath);
    if (uri.isAbsolute()) {
      return new File(uri);
    } else {
      return new File(filePath);
    }
  }

  @VisibleForTesting
  static class IsPredicate extends AbstractFilePredicate {
    private final Path path;

    public IsPredicate(Path path) {
      this.path = path;
    }

    @Override
    public boolean apply(InputFile inputFile) {
      try {
        return Files.isSameFile(path, inputFile.path());
      } catch (IOException e) {
        return false;
      }
    }
  }
}
