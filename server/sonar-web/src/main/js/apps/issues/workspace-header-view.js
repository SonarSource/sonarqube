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
import _ from 'underscore';
import WorkspaceHeaderView from '../../components/navigator/workspace-header-view';
import Template from './templates/issues-workspace-header.hbs';

export default WorkspaceHeaderView.extend({
  template: Template,

  events () {
    return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
      'click .js-selection': 'onSelectionClick',
      'click .js-back': 'returnToList',
      'click .js-new-search': 'newSearch',
      'click .js-bulk-change-selected': 'onBulkChangeSelectedClick'
    });
  },

  initialize () {
    WorkspaceHeaderView.prototype.initialize.apply(this, arguments);
    this._onBulkIssues = window.onBulkIssues;
    window.onBulkIssues = _.bind(this.afterBulkChange, this);
  },

  onDestroy () {
    WorkspaceHeaderView.prototype.onDestroy.apply(this, arguments);
    window.onBulkIssues = this._onBulkIssues;
  },

  onSelectionClick (e) {
    e.preventDefault();
    this.toggleSelection();
  },

  onBulkChangeSelectedClick (e) {
    e.preventDefault();
    this.bulkChangeSelected();
  },

  afterBulkChange () {
    const that = this;
    $('#modal').dialog('close');
    const selectedIndex = this.options.app.state.get('selectedIndex');
    const selectedKeys = _.pluck(this.options.app.list.where({ selected: true }), 'id');
    this.options.app.controller.fetchList().done(function () {
      that.options.app.state.set({ selectedIndex });
      that.options.app.list.selectByKeys(selectedKeys);
    });
  },

  render () {
    if (!this._suppressUpdate) {
      WorkspaceHeaderView.prototype.render.apply(this, arguments);
    }
  },

  toggleSelection () {
    this._suppressUpdate = true;
    const selectedCount = this.options.app.list.where({ selected: true }).length;
    const someSelected = selectedCount > 0;
    return someSelected ? this.selectNone() : this.selectAll();
  },

  selectNone () {
    this.options.app.list.where({ selected: true }).forEach(function (issue) {
      issue.set({ selected: false });
    });
    this._suppressUpdate = false;
    this.render();
  },

  selectAll () {
    this.options.app.list.forEach(function (issue) {
      issue.set({ selected: true });
    });
    this._suppressUpdate = false;
    this.render();
  },

  returnToList () {
    this.options.app.controller.closeComponentViewer();
  },

  newSearch () {
    this.options.app.controller.newSearch();
  },

  bulkChange () {
    const query = this.options.app.controller.getQuery('&', true);
    const url = window.baseUrl + '/issues/bulk_change_form?' + query;
    window.openModalWindow(url, {});
  },

  bulkChangeSelected () {
    const selected = this.options.app.list.where({ selected: true });
    const selectedKeys = _.first(_.pluck(selected, 'id'), 200);
    const query = 'issues=' + selectedKeys.join();
    const url = window.baseUrl + '/issues/bulk_change_form?' + query;
    window.openModalWindow(url, {});
  },

  serializeData () {
    const issuesCount = this.options.app.list.length;
    const selectedCount = this.options.app.list.where({ selected: true }).length;
    const allSelected = issuesCount > 0 && issuesCount === selectedCount;
    const someSelected = !allSelected && selectedCount > 0;
    return _.extend(WorkspaceHeaderView.prototype.serializeData.apply(this, arguments), {
      selectedCount,
      allSelected,
      someSelected
    });
  }
});


