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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.core.sarif.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class RegionMapperTest {
  private static final int LINE_END_OFFSET = 10;
  private static final DefaultInputFile INPUT_FILE = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), "relative/path", null),
    f -> f.setMetadata(generateMetadata()), f -> {
  });

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

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

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
  @UseDataProvider("index")
  public void mapRegion_whenColumnsDefined_convert1BasedIndexTo0BasedIndex(int startColumn, int endColumn, int startOffsetExpected, int endOffsetExpected) {
    Region fullRegion = mockRegion(startColumn, endColumn, 3, 4);

    TextRange textRange = regionMapper.mapRegion(fullRegion, INPUT_FILE).orElseGet(() -> fail("No TextRange"));

    assertThat(textRange.start().lineOffset()).isEqualTo(startOffsetExpected);
    assertThat(textRange.end().lineOffset()).isEqualTo(endOffsetExpected);
  }

  @DataProvider
  public static Object[][] index() {
    return new Object[][] {
      {1, 3, 0, 2},
      {5, 8, 4, 7}
    };
  }

  @Test
  public void mapRegion_whenAllCoordinatesDefined() {
    Region fullRegion = mockRegion(1, 2, 3, 4);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isZero();
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(1);
  }

  @Test
  public void mapRegion_whenStartEndLinesDefined() {
    Region fullRegion = mockRegion(null, null, 3, 8);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isZero();
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
    assertThat(textRange.start().lineOffset()).isEqualTo(7);
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
    assertThat(textRange.start().lineOffset()).isZero();
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(7);
  }

  @Test
  public void mapRegion_whenRangeIsEmpty_shouldSelectWholeLine() {
    Region fullRegion = mockRegion(8, 8, 3, 3);

    Optional<TextRange> optTextRange = regionMapper.mapRegion(fullRegion, INPUT_FILE);

    assertThat(optTextRange).isPresent();
    TextRange textRange = optTextRange.get();
    assertThat(textRange.start().line()).isEqualTo(fullRegion.getStartLine());
    assertThat(textRange.start().lineOffset()).isZero();
    assertThat(textRange.end().line()).isEqualTo(fullRegion.getEndLine());
    assertThat(textRange.end().lineOffset()).isEqualTo(LINE_END_OFFSET);
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
