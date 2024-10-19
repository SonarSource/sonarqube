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
package org.sonar.ce.task.projectanalysis.component;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.LoggerFactory;
import org.sonar.core.util.logs.Profiler;

import static com.google.common.collect.Iterables.concat;
import static java.util.Objects.requireNonNull;

/**
 * This crawler make any number of {@link TypeAwareVisitor} or {@link PathAwareVisitor} defined in a list visit a component tree, component per component, in the order of the list
 */
public class VisitorsCrawler implements ComponentCrawler {

  private final boolean computeDuration;
  private final Map<ComponentVisitor, VisitorDuration> visitorCumulativeDurations;
  private final List<VisitorWrapper> preOrderVisitorWrappers;
  private final List<VisitorWrapper> postOrderVisitorWrappers;

  public VisitorsCrawler(Collection<ComponentVisitor> visitors) {
    this(visitors, false);
  }

  public VisitorsCrawler(Collection<ComponentVisitor> visitors, boolean computeDuration) {
    List<VisitorWrapper> visitorWrappers = visitors.stream().map(ToVisitorWrapper.INSTANCE).toList();
    this.preOrderVisitorWrappers = visitorWrappers.stream().filter(MathPreOrderVisitor.INSTANCE).toList();
    this.postOrderVisitorWrappers = visitorWrappers.stream().filter(MatchPostOrderVisitor.INSTANCE).toList();
    this.computeDuration = computeDuration;
    this.visitorCumulativeDurations = computeDuration ? visitors.stream().collect(Collectors.toMap(v -> v, v -> new VisitorDuration())) : Collections.emptyMap();
  }

  public Map<ComponentVisitor, Long> getCumulativeDurations() {
    if (computeDuration) {
      return visitorCumulativeDurations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDuration()));
    }
    return Collections.emptyMap();
  }

  @Override
  public void visit(final Component component) {
    try {
      visitImpl(component);
    } catch (RuntimeException e) {
      VisitException.rethrowOrWrap(
        e,
        "Visit of Component {key=%s,type=%s} failed",
        component.getKey(), component.getType());
    }
  }

  private void visitImpl(Component component) {
    MatchVisitorMaxDepth visitorMaxDepth = MatchVisitorMaxDepth.forComponent(component);
    List<VisitorWrapper> preOrderVisitorWrappersToExecute = preOrderVisitorWrappers.stream().filter(visitorMaxDepth).toList();
    List<VisitorWrapper> postOrderVisitorWrappersToExecute = postOrderVisitorWrappers.stream().filter(visitorMaxDepth).toList();
    if (preOrderVisitorWrappersToExecute.isEmpty() && postOrderVisitorWrappersToExecute.isEmpty()) {
      return;
    }

    for (VisitorWrapper visitorWrapper : concat(preOrderVisitorWrappers, postOrderVisitorWrappers)) {
      visitorWrapper.beforeComponent(component);
    }

    for (VisitorWrapper visitorWrapper : preOrderVisitorWrappersToExecute) {
      visitNode(component, visitorWrapper);
    }

    visitChildren(component);

    for (VisitorWrapper visitorWrapper : postOrderVisitorWrappersToExecute) {
      visitNode(component, visitorWrapper);
    }

    for (VisitorWrapper visitorWrapper : concat(preOrderVisitorWrappersToExecute, postOrderVisitorWrappersToExecute)) {
      visitorWrapper.afterComponent(component);
    }
  }

  private void visitChildren(Component component) {
    for (Component child : component.getChildren()) {
      visit(child);
    }
  }

  private void visitNode(Component component, VisitorWrapper visitor) {
    Profiler profiler = Profiler.create(LoggerFactory.getLogger(visitor.getWrappedVisitor().getClass()))
      .startTrace("Visiting component {}", component.getKey());
    visitor.visitAny(component);
    switch (component.getType()) {
      case PROJECT:
        visitor.visitProject(component);
        break;
      case DIRECTORY:
        visitor.visitDirectory(component);
        break;
      case FILE:
        visitor.visitFile(component);
        break;
      case VIEW:
        visitor.visitView(component);
        break;
      case SUBVIEW:
        visitor.visitSubView(component);
        break;
      case PROJECT_VIEW:
        visitor.visitProjectView(component);
        break;
      default:
        throw new IllegalStateException(String.format("Unknown type %s", component.getType().name()));
    }
    long duration = profiler.stopTrace();
    incrementDuration(visitor, duration);
  }

  private void incrementDuration(VisitorWrapper visitorWrapper, long duration) {
    if (computeDuration) {
      visitorCumulativeDurations.get(visitorWrapper.getWrappedVisitor()).increment(duration);
    }
  }

  private enum ToVisitorWrapper implements Function<ComponentVisitor, VisitorWrapper> {
    INSTANCE;

    @Override
    public VisitorWrapper apply(@Nonnull ComponentVisitor componentVisitor) {
      if (componentVisitor instanceof TypeAwareVisitor typeAwareVisitor) {
        return new TypeAwareVisitorWrapper(typeAwareVisitor);
      } else if (componentVisitor instanceof PathAwareVisitor<?> pathAwareVisitor) {
        return new PathAwareVisitorWrapper(pathAwareVisitor);
      } else {
        throw new IllegalArgumentException("Only TypeAwareVisitor and PathAwareVisitor can be used");
      }
    }
  }

  private static class MatchVisitorMaxDepth implements Predicate<VisitorWrapper> {
    private static final Map<Component.Type, MatchVisitorMaxDepth> INSTANCES = buildInstances();
    private final Component.Type type;

    private MatchVisitorMaxDepth(Component.Type type) {
      this.type = requireNonNull(type);
    }

    private static Map<Component.Type, MatchVisitorMaxDepth> buildInstances() {
      ImmutableMap.Builder<Component.Type, MatchVisitorMaxDepth> builder = ImmutableMap.builder();
      for (Component.Type type : Component.Type.values()) {
        builder.put(type, new MatchVisitorMaxDepth(type));
      }
      return builder.build();
    }

    public static MatchVisitorMaxDepth forComponent(Component component) {
      return INSTANCES.get(component.getType());
    }

    @Override
    public boolean test(VisitorWrapper visitorWrapper) {
      CrawlerDepthLimit maxDepth = visitorWrapper.getMaxDepth();
      return maxDepth.isSameAs(type) || maxDepth.isDeeperThan(type);
    }
  }

  private enum MathPreOrderVisitor implements Predicate<VisitorWrapper> {
    INSTANCE;

    @Override
    public boolean test(VisitorWrapper visitorWrapper) {
      return visitorWrapper.getOrder() == ComponentVisitor.Order.PRE_ORDER;
    }
  }

  private enum MatchPostOrderVisitor implements Predicate<VisitorWrapper> {
    INSTANCE;

    @Override
    public boolean test(VisitorWrapper visitorWrapper) {
      return visitorWrapper.getOrder() == ComponentVisitor.Order.POST_ORDER;
    }
  }

  private static final class VisitorDuration {
    private long duration = 0;

    public void increment(long duration) {
      this.duration += duration;
    }

    public long getDuration() {
      return duration;
    }
  }
}
