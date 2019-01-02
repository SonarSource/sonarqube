/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.duplication;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentProvider;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderComponentProvider;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class DuplicationRepositoryRule extends ExternalResource implements DuplicationRepository {
  @CheckForNull
  private final ComponentProvider componentProvider;
  private DuplicationRepositoryImpl delegate;
  private final Multimap<Component, TextBlock> componentRefsWithInnerDuplications = ArrayListMultimap.create();
  private final Multimap<Component, TextBlock> componentRefsWithInProjectDuplications = ArrayListMultimap.create();
  private final Multimap<Component, TextBlock> componentRefsWithCrossProjectDuplications = ArrayListMultimap.create();

  private DuplicationRepositoryRule(TreeRootHolder treeRootHolder) {
    this.componentProvider = new TreeRootHolderComponentProvider(treeRootHolder);
  }

  public DuplicationRepositoryRule() {
    this.componentProvider = null;
  }

  public static DuplicationRepositoryRule create(TreeRootHolderRule treeRootHolder) {
    return new DuplicationRepositoryRule(treeRootHolder);
  }

  public static DuplicationRepositoryRule create() {
    return new DuplicationRepositoryRule();
  }

  @Override
  protected void before() {
    this.delegate = new DuplicationRepositoryImpl();
  }

  @Override
  protected void after() {
    if (this.componentProvider != null) {
      this.componentProvider.reset();
    }
    this.componentRefsWithInnerDuplications.clear();
    this.componentRefsWithInProjectDuplications.clear();
    this.componentRefsWithCrossProjectDuplications.clear();
    this.delegate = null;
  }

  public Iterable<Duplication> getDuplications(int fileRef) {
    ensureComponentProviderInitialized();

    return delegate.getDuplications(componentProvider.getByRef(fileRef));
  }

  public void add(int fileRef, Duplication duplication) {
    ensureComponentProviderInitialized();

    delegate.add(componentProvider.getByRef(fileRef), duplication);
  }

  public DuplicationRepositoryRule addDuplication(int fileRef, TextBlock original, TextBlock... duplicates) {
    ensureComponentProviderInitialized();
    Component component = componentProvider.getByRef(fileRef);
    checkArgument(!componentRefsWithInnerDuplications.containsEntry(component, original), "Inner duplications for file %s and original %s already set", fileRef, original);
    checkArgument(!componentRefsWithInProjectDuplications.containsEntry(component, original),
      "InProject duplications for file %s and original %s already set. Use add(int, Duplication) instead", fileRef, original);

    componentRefsWithInnerDuplications.put(component, original);
    delegate.add(
      component,
      new Duplication(
        original,
        from(Arrays.asList(duplicates)).transform(TextBlockToInnerDuplicate.INSTANCE)));

    return this;
  }

  public DuplicationRepositoryRule addDuplication(int fileRef, TextBlock original, int otherFileRef, TextBlock duplicate) {
    ensureComponentProviderInitialized();
    Component component = componentProvider.getByRef(fileRef);
    checkArgument(!componentRefsWithInProjectDuplications.containsEntry(component, original), "InProject duplications for file %s and original %s already set", fileRef, original);
    checkArgument(!componentRefsWithInnerDuplications.containsEntry(component, original),
      "Inner duplications for file %s and original %s already set. Use add(int, Duplication) instead", fileRef, original);

    componentRefsWithInProjectDuplications.put(component, original);
    delegate.add(component,
      new Duplication(
        original,
        Collections.singletonList(new InProjectDuplicate(componentProvider.getByRef(otherFileRef), duplicate))));

    return this;
  }

  public DuplicationRepositoryRule addExtendedProjectDuplication(int fileRef, TextBlock original, int otherFileRef, TextBlock duplicate) {
    ensureComponentProviderInitialized();
    Component component = componentProvider.getByRef(fileRef);
    checkArgument(!componentRefsWithCrossProjectDuplications.containsEntry(component, original), "CrossProject duplications for file %s and original %s already set", fileRef);

    componentRefsWithCrossProjectDuplications.put(component, original);
    delegate.add(componentProvider.getByRef(fileRef),
      new Duplication(
        original,
        Collections.singletonList(new InExtendedProjectDuplicate(componentProvider.getByRef(otherFileRef), duplicate))));

    return this;
  }

  public DuplicationRepositoryRule addCrossProjectDuplication(int fileRef, TextBlock original, String otherFileKey, TextBlock duplicate) {
    ensureComponentProviderInitialized();
    Component component = componentProvider.getByRef(fileRef);
    checkArgument(!componentRefsWithCrossProjectDuplications.containsEntry(component, original), "CrossProject duplications for file %s and original %s already set", fileRef);

    componentRefsWithCrossProjectDuplications.put(component, original);
    delegate.add(componentProvider.getByRef(fileRef),
      new Duplication(
        original,
        Collections.singletonList(new CrossProjectDuplicate(otherFileKey, duplicate))));

    return this;
  }

  @Override
  public Iterable<Duplication> getDuplications(Component file) {
    return delegate.getDuplications(file);
  }

  @Override
  public void add(Component file, Duplication duplication) {
    TextBlock original = duplication.getOriginal();
    checkArgument(!componentRefsWithInnerDuplications.containsEntry(file, original), "Inner duplications for file %s and original %s already set", file, original);
    checkArgument(!componentRefsWithInProjectDuplications.containsEntry(file, original), "InProject duplications for file %s and original %s already set", file, original);
    checkArgument(!componentRefsWithCrossProjectDuplications.containsEntry(file, original), "CrossProject duplications for file %s and original %s already set", file, original);

    delegate.add(file, duplication);
  }

  private void ensureComponentProviderInitialized() {
    requireNonNull(this.componentProvider, "Methods with component reference can not be used unless a TreeRootHolder has been provided when instantiating the rule");
    this.componentProvider.ensureInitialized();
  }

  private enum TextBlockToInnerDuplicate implements Function<TextBlock, Duplicate> {
    INSTANCE;

    @Override
    @Nonnull
    public Duplicate apply(@Nonnull TextBlock input) {
      return new InnerDuplicate(input);
    }
  }
}
