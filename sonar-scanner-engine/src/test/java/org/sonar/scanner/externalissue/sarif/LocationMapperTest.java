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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.core.sarif.Location;
import org.sonar.core.sarif.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LocationMapperTest {

  private static final String TEST_MESSAGE = "test message";
  private static final String URI_TEST = "URI_TEST";
  private static final String EXPECTED_MESSAGE_URI_MISSING = "The field location.physicalLocation.artifactLocation.uri is not set.";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SensorContext sensorContext;

  @Mock
  private RegionMapper regionMapper;

  @InjectMocks
  private LocationMapper locationMapper;

  @Mock
  private NewIssueLocation newIssueLocation;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Result result;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Location location;

  @Mock
  private InputFile inputFile;

  @Before
  public void setup() {
    when(newIssueLocation.message(any())).thenReturn(newIssueLocation);
    when(newIssueLocation.on(any())).thenReturn(newIssueLocation);
    when(newIssueLocation.at(any())).thenReturn(newIssueLocation);
    when(sensorContext.project()).thenReturn(mock(InputProject.class));

    when(result.getMessage().getText()).thenReturn(TEST_MESSAGE);

    when(location.getPhysicalLocation().getArtifactLocation().getUri()).thenReturn(URI_TEST);
    when(sensorContext.fileSystem().inputFile(any(FilePredicate.class))).thenReturn(inputFile);
  }

  @Test
  public void isPredicate_whenDifferentFile_returnsFalse() {
    Path path = Paths.get("file");
    InputFile inputFile = mock(InputFile.class);
    when((inputFile.path())).thenReturn(Paths.get("file2"));
    LocationMapper.IsPredicate isPredicate = new LocationMapper.IsPredicate(path);
    assertThat(isPredicate.apply(inputFile)).isFalse();
  }

  @Test
  public void isPredicate_whenSameFile_returnsTrue() {
    Path path = Paths.get("file");
    InputFile inputFile = mock(InputFile.class);
    when((inputFile.path())).thenReturn(path);
    LocationMapper.IsPredicate isPredicate = new LocationMapper.IsPredicate(path);
    assertThat(isPredicate.apply(inputFile)).isTrue();
  }

  @Test
  public void fillIssueInFileLocation_whenFileNotFound_returnsNull() {
    when(sensorContext.fileSystem().inputFile(any())).thenReturn(null);

    NewIssueLocation actualIssueLocation = locationMapper.fillIssueInFileLocation(result, newIssueLocation, location);

    assertThat(actualIssueLocation).isNull();
  }

  @Test
  public void fillIssueInFileLocation_whenMapRegionReturnsNull_onlyFillsSimpleFields() {
    when(regionMapper.mapRegion(location.getPhysicalLocation().getRegion(), inputFile))
      .thenReturn(Optional.empty());

    NewIssueLocation actualIssueLocation = locationMapper.fillIssueInFileLocation(result, newIssueLocation, location);

    assertThat(actualIssueLocation).isSameAs(newIssueLocation);
    verify(newIssueLocation).message(TEST_MESSAGE);
    verify(newIssueLocation).on(inputFile);
    verifyNoMoreInteractions(newIssueLocation);
  }

  @Test
  public void fillIssueInFileLocation_whenMapRegionReturnsRegion_callsAt() {
    TextRange textRange = mock(TextRange.class);
    when(regionMapper.mapRegion(location.getPhysicalLocation().getRegion(), inputFile))
      .thenReturn(Optional.of(textRange));

    NewIssueLocation actualIssueLocation = locationMapper.fillIssueInFileLocation(result, newIssueLocation, location);

    assertThat(actualIssueLocation).isSameAs(newIssueLocation);
    verify(newIssueLocation).message(TEST_MESSAGE);
    verify(newIssueLocation).on(inputFile);
    verify(newIssueLocation).at(textRange);
    verifyNoMoreInteractions(newIssueLocation);
  }

  @Test
  public void fillIssueInFileLocation_ifNullUri_throws() {
    when(location.getPhysicalLocation().getArtifactLocation().getUri()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> locationMapper.fillIssueInFileLocation(result, newIssueLocation, location))
      .withMessage(EXPECTED_MESSAGE_URI_MISSING);
  }

  @Test
  public void fillIssueInFileLocation_ifNullArtifactLocation_throws() {
    when(location.getPhysicalLocation().getArtifactLocation()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> locationMapper.fillIssueInFileLocation(result, newIssueLocation, location))
      .withMessage(EXPECTED_MESSAGE_URI_MISSING);
  }

  @Test
  public void fillIssueInFileLocation_ifNullPhysicalLocation_throws() {
    when(location.getPhysicalLocation().getArtifactLocation()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> locationMapper.fillIssueInFileLocation(result, newIssueLocation, location))
      .withMessage(EXPECTED_MESSAGE_URI_MISSING);
  }

}
