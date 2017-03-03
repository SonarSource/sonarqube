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
import SourceViewer from '../../../components/source-viewer/main';
import IssueView from './issue-view';

export default SourceViewer.extend({
  events () {
    return {
      ...SourceViewer.prototype.events.apply(this, arguments),
      'click .js-close-component-viewer': 'closeComponentViewer',
      'click .code-issue': 'selectIssue'
    };
  },

  initialize (options) {
    SourceViewer.prototype.initialize.apply(this, arguments);
    return this.listenTo(options.app.state, 'change:selectedIndex', this.select);
  },

  onLoaded () {
    SourceViewer.prototype.onLoaded.apply(this, arguments);
    this.bindShortcuts();
    if (this.baseIssue != null) {
      this.baseIssue.trigger('locations', this.baseIssue);
      this.scrollToLine(this.baseIssue.get('line'));
    }
  },

  bindShortcuts () {
    const that = this;
    const doAction = function (action) {
      const selectedIssueView = that.getSelectedIssueEl();
      if (!selectedIssueView) {
        return;
      }
      selectedIssueView.find('.js-issue-' + action).click();
    };
    key('up', 'componentViewer', () => {
      that.options.app.controller.selectPrev();
      return false;
    });
    key('down', 'componentViewer', () => {
      that.options.app.controller.selectNext();
      return false;
    });
    key('left,backspace', 'componentViewer', () => {
      that.options.app.controller.closeComponentViewer();
      return false;
    });
    key('f', 'componentViewer', () => doAction('transition'));
    key('a', 'componentViewer', () => doAction('assign'));
    key('m', 'componentViewer', () => doAction('assign-to-me'));
    key('p', 'componentViewer', () => doAction('plan'));
    key('i', 'componentViewer', () => doAction('set-severity'));
    key('c', 'componentViewer', () => doAction('comment'));
  },

  unbindShortcuts () {
    return key.deleteScope('componentViewer');
  },

  onDestroy () {
    SourceViewer.prototype.onDestroy.apply(this, arguments);
    this.unbindScrollEvents();
    return this.unbindShortcuts();
  },

  select () {
    const selected = this.options.app.state.get('selectedIndex');
    const selectedIssue = this.options.app.list.at(selected);
    if (selectedIssue.get('component') === this.model.get('key')) {
      selectedIssue.trigger('locations', selectedIssue);
      return this.scrollToIssue(selectedIssue.get('key'));
    } else {
      this.unbindShortcuts();
      return this.options.app.controller.showComponentViewer(selectedIssue);
    }
  },

  getSelectedIssueEl () {
    const selected = this.options.app.state.get('selectedIndex');
    if (selected == null) {
      return null;
    }
    const selectedIssue = this.options.app.list.at(selected);
    if (selectedIssue == null) {
      return null;
    }
    const selectedIssueView = this.$('#issue-' + (selectedIssue.get('key')));
    if (selectedIssueView.length > 0) {
      return selectedIssueView;
    } else {
      return null;
    }
  },

  selectIssue (e) {
    const key = $(e.currentTarget).data('issue-key');
    const issue = this.issues.find(model => model.get('key') === key);
    const index = this.options.app.list.indexOf(issue);
    return this.options.app.state.set({ selectedIndex: index });
  },

  scrollToIssue (key) {
    const el = this.$('#issue-' + key);
    if (el.length > 0) {
      const line = el.closest('[data-line-number]').data('line-number');
      return this.scrollToLine(line);
    } else {
      this.unbindShortcuts();
      const selected = this.options.app.state.get('selectedIndex');
      const selectedIssue = this.options.app.list.at(selected);
      return this.options.app.controller.showComponentViewer(selectedIssue);
    }
  },

  openFileByIssue (issue) {
    this.baseIssue = issue;
    const componentKey = issue.get('component');
    const componentUuid = issue.get('componentUuid');
    return this.open(componentUuid, componentKey);
  },

  linesLimit () {
    let line = this.LINES_LIMIT / 2;
    if ((this.baseIssue != null) && this.baseIssue.has('line')) {
      line = Math.max(line, this.baseIssue.get('line'));
    }
    return {
      from: line - this.LINES_LIMIT / 2 + 1,
      to: line + this.LINES_LIMIT / 2
    };
  },

  limitIssues (issues) {
    const that = this;
    let index = this.ISSUES_LIMIT / 2;
    if ((this.baseIssue != null) && this.baseIssue.has('index')) {
      index = Math.max(index, this.baseIssue.get('index'));
    }
    return issues.filter(issue => Math.abs(issue.get('index') - index) <= that.ISSUES_LIMIT / 2);
  },

  requestIssues () {
    const that = this;
    let r;
    if (this.options.app.list.last().get('component') === this.model.get('key')) {
      r = this.options.app.controller.fetchNextPage();
    } else {
      r = $.Deferred().resolve().promise();
    }
    return r.done(() => {
      that.issues.reset(that.options.app.list.filter(issue => issue.get('component') === that.model.key()));
      that.issues.reset(that.limitIssues(that.issues));
      return that.addIssuesPerLineMeta(that.issues);
    });
  },

  renderIssues () {
    this.issues.forEach(this.renderIssue, this);
    return this.$('.source-line-issues').addClass('hidden');
  },

  renderIssue (issue) {
    const issueView = new IssueView({
      el: '#issue-' + issue.get('key'),
      model: issue,
      app: this.options.app
    });
    this.issueViews.push(issueView);
    return issueView.render();
  },

  scrollToLine (line) {
    const row = this.$(`[data-line-number=${line}]`);
    const topOffset = $(window).height() / 2 - 60;
    const goal = row.length > 0 ? row.offset().top - topOffset : 0;
    return $(window).scrollTop(goal);
  },

  closeComponentViewer () {
    return this.options.app.controller.closeComponentViewer();
  }
});

