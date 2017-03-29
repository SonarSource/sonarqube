/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import WorkspaceHeaderView from '../../components/navigator/workspace-header-view';
import BulkChangeForm from './BulkChangeForm';
import Template from './templates/issues-workspace-header.hbs';
import { getOrganization, areThereCustomOrganizations } from '../../store/organizations/utils';

export default WorkspaceHeaderView.extend({
  template: Template,

  events() {
    return {
      ...WorkspaceHeaderView.prototype.events.apply(this, arguments),
      'click .js-selection': 'onSelectionClick',
      'click .js-back': 'returnToList',
      'click .js-new-search': 'newSearch',
      'click .js-bulk-change-selected': 'onBulkChangeSelectedClick'
    };
  },

  onSelectionClick(e) {
    e.preventDefault();
    this.toggleSelection();
  },

  onBulkChangeSelectedClick(e) {
    e.preventDefault();
    this.bulkChangeSelected();
  },

  afterBulkChange() {
    const selectedIndex = this.options.app.state.get('selectedIndex');
    const selectedKeys = this.options.app.list.where({ selected: true }).map(item => item.id);
    this.options.app.controller.fetchList().done(() => {
      this.options.app.state.set({ selectedIndex });
      this.options.app.list.selectByKeys(selectedKeys);
    });
  },

  render() {
    if (!this._suppressUpdate) {
      WorkspaceHeaderView.prototype.render.apply(this, arguments);
    }
  },

  toggleSelection() {
    this._suppressUpdate = true;
    const selectedCount = this.options.app.list.where({ selected: true }).length;
    const someSelected = selectedCount > 0;
    return someSelected ? this.selectNone() : this.selectAll();
  },

  selectNone() {
    this.options.app.list.where({ selected: true }).forEach(issue => {
      issue.set({ selected: false });
    });
    this._suppressUpdate = false;
    this.render();
  },

  selectAll() {
    this.options.app.list.forEach(issue => {
      issue.set({ selected: true });
    });
    this._suppressUpdate = false;
    this.render();
  },

  returnToList() {
    this.options.app.controller.closeComponentViewer();
  },

  newSearch() {
    this.options.app.controller.newSearch();
  },

  bulkChange() {
    const query = this.options.app.controller.getQueryAsObject();
    new BulkChangeForm({
      query,
      onChange: () => this.afterBulkChange()
    }).render();
  },

  bulkChangeSelected() {
    const selected = this.options.app.list.where({ selected: true });
    const selectedKeys = selected.map(item => item.id).slice(0, 500);
    const query = { issues: selectedKeys.join() };
    new BulkChangeForm({
      query,
      onChange: () => this.afterBulkChange()
    }).render();
  },

  serializeData() {
    const issuesCount = this.options.app.list.length;
    const selectedCount = this.options.app.list.where({ selected: true }).length;
    const allSelected = issuesCount > 0 && issuesCount === selectedCount;
    const someSelected = !allSelected && selectedCount > 0;
    const data = {
      ...WorkspaceHeaderView.prototype.serializeData.apply(this, arguments),
      selectedCount,
      allSelected,
      someSelected
    };
    const component = this.options.app.state.get('component');
    if (component) {
      const qualifier = this.options.app.state.get('contextComponentQualifier');
      if (qualifier === 'VW' || qualifier === 'SVW') {
        // do nothing
      } else if (qualifier === 'TRK') {
        data.state.component.project = null;
      } else if (qualifier === 'BRC') {
        data.state.component.project = null;
        data.state.component.subProject = null;
      } else {
        const organization = areThereCustomOrganizations()
          ? getOrganization(component.projectOrganization)
          : null;
        Object.assign(data, { organization });
      }
    }
    return data;
  }
});
