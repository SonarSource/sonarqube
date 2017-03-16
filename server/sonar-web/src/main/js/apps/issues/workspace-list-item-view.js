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
import React from 'react';
import { render, unmountComponentAtNode } from 'react-dom';
import Marionette from 'backbone.marionette';
import ConnectedIssue from '../../components/issue/ConnectedIssue';
import IssueFilterView from './issue-filter-view';
import WithStore from '../../components/shared/WithStore';
import getStore from '../../app/utils/getStore';
import { getIssueByKey } from '../../store/rootReducer';

const SHOULD_NULL = {
  any: ['issues'],
  resolutions: ['resolved'],
  resolved: ['resolutions'],
  assignees: ['assigned'],
  assigned: ['assignees']
};

export default Marionette.ItemView.extend({
  className: 'issues-workspace-list-item',

  initialize(options) {
    this.openComponentViewer = this.openComponentViewer.bind(this);
    this.onIssueFilterClick = this.onIssueFilterClick.bind(this);
    this.onIssueCheck = this.onIssueCheck.bind(this);
    this.listenTo(options.app.state, 'change:selectedIndex', this.showIssue);
    this.listenTo(this.model, 'change:selected', this.showIssue);
    this.subscribeToStore();
  },

  template() {
    return '<div></div>';
  },

  subscribeToStore() {
    const store = getStore();
    store.subscribe(() => {
      const issue = getIssueByKey(store.getState(), this.model.get('key'));
      this.model.set(issue);
    });
  },

  onRender() {
    this.showIssue();
  },

  onDestroy() {
    unmountComponentAtNode(this.el);
  },

  showIssue() {
    const selected = this.model.get('index') === this.options.app.state.get('selectedIndex');

    render(
      <WithStore>
        <ConnectedIssue
          issueKey={this.model.get('key')}
          checked={this.model.get('selected')}
          onCheck={this.onIssueCheck}
          onClick={this.openComponentViewer}
          onFilterClick={this.onIssueFilterClick}
          selected={selected}
        />
      </WithStore>,
      this.el
    );
  },

  onIssueFilterClick(e) {
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

  onIssueCheck(e) {
    e.preventDefault();
    e.stopPropagation();
    this.model.set({ selected: !this.model.get('selected') });
    const selected = this.model.collection.where({ selected: true }).length;
    this.options.app.state.set({ selected });
  },

  changeSelection() {
    const selected = this.model.get('index') === this.options.app.state.get('selectedIndex');
    if (selected) {
      this.select();
    } else {
      this.unselect();
    }
  },

  selectCurrent() {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
  },

  resetIssue(options) {
    const that = this;
    const key = this.model.get('key');
    const componentUuid = this.model.get('componentUuid');
    const index = this.model.get('index');
    const selected = this.model.get('selected');
    this.model.reset(
      {
        key,
        componentUuid,
        index,
        selected
      },
      { silent: true }
    );
    return this.model.fetch(options).done(() => that.trigger('reset'));
  },

  openComponentViewer() {
    this.options.app.state.set({ selectedIndex: this.model.get('index') });
    if (this.options.app.state.has('component')) {
      return this.options.app.controller.closeComponentViewer();
    } else {
      return this.options.app.controller.showComponentViewer(this.model);
    }
  }
});
