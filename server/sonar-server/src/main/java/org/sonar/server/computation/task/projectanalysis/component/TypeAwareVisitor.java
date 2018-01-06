/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

/**
 * A {@link ComponentVisitor} which can exposes methods which ensure the type of the visited Component.
 */
public interface TypeAwareVisitor extends ComponentVisitor {
  /**
   * Called when encountering a Component of type {@link Component.Type#PROJECT}
   */
  void visitProject(Component project);

  /**
   * Called when encountering a Component of type {@link Component.Type#MODULE}
   */
  void visitModule(Component module);

  /**
   * Called when encountering a Component of type {@link Component.Type#DIRECTORY}
   */
  void visitDirectory(Component directory);

  /**
   * Called when encountering a Component of type {@link Component.Type#FILE}
   */
  void visitFile(Component file);

  /**
   * Called when encountering a Component of type {@link Component.Type#VIEW}
   */
  void visitView(Component view);

  /**
   * Called when encountering a Component of type {@link Component.Type#SUBVIEW}
   */
  void visitSubView(Component subView);

  /**
   * Called when encountering a Component of type {@link Component.Type#PROJECT_VIEW}
   */
  void visitProjectView(Component projectView);

  /**
   * Called for any component, <strong>in addition</strong> to the methods specific to each type
   */
  void visitAny(Component any);

}
