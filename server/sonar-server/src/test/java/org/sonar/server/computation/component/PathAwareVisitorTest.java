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
package org.sonar.server.computation.component;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class PathAwareVisitorTest {

  private static final int ROOT_REF = 1;
  private static final DumbComponent SOME_TREE_ROOT = DumbComponent.builder(PROJECT, ROOT_REF)
    .addChildren(
      DumbComponent.builder(MODULE, 11)
        .addChildren(
          DumbComponent.builder(DIRECTORY, 111)
            .addChildren(
              DumbComponent.builder(FILE, 1111).build(),
              DumbComponent.builder(FILE, 1112).build()
            )
            .build(),
          DumbComponent.builder(DIRECTORY, 112)
            .addChildren(
              DumbComponent.builder(FILE, 1121).build()
            )
            .build())
        .build(),
      DumbComponent.builder(MODULE, 12)
        .addChildren(
          DumbComponent.builder(DIRECTORY, 121)
            .addChildren(
              DumbComponent.builder(FILE, 1211).build()
            )
            .build()
        ).build()
    ).build();

  @Test
  public void verify_preOrder() {
    TestPathAwareVisitor underTest = new TestPathAwareVisitor(PRE_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitDirectory", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitFile", 1211, 121, of(1211, 121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder() {
    TestPathAwareVisitor underTest = new TestPathAwareVisitor(POST_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitFile", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitDirectory", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  private static void verifyCallRecords(Iterator<CallRecord> expected, Iterator<CallRecord> actual) {
    while (expected.hasNext()) {
      assertThat(actual.next()).isEqualTo(expected.next());
    }
  }

  private static CallRecord newCallRecord(String method, int currentRef, @Nullable Integer parentRef, List<Integer> path) {
    return new CallRecord(method, currentRef, currentRef, parentRef, ROOT_REF, path);
  }

  private static class TestPathAwareVisitor extends PathAwareVisitor<Integer> {
    private final List<CallRecord> callsRecords = new ArrayList<>();

    public TestPathAwareVisitor(ComponentVisitor.Order order) {
      super(FILE, order, new SimpleStackElementFactory<Integer>() {
        @Override
        public Integer createForAny(Component component) {
          return component.getRef();
        }
      });
    }

    @Override
    protected void visitProject(Component project, Path<Integer> path) {
      callsRecords.add(newCallRecord(project, path, "visitProject"));
    }

    @Override
    protected void visitModule(Component module, Path<Integer> path) {
      callsRecords.add(newCallRecord(module, path, "visitModule"));
    }

    @Override
    protected void visitDirectory(Component directory, Path<Integer> path) {
      callsRecords.add(newCallRecord(directory, path, "visitDirectory"));
    }

    @Override
    protected void visitFile(Component file, Path<Integer> path) {
      callsRecords.add(newCallRecord(file, path, "visitFile"));
    }

    @Override
    protected void visitUnknown(Component unknownComponent, Path<Integer> path) {
      callsRecords.add(newCallRecord(unknownComponent, path, "visitUnknown"));
    }

    @Override
    protected void visitAny(Component component, Path<Integer> path) {
      callsRecords.add(newCallRecord(component, path, "visitAny"));
    }

    private static CallRecord newCallRecord(Component project, Path<Integer> path, String method) {
      return new CallRecord(method, project.getRef(), path.current(), getParent(path), path.root(),
        toValueList(path));
    }

    private static List<Integer> toValueList(Path<Integer> path) {
      return from(path.getCurrentPath()).transform(new Function<PathElement<Integer>, Integer>() {
        @Nonnull
        @Override
        public Integer apply(@Nonnull PathElement<Integer> input) {
          return input.getElement();
        }
      }).toList();
    }

    private static Integer getParent(Path<Integer> path) {
      try {
        Integer parent = path.parent();
        checkArgument(parent != null, "Path.parent returned a null value!");
        return parent;
      } catch (NoSuchElementException e) {
        return null;
      }
    }
  }

  private static class CallRecord {
    private final String method;
    private final int ref;
    private final int current;
    @CheckForNull
    private final Integer parent;
    private final int root;
    private final List<Integer> path;

    private CallRecord(String method, int ref, int current, @Nullable Integer parent, int root, List<Integer> path) {
      this.method = method;
      this.ref = ref;
      this.current = current;
      this.parent = parent;
      this.root = root;
      this.path = path;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CallRecord that = (CallRecord) o;
      return Objects.equals(ref, that.ref) &&
        Objects.equals(current, that.current) &&
        Objects.equals(root, that.root) &&
        Objects.equals(method, that.method) &&
        Objects.equals(parent, that.parent) &&
        Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, ref, current, parent, root, path);
    }

    @Override
    public String toString() {
      return "{" +
        "method='" + method + '\'' +
        ", ref=" + ref +
        ", current=" + current +
        ", parent=" + parent +
        ", root=" + root +
        ", path=" + path +
        '}';
    }
  }

}
