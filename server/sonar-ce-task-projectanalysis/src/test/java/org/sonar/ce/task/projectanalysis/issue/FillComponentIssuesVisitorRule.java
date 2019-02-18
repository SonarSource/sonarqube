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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.core.issue.DefaultIssue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * This rule can be used when testing a visitor that depends on {@link ComponentIssuesRepository}.
 */
public class FillComponentIssuesVisitorRule extends TypeAwareVisitorAdapter implements TestRule {

  private MutableComponentIssuesRepository issuesRepository = new ComponentIssuesRepositoryImpl();
  private final TreeRootHolder treeRootHolder;

  private ListMultimap<Component, DefaultIssue> issues = ArrayListMultimap.create();

  public FillComponentIssuesVisitorRule(MutableComponentIssuesRepository issuesRepository, TreeRootHolder treeRootHolder) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.issuesRepository = issuesRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          issues = ArrayListMultimap.create();
        }
      }
    };
  }

  public void setIssues(int componentRef, DefaultIssue... issues) {
    Component component = treeRootHolder.getComponentByRef(componentRef);
    checkArgument(component != null, String.format("Component '%s' does not exists in the report ", componentRef));
    this.issues.get(component).clear();
    this.issues.putAll(component, asList(issues));
  }

  @Override
  public void visitAny(Component component) {
    issuesRepository.setIssues(component, issues.get(component));
  }

}
