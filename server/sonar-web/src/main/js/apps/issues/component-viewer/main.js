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
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import WithStore from '../../../components/shared/WithStore';

export default Marionette.ItemView.extend({
  template() {
    return '<div></div>';
  },

  initialize(options) {
    this.handleLoadIssues = this.handleLoadIssues.bind(this);
    this.scrollToBaseIssue = this.scrollToBaseIssue.bind(this);
    this.selectIssue = this.selectIssue.bind(this);
    this.listenTo(options.app.state, 'change:selectedIndex', this.select);
  },

  onRender() {
    this.showViewer();
  },

  onDestroy() {
    this.unbindShortcuts();
    unmountComponentAtNode(this.el);
  },

  handleLoadIssues(component: string) {
    // TODO fromLine: number, toLine: number
    const issues = this.options.app.list.toJSON().filter(issue => issue.componentKey === component);
    return Promise.resolve(issues);
  },

  showViewer(onLoaded) {
    if (!this.baseIssue) {
      return;
    }

    const componentKey = this.baseIssue.get('component');

    render(
      <WithStore>
        <SourceViewer
          aroundLine={this.baseIssue.get('line')}
          component={componentKey}
          displayAllIssues={true}
          loadIssues={this.handleLoadIssues}
          onLoaded={onLoaded}
          onIssueSelect={this.selectIssue}
          selectedIssue={this.baseIssue.get('key')}
        />
      </WithStore>,
      this.el
    );
  },

  openFileByIssue(issue) {
    this.baseIssue = issue;
    this.selectedIssue = issue.get('key');
    this.showViewer(this.scrollToBaseIssue);
    this.bindShortcuts();
  },

  bindShortcuts() {
    key('up', 'componentViewer', () => {
      this.options.app.controller.selectPrev();
      return false;
    });
    key('down', 'componentViewer', () => {
      this.options.app.controller.selectNext();
      return false;
    });
    key('left,backspace', 'componentViewer', () => {
      this.options.app.controller.closeComponentViewer();
      return false;
    });
  },

  unbindShortcuts() {
    key.deleteScope('componentViewer');
  },

  select() {
    const selected = this.options.app.state.get('selectedIndex');
    const selectedIssue = this.options.app.list.at(selected);

    if (selectedIssue.get('component') === this.baseIssue.get('component')) {
      this.baseIssue = selectedIssue;
      this.showViewer(this.scrollToBaseIssue);
      this.scrollToBaseIssue();
    } else {
      this.options.app.controller.showComponentViewer(selectedIssue);
    }
  },

  scrollToLine(line) {
    const row = this.$(`[data-line-number=${line}]`);
    const topOffset = $(window).height() / 2 - 60;
    const goal = row.length > 0 ? row.offset().top - topOffset : 0;
    $(window).scrollTop(goal);
  },

  selectIssue(issueKey) {
    const issue = this.options.app.list.find(model => model.get('key') === issueKey);
    const index = this.options.app.list.indexOf(issue);
    this.options.app.state.set({ selectedIndex: index });
  },

  scrollToBaseIssue() {
    this.scrollToLine(this.baseIssue.get('line'));
  }
});
