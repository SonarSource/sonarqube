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
import $ from 'jquery';
import IssueView from '../../components/issue/issue-view';
import IssueFilterView from './issue-filter-view';
import CheckboxTemplate from './templates/issues-issue-checkbox.hbs';
import FilterTemplate from './templates/issues-issue-filter.hbs';

const SHOULD_NULL = {
  any: ['issues'],
  resolutions: ['resolved'],
  resolved: ['resolutions'],
  assignees: ['assigned'],
  assigned: ['assignees']
};

export default IssueView.extend({
  checkboxTemplate: CheckboxTemplate,
  filterTemplate: FilterTemplate,

  events () {
    return {
      ...IssueView.prototype.events.apply(this, arguments),
      'click': 'selectCurrent',
      'dblclick': 'openComponentViewer',
      'click .js-issue-navigate': 'openComponentViewer',
      'click .js-issue-filter': 'onIssueFilterClick',
      'click .js-toggle': 'onIssueToggle'
    };
  },

  initialize (options) {
    IssueView.prototype.initialize.apply(this, arguments);
    this.listenTo(options.app.state, 'change:selectedIndex', this.select);
  },

  onRender () {
    IssueView.prototype.onRender.apply(this, arguments);
    this.select();
    this.addFilterSelect();
    this.addCheckbox();
    this.$el.addClass('issue-navigate-right');
    if (this.options.app.state.get('canBulkChange')) {
      this.$el.addClass('issue-with-checkbox');
    }
  },

  onIssueFilterClick (e) {
    const that = this;
    e.preventDefault();
    e.stopPropagation();
    $('body').click();
    this.popup = new IssueFilterView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: this.model
    });
    this.popup.on('select', (property, value) => {
      const obj = {};
      obj[property] = '' + value;
      SHOULD_NULL.any.forEach(p => {
        obj[p] = null;
      });
      if (SHOULD_NULL[property] != null) {
        SHOULD_NULL[property].forEach(p => {
          obj[p] = null;
        });
      }
      that.options.app.state.updateFilter(obj);
      that.popup.destroy();
    });
    this.popup.render();
  },

  onIssueToggle (e) {
    e.preventDefault();
    this.model.set({ selected: !this.model.get('selected') });
    const selected = this.model.collection.where({ selected: true }).length;
    this.options.app.state.set({ selected });
  },

  addFilterSelect () {
    this.$('.issue-table-meta-cell-first')
        .find('.issue-meta-list')
        .append(this.filterTemplate(this.model.toJSON()));
  },

  addCheckbox () {
    this.$el.append(this.checkboxTemplate(this.model.toJSON()));
  },

  select () {
    const selected = this.model.get('index') === this.options.app.state.get('selectedIndex');
    this.$el.toggleClass('selected', selected);
  },

  selectCurrent () {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
  },

  resetIssue (options) {
    const that = this;
    const key = this.model.get('key');
    const componentUuid = this.model.get('componentUuid');
    const index = this.model.get('index');
    const selected = this.model.get('selected');
    this.model.reset({
      key,
      componentUuid,
      index,
      selected
    }, { silent: true });
    return this.model.fetch(options).done(() => that.trigger('reset'));
  },

  openComponentViewer () {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
    if (this.options.app.state.has('component')) {
      return this.options.app.controller.closeComponentViewer();
    } else {
      return this.options.app.controller.showComponentViewer(this.model);
    }
  },

  serializeData () {
    return {
      ...IssueView.prototype.serializeData.apply(this, arguments),
      showComponent: true
    };
  }
});
