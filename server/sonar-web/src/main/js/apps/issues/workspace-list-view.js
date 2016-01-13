/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import WorkspaceListView from '../../components/navigator/workspace-list-view';
import IssueView from './workspace-list-item-view';
import EmptyView from './workspace-list-empty-view';
import Template from './templates/issues-workspace-list.hbs';
import ComponentTemplate from './templates/issues-workspace-list-component.hbs';

var COMPONENT_HEIGHT = 29,
    BOTTOM_OFFSET = 60;

export default WorkspaceListView.extend({
  template: Template,
  componentTemplate: ComponentTemplate,
  childView: IssueView,
  childViewContainer: '.js-list',
  emptyView: EmptyView,

  bindShortcuts: function () {
    var that = this;
    var doAction = function (action) {
      var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
      if (selectedIssue == null) {
        return;
      }
      var selectedIssueView = that.children.findByModel(selectedIssue);
      selectedIssueView.$('.js-issue-' + action).click();
    };
    WorkspaceListView.prototype.bindShortcuts.apply(this, arguments);
    key('right', 'list', function () {
      var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
      that.options.app.controller.showComponentViewer(selectedIssue);
      return false;
    });
    key('space', 'list', function () {
      var selectedIssue = that.collection.at(that.options.app.state.get('selectedIndex'));
      selectedIssue.set({ selected: !selectedIssue.get('selected') });
      return false;
    });
    key('f', 'list', function () {
      return doAction('transition');
    });
    key('a', 'list', function () {
      return doAction('assign');
    });
    key('m', 'list', function () {
      return doAction('assign-to-me');
    });
    key('p', 'list', function () {
      return doAction('plan');
    });
    key('i', 'list', function () {
      return doAction('set-severity');
    });
    key('c', 'list', function () {
      return doAction('comment');
    });
    return key('t', 'list', function () {
      return doAction('edit-tags');
    });
  },

  scrollTo: function () {
    var selectedIssue = this.collection.at(this.options.app.state.get('selectedIndex'));
    if (selectedIssue == null) {
      return;
    }
    var selectedIssueView = this.children.findByModel(selectedIssue),
        parentTopOffset = this.$el.offset().top,
        viewTop = selectedIssueView.$el.offset().top - parentTopOffset;
    if (selectedIssueView.$el.prev().is('.issues-workspace-list-component')) {
      viewTop -= COMPONENT_HEIGHT;
    }
    var viewBottom = selectedIssueView.$el.offset().top + selectedIssueView.$el.outerHeight() + BOTTOM_OFFSET,
        windowTop = $(window).scrollTop(),
        windowBottom = windowTop + $(window).height();
    if (viewTop < windowTop) {
      $(window).scrollTop(viewTop);
    }
    if (viewBottom > windowBottom) {
      $(window).scrollTop($(window).scrollTop() - windowBottom + viewBottom);
    }
  },

  attachHtml: function (compositeView, childView, index) {
    var $container = this.getChildViewContainer(compositeView),
        model = this.collection.at(index);
    if (model != null) {
      var prev = index > 0 && this.collection.at(index - 1),
          putComponent = !prev;
      if (prev) {
        var fullComponent = [model.get('project'), model.get('component')].join(' '),
            fullPrevComponent = [prev.get('project'), prev.get('component')].join(' ');
        if (fullComponent !== fullPrevComponent) {
          putComponent = true;
        }
      }
      if (putComponent) {
        $container.append(this.componentTemplate(model.toJSON()));
      }
    }
    $container.append(childView.el);
  },

  destroyChildren: function () {
    WorkspaceListView.prototype.destroyChildren.apply(this, arguments);
    this.$('.issues-workspace-list-component').remove();
  }
});


