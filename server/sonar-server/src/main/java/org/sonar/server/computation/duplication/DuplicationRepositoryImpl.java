/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.duplication;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.TreeRootHolder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

/**
 * In-memory implementation of {@link DuplicationRepository}.
 */
public class DuplicationRepositoryImpl implements DuplicationRepository {
  private final TreeRootHolder treeRootHolder;
  private final Map<String, Duplications> duplicationsByComponentUuid = new HashMap<>();

  public DuplicationRepositoryImpl(TreeRootHolder treeRootHolder) {
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public Set<Duplication> getDuplications(Component file) {
    checkFileComponentArgument(file);

    if (duplicationsByComponentUuid.containsKey(file.getUuid())) {
      return from(duplicationsByComponentUuid.get(file.getUuid()).getDuplicates())
        .transform(DuplicatesEntryToDuplication.INSTANCE)
        .toSet();
    }
    return Collections.emptySet();
  }

  @Override
  public void addDuplication(Component file, TextBlock original, TextBlock duplicate) {
    checkFileComponentArgument(file);
    checkOriginalTextBlockArgument(original);
    checkDuplicateTextBlockArgument(duplicate);
    checkArgument(!original.equals(duplicate), "original and duplicate TextBlocks can not be the same");

    Optional<Duplicates> duplicates = getDuplicates(file, original);
    if (duplicates.isPresent()) {
      checkDuplicationAlreadyExists(duplicates, file, original, duplicate);
      checkReverseDuplicationAlreadyExists(file, original, duplicate);

      duplicates.get().add(duplicate);
    } else {
      checkReverseDuplicationAlreadyExists(file, original, duplicate);
      getOrCreate(file, original).add(duplicate);
    }
  }

  @Override
  public void addDuplication(Component file, TextBlock original, Component otherFile, TextBlock duplicate) {
    checkFileComponentArgument(file);
    checkOriginalTextBlockArgument(original);
    checkComponentArgument(otherFile, "otherFile");
    checkDuplicateTextBlockArgument(duplicate);
    checkArgument(!file.equals(otherFile), "file and otherFile Components can not be the same");

    Optional<Duplicates> duplicates = getDuplicates(file, original);
    if (duplicates.isPresent()) {
      checkDuplicationAlreadyExists(duplicates, file, original, otherFile, duplicate);

      duplicates.get().add(otherFile, duplicate);
    } else {
      getOrCreate(file, original).add(otherFile, duplicate);
    }
  }

  @Override
  public void addDuplication(Component file, TextBlock original, String otherFileKey, TextBlock duplicate) {
    checkFileComponentArgument(file);
    checkOriginalTextBlockArgument(original);
    requireNonNull(otherFileKey, "otherFileKey can not be null");
    checkDuplicateTextBlockArgument(duplicate);
    checkArgument(!treeRootHolder.hasComponentWithKey(otherFileKey), "otherFileKey '%s' can not be the key of a Component in the project", otherFileKey);

    Optional<Duplicates> duplicates = getDuplicates(file, original);
    if (duplicates.isPresent()) {
      checkDuplicationAlreadyExists(duplicates, file, original, otherFileKey, duplicate);

      duplicates.get().add(otherFileKey, duplicate);
    } else {
      getOrCreate(file, original).add(otherFileKey, duplicate);
    }
  }

  private Optional<Duplicates> getDuplicates(Component file, TextBlock original) {
    Duplications duplications = duplicationsByComponentUuid.get(file.getUuid());
    if (duplications == null) {
      return Optional.absent();
    }
    return duplications.get(original);
  }

  private void checkDuplicationAlreadyExists(Optional<Duplicates> duplicates, Component file, TextBlock original, TextBlock duplicate) {
    checkArgument(!duplicates.get().hasDuplicate(duplicate),
      "Inner duplicate %s is already associated to original %s in file %s", duplicate, original, file.getKey());
  }

  private void checkReverseDuplicationAlreadyExists(Component file, TextBlock original, TextBlock duplicate) {
    Optional<Duplicates> reverseDuplication = getDuplicates(file, duplicate);
    if (reverseDuplication.isPresent()) {
      checkArgument(!reverseDuplication.get().hasDuplicate(original),
        "Inner duplicate %s is already associated to original %s in file %s", duplicate, original, file.getKey());
    }
  }

  private static void checkDuplicationAlreadyExists(Optional<Duplicates> duplicates, Component file, TextBlock original, Component otherFile, TextBlock duplicate) {
    checkArgument(!duplicates.get().hasDuplicate(otherFile, duplicate),
      "In-project duplicate %s in file %s is already associated to original %s in file %s", duplicate, otherFile.getKey(), original, file.getKey());
  }

  private static void checkDuplicationAlreadyExists(Optional<Duplicates> duplicates, Component file, TextBlock original, String otherFileKey, TextBlock duplicate) {
    checkArgument(!duplicates.get().hasDuplicate(otherFileKey, duplicate),
      "Cross-project duplicate %s in file %s is already associated to original %s in file %s", duplicate, otherFileKey, original, file.getKey());
  }

  private Duplicates getOrCreate(Component file, TextBlock original) {
    Duplications duplications = duplicationsByComponentUuid.get(file.getUuid());
    if (duplications == null) {
      duplications = new Duplications();
      duplicationsByComponentUuid.put(file.getUuid(), duplications);
    }

    return duplications.getOrCreate(original);
  }

  /**
   * Represents the location of the file of one or more duplicate {@link TextBlock}.
   */
  private interface DuplicateLocation {

  }

  private enum InnerDuplicationLocation implements DuplicateLocation {
    INSTANCE
  }

  @Immutable
  private static final class InProjectDuplicationLocation implements DuplicateLocation {
    private final Component component;

    public InProjectDuplicationLocation(Component component) {
      this.component = component;
    }

    public Component getComponent() {
      return component;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InProjectDuplicationLocation that = (InProjectDuplicationLocation) o;
      return component.equals(that.component);
    }

    @Override
    public int hashCode() {
      return component.hashCode();
    }
  }

  @Immutable
  private static final class CrossProjectDuplicationLocation implements DuplicateLocation {
    private final String fileKey;

    private CrossProjectDuplicationLocation(String fileKey) {
      this.fileKey = fileKey;
    }

    public String getFileKey() {
      return fileKey;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CrossProjectDuplicationLocation that = (CrossProjectDuplicationLocation) o;
      return fileKey.equals(that.fileKey);
    }

    @Override
    public int hashCode() {
      return fileKey.hashCode();
    }
  }

  private static final class Duplications {
    private final Map<TextBlock, Duplicates> duplicatesByTextBlock = new HashMap<>();

    public Duplicates getOrCreate(TextBlock textBlock) {
      if (duplicatesByTextBlock.containsKey(textBlock)) {
        return duplicatesByTextBlock.get(textBlock);
      }
      Duplicates res = new Duplicates();
      duplicatesByTextBlock.put(textBlock, res);
      return res;
    }

    public Set<Map.Entry<TextBlock, Duplicates>> getDuplicates() {
      return duplicatesByTextBlock.entrySet();
    }

    public Optional<Duplicates> get(TextBlock original) {
      return Optional.fromNullable(duplicatesByTextBlock.get(original));
    }
  }

  private static final class Duplicates {
    private final Map<DuplicateLocation, Set<TextBlock>> duplicatesByLocation = new HashMap<>();

    public Iterable<DuplicateWithLocation> getDuplicates() {
      return from(duplicatesByLocation.entrySet())
        .transformAndConcat(MapEntryToDuplicateWithLocation.INSTANCE);
    }

    public void add(TextBlock duplicate) {
      add(InnerDuplicationLocation.INSTANCE, duplicate);
    }

    public void add(Component otherFile, TextBlock duplicate) {
      InProjectDuplicationLocation key = new InProjectDuplicationLocation(otherFile);
      add(key, duplicate);
    }

    public void add(String otherFileKey, TextBlock duplicate) {
      CrossProjectDuplicationLocation key = new CrossProjectDuplicationLocation(otherFileKey);
      add(key, duplicate);
    }

    private void add(DuplicateLocation duplicateLocation, TextBlock duplicate) {
      Set<TextBlock> textBlocks = duplicatesByLocation.get(duplicateLocation);
      if (textBlocks == null) {
        textBlocks = new HashSet<>(1);
        duplicatesByLocation.put(duplicateLocation, textBlocks);
      }
      textBlocks.add(duplicate);
    }

    public boolean hasDuplicate(TextBlock duplicate) {
      return containsEntry(InnerDuplicationLocation.INSTANCE, duplicate);
    }

    public boolean hasDuplicate(Component otherFile, TextBlock duplicate) {
      return containsEntry(new InProjectDuplicationLocation(otherFile), duplicate);
    }

    public boolean hasDuplicate(String otherFileKey, TextBlock duplicate) {
      return containsEntry(new CrossProjectDuplicationLocation(otherFileKey), duplicate);
    }

    private boolean containsEntry(DuplicateLocation duplicateLocation, TextBlock duplicate) {
      Set<TextBlock> textBlocks = duplicatesByLocation.get(duplicateLocation);
      return textBlocks != null && textBlocks.contains(duplicate);
    }

    private enum MapEntryToDuplicateWithLocation implements Function<Map.Entry<DuplicateLocation, Set<TextBlock>>, Iterable<DuplicateWithLocation>> {
      INSTANCE;

      @Override
      @Nonnull
      public Iterable<DuplicateWithLocation> apply(@Nonnull final Map.Entry<DuplicateLocation, Set<TextBlock>> entry) {
        return from(entry.getValue())
          .transform(new Function<TextBlock, DuplicateWithLocation>() {
            @Override
            @Nonnull
            public DuplicateWithLocation apply(@Nonnull TextBlock input) {
              return new DuplicateWithLocation(entry.getKey(), input);
            }
          });
      }
    }
  }

  @Immutable
  private static final class DuplicateWithLocation {
    private final DuplicateLocation location;
    private final TextBlock duplicate;

    private DuplicateWithLocation(DuplicateLocation location, TextBlock duplicate) {
      this.location = location;
      this.duplicate = duplicate;
    }

    public DuplicateLocation getLocation() {
      return location;
    }

    public TextBlock getDuplicate() {
      return duplicate;
    }

  }

  private static void checkFileComponentArgument(Component file) {
    checkComponentArgument(file, "file");
  }

  private static void checkComponentArgument(Component file, String argName) {
    checkNotNull(file, "%s can not be null", argName);
    checkArgument(file.getType() == Component.Type.FILE, "type of %s argument must be FILE", argName);
  }

  private static void checkDuplicateTextBlockArgument(TextBlock duplicate) {
    requireNonNull(duplicate, "duplicate can not be null");
  }

  private static void checkOriginalTextBlockArgument(TextBlock original) {
    requireNonNull(original, "original can not be null");
  }

  private enum DuplicatesEntryToDuplication implements Function<Map.Entry<TextBlock, Duplicates>, Duplication> {
    INSTANCE;

    @Override
    @Nonnull
    public Duplication apply(@Nonnull Map.Entry<TextBlock, Duplicates> entry) {
      return new Duplication(
        entry.getKey(),
        from(entry.getValue().getDuplicates()).transform(DuplicateLocationEntryToDuplicate.INSTANCE));
    }

    private enum DuplicateLocationEntryToDuplicate implements Function<DuplicateWithLocation, Duplicate> {
      INSTANCE;

      @Override
      @Nonnull
      public Duplicate apply(@Nonnull DuplicateWithLocation input) {
        DuplicateLocation duplicateLocation = input.getLocation();
        if (duplicateLocation instanceof InnerDuplicationLocation) {
          return new InnerDuplicate(input.getDuplicate());
        }
        if (duplicateLocation instanceof InProjectDuplicationLocation) {
          return new InProjectDuplicate(((InProjectDuplicationLocation) duplicateLocation).getComponent(), input.getDuplicate());
        }
        if (duplicateLocation instanceof CrossProjectDuplicationLocation) {
          return new CrossProjectDuplicate(((CrossProjectDuplicationLocation) duplicateLocation).getFileKey(), input.getDuplicate());
        }
        throw new IllegalArgumentException("Unsupported DuplicationLocation type " + duplicateLocation.getClass());
      }
    }
  }
}
