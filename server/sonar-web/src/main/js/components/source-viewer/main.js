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
import moment from 'moment';
import sortBy from 'lodash/sortBy';
import toPairs from 'lodash/toPairs';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Source from './source';
import Issues from '../issue/collections/issues';
import IssueView from '../issue/issue-view';
import HeaderView from './header';
import SCMPopupView from './popups/scm-popup';
import CoveragePopupView from './popups/coverage-popup';
import DuplicationPopupView from './popups/duplication-popup';
import LineActionsPopupView from './popups/line-actions-popup';
import highlightLocations from './helpers/code-with-issue-locations-helper';
import Template from './templates/source-viewer.hbs';
import IssueLocationTemplate from './templates/source-viewer-issue-location.hbs';
import { translateWithParameters } from '../../helpers/l10n';

const HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted';

export default Marionette.LayoutView.extend({
  className: 'source-viewer',
  template: Template,
  issueLocationTemplate: IssueLocationTemplate,

  ISSUES_LIMIT: 3000,

  LINES_AROUND: 500,

  // keep it twice bigger than LINES_AROUND
  LINES_LIMIT: 1000,
  TOTAL_LINES_LIMIT: 1000,

  regions: {
    headerRegion: '.source-viewer-header'
  },

  ui: {
    sourceBeforeSpinner: '.js-component-viewer-source-before',
    sourceAfterSpinner: '.js-component-viewer-source-after'
  },

  events () {
    return {
      'click .sym': 'highlightUsages',
      'click .source-line-scm': 'showSCMPopup',
      'click .source-line-covered': 'showCoveragePopup',
      'click .source-line-partially-covered': 'showCoveragePopup',
      'click .source-line-uncovered': 'showCoveragePopup',
      'click .source-line-duplications': 'showDuplications',
      'click .source-line-duplications-extra': 'showDuplicationPopup',
      'click .source-line-with-issues': 'onLineIssuesClick',
      'click .source-line-number[data-line-number]': 'onLineNumberClick',
      'mouseenter .source-line-filtered .source-line-filtered-container': 'showFilteredTooltip',
      'mouseleave .source-line-filtered .source-line-filtered-container': 'hideFilteredTooltip',
      'click @ui.sourceBeforeSpinner': 'loadSourceBefore',
      'click @ui.sourceAfterSpinner': 'loadSourceAfter'
    };
  },

  initialize () {
    if (this.model == null) {
      this.model = new Source();
    }
    this.issues = new Issues();
    this.listenTo(this.issues, 'change:severity', this.onIssuesSeverityChange);
    this.listenTo(this.issues, 'locations', this.toggleIssueLocations);
    this.issueViews = [];
    this.highlightedLine = null;
    this.listenTo(this, 'loaded', this.onLoaded);
  },

  renderHeader () {
    this.headerRegion.show(new HeaderView({
      viewer: this,
      model: this.model
    }));
  },

  onRender () {
    this.renderHeader();
    this.renderIssues();
    if (this.model.has('filterLinesFunc')) {
      this.filterLines(this.model.get('filterLinesFunc'));
    }
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy () {
    this.issueViews.forEach(view => view.destroy());
    this.issueViews = [];
    this.clearTooltips();
    this.unbindScrollEvents();
  },

  clearTooltips () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onLoaded () {
    this.bindScrollEvents();
  },

  open (id, options) {
    const that = this;
    const opts = typeof options === 'object' ? options : {};
    const finalize = function () {
      that.requestIssues().done(() => {
        if (!that.isDestroyed) {
          that.render();
          that.trigger('loaded');
        }
      });
    };
    Object.assign(this.options, { workspace: false, ...opts });
    this.model
        .clear()
        .set(this.model.defaults())
        .set({ uuid: id });
    this.requestComponent().done(() => {
      that.requestSource(opts.aroundLine)
          .done(finalize)
          .fail(() => {
            that.model.set({
              source: [
                { line: 0 }
              ]
            });
            finalize();
          });
    });
    return this;
  },

  requestComponent () {
    const that = this;
    const url = window.baseUrl + '/api/components/app';
    const data = { uuid: this.model.id };
    return $.ajax({
      url,
      data,
      type: 'GET',
      statusCode: {
        404 () {
          that.model.set({ exist: false });
          that.render();
          that.trigger('loaded');
        }
      }
    }).done(r => {
      that.model.set(r);
      that.model.set({ isUnitTest: r.q === 'UTS' });
    });
  },

  linesLimit (aroundLine) {
    if (aroundLine) {
      return {
        from: Math.max(1, aroundLine - this.LINES_AROUND),
        to: aroundLine + this.LINES_AROUND
      };
    }
    return { from: 1, to: this.LINES_AROUND };
  },

  getCoverageStatus (row) {
    let status = null;
    if (row.lineHits > 0) {
      status = 'partially-covered';
    }
    if (row.lineHits > 0 && row.conditions === row.coveredConditions) {
      status = 'covered';
    }
    if (row.lineHits === 0 || row.coveredConditions === 0) {
      status = 'uncovered';
    }
    return status;
  },

  requestSource (aroundLine) {
    const that = this;
    const url = window.baseUrl + '/api/sources/lines';
    const data = { uuid: this.model.id, ...this.linesLimit(aroundLine) };
    return $.ajax({
      url,
      data,
      statusCode: {
        // don't display global error
        403: null
      }
    }).done(r => {
      let source = (r.sources || []).slice(0);
      if (source.length === 0 || (source.length > 0 && source[0].line === 1)) {
        source.unshift({ line: 0 });
      }
      source = source.map(row => {
        return { ...row, coverageStatus: that.getCoverageStatus(row) };
      });
      const firstLine = source.length > 0 ? source[0].line : null;
      const linesRequested = data.to - data.from + 1;
      that.model.set({
        source,
        hasCoverage: that.model.hasCoverage(source),
        hasSourceBefore: firstLine > 1,
        hasSourceAfter: r.sources.length === linesRequested
      });
      that.model.checkIfHasDuplications();
    }).fail(request => {
      if (request.status === 403) {
        that.model.set({
          source: [],
          hasSourceBefore: false,
          hasSourceAfter: false,
          canSeeCode: false
        });
      }
    });
  },

  requestDuplications () {
    const that = this;
    const url = window.baseUrl + '/api/duplications/show';
    const options = { uuid: this.model.id };
    return $.get(url, options, data => {
      const hasDuplications = data.duplications != null;
      let duplications = [];
      if (hasDuplications) {
        duplications = {};
        data.duplications.forEach(d => {
          d.blocks.forEach(b => {
            if (b._ref === '1') {
              const lineFrom = b.from;
              const lineTo = b.from + b.size - 1;
              for (let j = lineFrom; j <= lineTo; j++) {
                duplications[j] = true;
              }
            }
          });
        });
        duplications = toPairs(duplications).map(line => {
          return {
            line: +line[0],
            duplicated: line[1]
          };
        });
      }
      that.model.addMeta(duplications);
      that.model.addDuplications(data.duplications);
      that.model.set({
        duplications: data.duplications,
        duplicationsParsed: duplications,
        duplicationFiles: data.files
      });
    });
  },

  requestIssues () {
    const that = this;
    const options = {
      data: {
        componentUuids: this.model.id,
        f: 'component,componentId,project,subProject,rule,status,resolution,author,assignee,debt,' +
        'line,message,severity,creationDate,updateDate,closeDate,tags,comments,attr,actions,' +
        'transitions',
        additionalFields: '_all',
        resolved: false,
        s: 'FILE_LINE',
        asc: true,
        ps: this.ISSUES_LIMIT
      }
    };
    return this.issues.fetch(options).done(() => {
      that.addIssuesPerLineMeta(that.issues);
    });
  },

  _sortBySeverity (issues) {
    const order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    return sortBy(issues, issue => order.indexOf(issue.severity));
  },

  addIssuesPerLineMeta (issues) {
    const that = this;
    const lines = {};
    issues.forEach(issue => {
      const line = issue.get('line') || 0;
      if (!Array.isArray(lines[line])) {
        lines[line] = [];
      }
      lines[line].push(issue.toJSON());
    });
    const issuesPerLine = toPairs(lines).map(line => {
      return {
        line: +line[0],
        issues: that._sortBySeverity(line[1])
      };
    });
    this.model.addMeta(issuesPerLine);
    this.addIssueLocationsMeta(issues);
  },

  addIssueLocationsMeta (issues) {
    const issueLocations = [];
    issues.forEach(issue => {
      issue.getLinearLocations().forEach(location => {
        const record = issueLocations.find(row => row.line === location.line);
        if (record) {
          record.issueLocations.push({ from: location.from, to: location.to });
        } else {
          issueLocations.push({
            line: location.line,
            issueLocations: [{ from: location.from, to: location.to }]
          });
        }
      });
    });
    this.model.addMeta(issueLocations);
  },

  renderIssues () {
    this.$('.issue-list').addClass('hidden');
  },

  renderIssue (issue) {
    const issueView = new IssueView({
      el: '#issue-' + issue.get('key'),
      model: issue
    });
    this.issueViews.push(issueView);
    issueView.render();
  },

  addIssue (issue) {
    const line = issue.get('line') || 0;
    const code = this.$(`.source-line-code[data-line-number=${line}]`);
    const issueBox = `<div class="issue" id="issue-${issue.get('key')}" data-key="${issue.get('key')}">`;
    code.addClass('has-issues');
    let issueList = code.find('.issue-list');
    if (issueList.length === 0) {
      code.append('<div class="issue-list"></div>');
      issueList = code.find('.issue-list');
    }
    issueList
        .append(issueBox)
        .removeClass('hidden');
    this.renderIssue(issue);
  },

  showIssuesForLine (line) {
    this.$(`.source-line-code[data-line-number="${line}"]`).find('.issue-list').removeClass('hidden');
    const issues = this.issues.filter(issue => (
        (issue.get('line') === line) || (!issue.get('line') && !line)
    ));
    issues.forEach(this.renderIssue, this);
  },

  onIssuesSeverityChange () {
    const that = this;
    this.addIssuesPerLineMeta(this.issues);
    this.$('.source-line-with-issues').each(function () {
      const line = +$(this).data('line-number');
      const row = that.model.get('source').find(row => row.line === line);
      const issue = row.issues[0];
      $(this).html(`<i class="icon-severity-${issue.severity.toLowerCase()}"></i>`);
    });
  },

  highlightUsages (e) {
    const highlighted = $(e.currentTarget).is('.highlighted');
    const key = e.currentTarget.className.match(/sym-\d+/);
    if (key) {
      this.$('.sym.highlighted').removeClass('highlighted');
      if (!highlighted) {
        this.$('.sym.' + key[0]).addClass('highlighted');
      }
    }
  },

  showSCMPopup (e) {
    e.stopPropagation();
    $('body').click();
    const line = +$(e.currentTarget).data('line-number');
    const row = this.model.get('source').find(row => row.line === line);
    const popup = new SCMPopupView({
      triggerEl: $(e.currentTarget),
      model: new Backbone.Model(row)
    });
    popup.render();
  },

  showCoveragePopup (e) {
    e.stopPropagation();
    $('body').click();
    this.clearTooltips();
    const line = $(e.currentTarget).data('line-number');
    const row = this.model.get('source').find(row => row.line === line);
    const url = window.baseUrl + '/api/tests/list';
    const options = {
      sourceFileId: this.model.id,
      sourceFileLineNumber: line,
      ps: 1000
    };
    return $.get(url, options).done(data => {
      const popup = new CoveragePopupView({
        row,
        collection: new Backbone.Collection(data.tests),
        triggerEl: $(e.currentTarget)
      });
      popup.render();
    });
  },

  showDuplications (e) {
    const that = this;
    const lineNumber = $(e.currentTarget).closest('.source-line').data('line-number');
    this.clearTooltips();
    this.requestDuplications().done(() => {
      that.render();
      that.$el.addClass('source-duplications-expanded');

      // immediately show dropdown popup if there is only one duplicated block
      if (that.model.get('duplications').length === 1) {
        const dupsBlock = that.$(`.source-line[data-line-number=${lineNumber}]`)
            .find('.source-line-duplications-extra');
        dupsBlock.click();
      }
    });
  },

  showDuplicationPopup (e) {
    e.stopPropagation();
    $('body').click();
    this.clearTooltips();
    const index = $(e.currentTarget).data('index');
    const line = $(e.currentTarget).data('line-number');
    let blocks = this.model.get('duplications')[index - 1].blocks;
    const inRemovedComponent = blocks.some(b => b._ref == null);
    let foundOne = false;
    blocks = blocks.filter(b => {
      const outOfBounds = b.from > line || b.from + b.size < line;
      const currentFile = b._ref === '1';
      const shouldDisplayForCurrentFile = outOfBounds || foundOne;
      const shouldDisplay = !currentFile || shouldDisplayForCurrentFile;
      const isOk = (b._ref != null) && shouldDisplay;
      if (b._ref === '1' && !outOfBounds) {
        foundOne = true;
      }
      return isOk;
    });
    const popup = new DuplicationPopupView({
      inRemovedComponent,
      triggerEl: $(e.currentTarget),
      model: this.model,
      collection: new Backbone.Collection(blocks)
    });
    popup.render();
  },

  onLineIssuesClick (e) {
    const line = $(e.currentTarget).data('line-number');
    const issuesList = $(e.currentTarget).parent().find('.issue-list');
    const areIssuesRendered = issuesList.find('.issue-inner').length > 0;
    if (issuesList.is('.hidden')) {
      if (areIssuesRendered) {
        issuesList.removeClass('hidden');
      } else {
        this.showIssuesForLine(line);
      }
    } else {
      issuesList.addClass('hidden');
    }
  },

  showLineActionsPopup (e) {
    e.stopPropagation();
    $('body').click();
    const line = $(e.currentTarget).data('line-number');
    const popup = new LineActionsPopupView({
      line,
      triggerEl: $(e.currentTarget),
      model: this.model,
      row: $(e.currentTarget).closest('.source-line')
    });
    popup.render();
  },

  onLineNumberClick (e) {
    const row = $(e.currentTarget).closest('.source-line');
    const line = row.data('line-number');
    const highlighted = row.is('.' + HIGHLIGHTED_ROW_CLASS);
    if (!highlighted) {
      this.highlightLine(line);
      this.showLineActionsPopup(e);
    } else {
      this.removeHighlighting();
    }
  },

  removeHighlighting () {
    this.highlightedLine = null;
    this.$('.' + HIGHLIGHTED_ROW_CLASS).removeClass(HIGHLIGHTED_ROW_CLASS);
  },

  highlightLine (line) {
    const row = this.$(`.source-line[data-line-number=${line}]`);
    this.removeHighlighting();
    this.highlightedLine = line;
    row.addClass(HIGHLIGHTED_ROW_CLASS);
    return this;
  },

  bindScrollEvents () {
    // no op
  },

  unbindScrollEvents () {
    // no op
  },

  onScroll () {
    // no op
  },

  scrollToLine (line) {
    const row = this.$(`.source-line[data-line-number=${line}]`);
    if (row.length > 0) {
      let p = this.$el.scrollParent();
      if (p.is(document) || p.is('body')) {
        p = $(window);
      }
      const pTopOffset = p.offset() != null ? p.offset().top : 0;
      const pHeight = p.height();
      const goal = row.offset().top - pHeight / 3 - pTopOffset;
      p.scrollTop(goal);
    }
    return this;
  },

  scrollToFirstLine (line) {
    const row = this.$(`.source-line[data-line-number=${line}]`);
    if (row.length > 0) {
      let p = this.$el.scrollParent();
      if (p.is(document) || p.is('body')) {
        p = $(window);
      }
      const pTopOffset = p.offset() != null ? p.offset().top : 0;
      const goal = row.offset().top - pTopOffset;
      p.scrollTop(goal);
    }
    return this;
  },

  scrollToLastLine (line) {
    const row = this.$(`.source-line[data-line-number=${line}]`);
    if (row.length > 0) {
      let p = this.$el.scrollParent();
      if (p.is(document) || p.is('body')) {
        p = $(window);
      }
      const pTopOffset = p.offset() != null ? p.offset().top : 0;
      const pHeight = p.height();
      const goal = row.offset().top - pTopOffset - pHeight + row.height();
      p.scrollTop(goal);
    }
    return this;
  },

  loadSourceBefore (e) {
    e.preventDefault();
    this.unbindScrollEvents();
    this.$('.js-component-viewer-loading-before').removeClass('hidden');
    this.$('.js-component-viewer-source-before').addClass('hidden');
    const that = this;
    let source = this.model.get('source');
    const firstLine = source[0].line;
    const url = window.baseUrl + '/api/sources/lines';
    const options = {
      uuid: this.model.id,
      from: Math.max(1, firstLine - this.LINES_AROUND),
      to: firstLine - 1
    };
    return $.get(url, options).done(data => {
      source = (data.sources || []).concat(source);
      if (source.length > that.TOTAL_LINES_LIMIT + 1) {
        source = source.slice(0, that.TOTAL_LINES_LIMIT);
        that.model.set({ hasSourceAfter: true });
      }
      if (source.length === 0 || (source.length > 0 && source[0].line === 1)) {
        source.unshift({ line: 0 });
      }
      source = source.map(row => {
        return { ...row, coverageStatus: that.getCoverageStatus(row) };
      });
      that.model.set({
        source,
        hasCoverage: that.model.hasCoverage(source),
        hasSourceBefore: data.sources.length === that.LINES_AROUND && source.length > 0 && source[0].line > 0
      });
      that.addIssuesPerLineMeta(that.issues);
      if (that.model.has('duplications')) {
        that.model.addDuplications(that.model.get('duplications'));
        that.model.addMeta(that.model.get('duplicationsParsed'));
      }
      that.model.checkIfHasDuplications();
      that.render();
      that.scrollToFirstLine(firstLine);
      if (that.model.get('hasSourceBefore') || that.model.get('hasSourceAfter')) {
        that.bindScrollEvents();
      }
    });
  },

  loadSourceAfter (e) {
    e.preventDefault();
    this.unbindScrollEvents();
    this.$('.js-component-viewer-loading-after').removeClass('hidden');
    this.$('.js-component-viewer-source-after').addClass('hidden');
    const that = this;
    let source = this.model.get('source');
    const lastLine = source[source.length - 1].line;
    const url = window.baseUrl + '/api/sources/lines';
    const options = {
      uuid: this.model.id,
      from: lastLine + 1,
      to: lastLine + this.LINES_AROUND
    };
    return $.get(url, options).done(data => {
      source = source.concat(data.sources);
      if (source.length > that.TOTAL_LINES_LIMIT + 1) {
        source = source.slice(source.length - that.TOTAL_LINES_LIMIT);
        that.model.set({ hasSourceBefore: true });
      }
      source = source.map(row => {
        return { ...row, coverageStatus: that.getCoverageStatus(row) };
      });
      that.model.set({
        source,
        hasCoverage: that.model.hasCoverage(source),
        hasSourceAfter: data.sources.length === that.LINES_AROUND
      });
      that.addIssuesPerLineMeta(that.issues);
      if (that.model.has('duplications')) {
        that.model.addDuplications(that.model.get('duplications'));
        that.model.addMeta(that.model.get('duplicationsParsed'));
      }
      that.model.checkIfHasDuplications();
      that.render();
      that.scrollToLastLine(lastLine);
      if (that.model.get('hasSourceBefore') || that.model.get('hasSourceAfter')) {
        that.bindScrollEvents();
      }
    }).fail(() => {
      that.model.set({
        hasSourceAfter: false
      });
      that.render();
      if (that.model.get('hasSourceBefore') || that.model.get('hasSourceAfter')) {
        that.bindScrollEvents();
      }
    });
  },

  filterLines (func) {
    const lines = this.model.get('source');
    const $lines = this.$('.source-line');
    this.model.set('filterLinesFunc', func);
    lines.forEach((line, idx) => {
      const $line = $($lines[idx]);
      const filtered = func(line) && line.line > 0;
      $line.toggleClass('source-line-shadowed', !filtered);
      $line.toggleClass('source-line-filtered', filtered);
    });
  },

  filterLinesByDate (date, label) {
    const sinceDate = moment(date).toDate();
    this.sinceLabel = label;
    this.filterLines(line => {
      const scmDate = moment(line.scmDate).toDate();
      return scmDate >= sinceDate;
    });
  },

  showFilteredTooltip (e) {
    $(e.currentTarget).tooltip({
      container: 'body',
      placement: 'right',
      title: translateWithParameters('source_viewer.tooltip.new_code', this.sinceLabel),
      trigger: 'manual'
    }).tooltip('show');
  },

  hideFilteredTooltip (e) {
    $(e.currentTarget).tooltip('destroy');
  },

  toggleIssueLocations (issue) {
    if (this.locationsShowFor === issue) {
      this.hideIssueLocations();
    } else {
      this.hideIssueLocations();
      this.showIssueLocations(issue);
    }
  },

  showIssueLocations (issue) {
    this.locationsShowFor = issue;
    const primaryLocation = {
      msg: issue.get('message'),
      textRange: issue.get('textRange')
    };
    let _locations = [primaryLocation];
    issue.get('flows').forEach(flow => {
      const flowLocationsCount = Array.isArray(flow.locations) ? flow.locations.length : 0;
      const flowLocations = flow.locations.map((location, index) => {
        const _location = { ...location };
        if (flowLocationsCount > 1) {
          Object.assign(_location, { index: flowLocationsCount - index });
        }
        return _location;
      });
      _locations = [].concat(_locations, flowLocations);
    });
    _locations.forEach(this.showIssueLocation, this);
  },

  showIssueLocation (location, index) {
    if (location && location.textRange) {
      const line = location.textRange.startLine;
      const row = this.$(`.source-line-code[data-line-number="${line}"]`);

      if (index > 0 && location.msg) {
        // render location marker only for
        // secondary locations and execution flows
        // and only if message is not empty
        const renderedFlowLocation = this.renderIssueLocation(location);
        row.find('.source-line-issue-locations').prepend(renderedFlowLocation);
      }

      this.highlightIssueLocationInCode(location);
    }
  },

  renderIssueLocation (location) {
    location.msg = location.msg ? location.msg : ' ';
    return this.issueLocationTemplate(location);
  },

  highlightIssueLocationInCode (location) {
    for (let line = location.textRange.startLine; line <= location.textRange.endLine; line++) {
      const row = this.$(`.source-line-code[data-line-number="${line}"]`);

      // get location for the current line
      const from = line === location.textRange.startLine ? location.textRange.startOffset : 0;
      const to = line === location.textRange.endLine ? location.textRange.endOffset : 999999;
      const _location = { from, to };

      // mark issue location in the source code
      const codeEl = row.find('.source-line-code-inner > pre');
      const code = codeEl.html();
      const newCode = highlightLocations(code, [_location], 'source-line-code-secondary-issue');
      codeEl.html(newCode);
    }
  },

  hideIssueLocations () {
    this.locationsShowFor = null;
    this.$('.source-line-issue-locations').empty();
    this.$('.source-line-code-secondary-issue').removeClass('source-line-code-secondary-issue');
  }
});
