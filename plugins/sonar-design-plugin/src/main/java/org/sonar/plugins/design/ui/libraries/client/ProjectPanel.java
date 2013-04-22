/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.design.ui.libraries.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.DependencyTree;
import org.sonar.wsclient.services.DependencyTreeQuery;
import org.sonar.wsclient.services.Resource;

import java.util.List;

public class ProjectPanel extends FlowPanel {

  private Label title;
  private Tree tree;
  private Filters filters;

  public ProjectPanel(Resource project, Filters filters) {
    this.filters = filters;
    setStyleName("libs");
    getElement().setId("libs-" + project.getKey());
    add(new Loading(project.getName()));
    loadLibraries(project);
  }

  private void loadLibraries(final Resource project) {
    Sonar.getInstance().findAll(DependencyTreeQuery.createForProject(project.getId().toString()), new AbstractListCallback<DependencyTree>() {

      @Override
      protected void doOnResponse(List<DependencyTree> dependencyTrees) {
        createTitle(project);
        createTree();

        if (dependencyTrees == null || dependencyTrees.isEmpty()) {
          clear();
          add(title);
          add(createNoLibsMessage());
          
        } else {
          display(dependencyTrees, null);
          filter();

          clear();
          add(title);
          add(tree);
        }
      }

      private void createTitle(Resource project) {
        String html = Icons.forQualifier(project.getQualifier()).getHTML();
        html += " <span class=''> " + project.getName() + "</span> ";

        if (project.getVersion() != null) {
          html += project.getVersion() + " ";
        }
        title = new HTML(html);
      }

      private void display(List<DependencyTree> depTrees, TreeItem parentLibrary) {
        if (depTrees != null) {
          for (DependencyTree depTree : depTrees) {
            Library library = new Library(depTree);
            if (parentLibrary == null) {
              tree.addItem(library);
              library.setState(true);
            } else {
              parentLibrary.addItem(library);
            }
            display(depTree.getTo(), library);
            library.setState(true);
          }
        }
      }

      private void createTree() {
        tree = new Tree();
        tree.setAnimationEnabled(false);
      }
    });
  }

  private Label createNoLibsMessage() {
    Label msg = new Label(Dictionary.getDictionary("l10n").get("libs.noLibraries"));
    msg.setStyleName("nolibs");
    return msg;
  }

  public void filter() {
    boolean visible = (tree.getItemCount() == 0 && !filters.hasKeyword());
    for (int index = 0; index < tree.getItemCount(); index++) {
      Library lib = (Library) tree.getItem(index);
      visible |= !lib.filter(filters.getKeywordFilter().getKeyword(), filters.isTestFiltered());
    }
    setVisible(visible);
  }

  public void expandCollapse(boolean expand) {
    for (int index = 0; index < tree.getItemCount(); index++) {
      openItem(tree.getItem(index), expand);
    }
  }

  private void openItem(TreeItem item, boolean open) {
    item.setState(open);
    for (int i = 0; i < item.getChildCount(); i++) {
      openItem(item.getChild(i), open);
    }
  }
}
