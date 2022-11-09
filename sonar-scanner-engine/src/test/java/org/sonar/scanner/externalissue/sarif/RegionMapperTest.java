/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.core.sarif.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegionMapperTest {
  private static final int LINE_END_OFFSET = 10;
  private static final DefaultInputFile INPUT_FILE = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), "relative/path", null),
    f -> f.setMetadata(generateMetadata()));


  private static Metadata generateMetadata() {
    Metadata metadata = mock(Metadata.class);
    when(metadata.lines()).thenReturn(100);
    when(metadata.originalLineStartOffsets()).thenReturn(IntStream.range(0, 100).toArray());
    when(metadata.originalLineEndOffsets()).thenReturn(IntStream.range(0, 100).map(i -> i + LINE_END_OFFSET).toArray());
    return metadata;
  }

  @Mock
  private Region region;

  @InjectMocks
  private RegionMapper regionMapper;

  @Test
  public void mapRegion_whenNullRegion_returnsEmpty() {
    assertThat(regionMapper.mapRegion(null, INPUT_FILE)).isEmpty();
  }

  @Test
  public void mapRegion_whenStartLineIsNull_shouldThrow() {
    when(region.getStartLine()).thenReturn(null);

    assertThatNullPointerException()
      .isThrownBy(() -> regionMapper.mapRegion(region, INPUT_FILE))
      .withMessage("No start line defined for the region.");
  }

  @Test
  public void mapRegion_whenAllCoordinatesDefined() {
    Region fullRegion = mockRegion(1, 2, 3, 4);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isEqualTo(fullRegion.getStartColumn());
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(fullRegion.getEndColumn());
  }

  @Test
  public void mapRegion_whenStartEndLinesDefined() {
    Region fullRegion = mockRegion(null, null, 3, 8);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isEqualTo(1);
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(LINE_END_OFFSET);
  }

  @Test
  public void mapRegion_whenStartEndLinesDefinedAndStartColumn() {
    Region fullRegion = mockRegion(8, null, 3, 8);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isEqualTo(fullRegion.getStartColumn());
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(LINE_END_OFFSET);
  }

  @Test
  public void mapRegion_whenStartEndLinesDefinedAndEndColumn() {
    Region fullRegion = mockRegion(null, 8, 3, 8);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isEqualTo(1);
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(fullRegion.getEndLine());
  }

  private static Region mockRegion(@Nullable Integer startColumn, @Nullable Integer endColumn, @Nullable Integer startLine, @Nullable Integer endLine) {
    Region region = mock(Region.class);
    when(region.getStartColumn()).thenReturn(startColumn);
    when(region.getEndColumn()).thenReturn(endColumn);
    when(region.getStartLine()).thenReturn(startLine);
    when(region.getEndLine()).thenReturn(endLine);
    return region;
  }

}
