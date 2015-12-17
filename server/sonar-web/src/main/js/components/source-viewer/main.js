import $ from 'jquery';
import _ from 'underscore';
import moment from 'moment';
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

var HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted';

export default Marionette.LayoutView.extend({
  className: 'source-viewer',
  template: Template,
  issueLocationTemplate: IssueLocationTemplate,

  ISSUES_LIMIT: 3000,
  LINES_LIMIT: 1000,
  TOTAL_LINES_LIMIT: 3000,
  LINES_AROUND: 500,

  regions: {
    headerRegion: '.source-viewer-header'
  },

  ui: {
    sourceBeforeSpinner: '.js-component-viewer-source-before',
    sourceAfterSpinner: '.js-component-viewer-source-after'
  },

  events: function () {
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
      'mouseleave .source-line-filtered .source-line-filtered-container': 'hideFilteredTooltip'
    };
  },

  initialize: function () {
    if (this.model == null) {
      this.model = new Source();
    }
    this.issues = new Issues();
    this.listenTo(this.issues, 'change:severity', this.onIssuesSeverityChange);
    this.listenTo(this.issues, 'locations', this.toggleIssueLocations);
    this.issueViews = [];
    this.loadSourceBeforeThrottled = _.throttle(this.loadSourceBefore, 1000);
    this.loadSourceAfterThrottled = _.throttle(this.loadSourceAfter, 1000);
    this.highlightedLine = null;
    this.listenTo(this, 'loaded', this.onLoaded);
  },

  renderHeader: function () {
    this.headerRegion.show(new HeaderView({
      viewer: this,
      model: this.model
    }));
  },

  onRender: function () {
    this.renderHeader();
    this.renderIssues();
    if (this.model.has('filterLinesFunc')) {
      this.filterLines(this.model.get('filterLinesFunc'));
    }
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy: function () {
    this.issueViews.forEach(function (view) {
      return view.destroy();
    });
    this.issueViews = [];
    this.clearTooltips();
  },

  clearTooltips: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onLoaded: function () {
    this.bindScrollEvents();
  },

  open: function (id, options) {
    var that = this,
        opts = typeof options === 'object' ? options : {},
        finalize = function () {
          that.requestIssues().done(function () {
            that.render();
            that.trigger('loaded');
          });
        };
    _.extend(this.options, _.defaults(opts, { workspace: false }));
    this.model
        .clear()
        .set(_.result(this.model, 'defaults'))
        .set({ uuid: id });
    this.requestComponent().done(function () {
      that.requestSource()
          .done(finalize)
          .fail(function () {
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

  requestComponent: function () {
    var that = this,
        url = baseUrl + '/api/components/app',
        data = { uuid: this.model.id };
    return $.ajax({
      type: 'GET',
      url: url,
      data: data,
      statusCode: {
        404: function () {
          that.model.set({ exist: false });
          that.render();
          that.trigger('loaded');
        }
      }
    }).done(function (r) {
      that.model.set(r);
      that.model.set({ isUnitTest: r.q === 'UTS' });
    });
  },

  linesLimit: function () {
    return {
      from: 1,
      to: this.LINES_LIMIT
    };
  },

  getUTCoverageStatus: function (row) {
    var status = null;
    if (row.utLineHits > 0) {
      status = 'partially-covered';
    }
    if (row.utLineHits > 0 && row.utConditions === row.utCoveredConditions) {
      status = 'covered';
    }
    if (row.utLineHits === 0 || row.utCoveredConditions === 0) {
      status = 'uncovered';
    }
    return status;
  },

  getItCoverageStatus: function (row) {
    var status = null;
    if (row.itLineHits > 0) {
      status = 'partially-covered';
    }
    if (row.itLineHits > 0 && row.itConditions === row.itCoveredConditions) {
      status = 'covered';
    }
    if (row.itLineHits === 0 || row.itCoveredConditions === 0) {
      status = 'uncovered';
    }
    return status;
  },

  requestSource: function () {
    var that = this,
        url = baseUrl + '/api/sources/lines',
        options = _.extend({ uuid: this.model.id }, this.linesLimit());
    return $.get(url, options).done(function (data) {
      var source = (data.sources || []).slice(0);
      if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
        source.unshift({ line: 0 });
      }
      source = source.map(function (row) {
        return _.extend(row, {
          utCoverageStatus: that.getUTCoverageStatus(row),
          itCoverageStatus: that.getItCoverageStatus(row)
        });
      });
      var firstLine = _.first(source).line,
          linesRequested = options.to - options.from + 1;
      that.model.set({
        source: source,
        hasUTCoverage: that.model.hasUTCoverage(source),
        hasITCoverage: that.model.hasITCoverage(source),
        hasSourceBefore: firstLine > 1,
        hasSourceAfter: data.sources.length === linesRequested
      });
      that.model.checkIfHasDuplications();
    }).fail(function (request) {
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

  requestDuplications: function () {
    var that = this,
        url = baseUrl + '/api/duplications/show',
        options = { uuid: this.model.id };
    return $.get(url, options, function (data) {
      var hasDuplications = (data != null) && (data.duplications != null),
          duplications = [];
      if (hasDuplications) {
        duplications = {};
        data.duplications.forEach(function (d) {
          d.blocks.forEach(function (b) {
            if (b._ref === '1') {
              var lineFrom = b.from,
                  lineTo = b.from + b.size - 1;
              for (var j = lineFrom; j <= lineTo; j++) {
                duplications[j] = true;
              }
            }
          });
        });
        duplications = _.pairs(duplications).map(function (line) {
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

  requestIssues: function () {
    var that = this,
        options = {
          data: {
            componentUuids: this.model.id,
            f: 'component,componentId,project,subProject,rule,status,resolution,author,reporter,assignee,debt,' +
            'line,message,severity,actionPlan,creationDate,updateDate,closeDate,tags,comments,attr,actions,' +
            'transitions,actionPlanName',
            additionalFields: '_all',
            resolved: false,
            s: 'FILE_LINE',
            asc: true,
            ps: this.ISSUES_LIMIT
          }
        };
    return this.issues.fetch(options).done(function () {
      that.addIssuesPerLineMeta(that.issues);
    });
  },

  _sortBySeverity: function (issues) {
    var order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    return _.sortBy(issues, function (issue) {
      return order.indexOf(issue.severity);
    });
  },

  addIssuesPerLineMeta: function (issues) {
    var that = this,
        lines = {};
    issues.forEach(function (issue) {
      var line = issue.get('line') || 0;
      if (!_.isArray(lines[line])) {
        lines[line] = [];
      }
      lines[line].push(issue.toJSON());
    });
    var issuesPerLine = _.pairs(lines).map(function (line) {
      return {
        line: +line[0],
        issues: that._sortBySeverity(line[1])
      };
    });
    this.model.addMeta(issuesPerLine);
    this.addIssueLocationsMeta(issues);
  },

  addIssueLocationsMeta: function (issues) {
    var issueLocations = [];
    issues.forEach(function (issue) {
      issue.getLinearLocations().forEach(function (location) {
        var record = _.findWhere(issueLocations, { line: location.line });
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

  renderIssues: function () {
    this.$('.issue-list').addClass('hidden');
  },

  renderIssue: function (issue) {
    var issueView = new IssueView({
      el: '#issue-' + issue.get('key'),
      model: issue
    });
    this.issueViews.push(issueView);
    issueView.render();
  },

  addIssue: function (issue) {
    var line = issue.get('line') || 0,
        code = this.$('.source-line-code[data-line-number=' + line + ']'),
        issueBox = '<div class="issue" id="issue-' + issue.get('key') + '" data-key="' + issue.get('key') + '">';
    code.addClass('has-issues');
    var issueList = code.find('.issue-list');
    if (issueList.length === 0) {
      code.append('<div class="issue-list"></div>');
      issueList = code.find('.issue-list');
    }
    issueList
        .append(issueBox)
        .removeClass('hidden');
    this.renderIssue(issue);
  },

  showIssuesForLine: function (line) {
    this.$('.source-line-code[data-line-number="' + line + '"]').find('.issue-list').removeClass('hidden');
    var issues = this.issues.filter(function (issue) {
      return (issue.get('line') === line) || (!issue.get('line') && !line);
    });
    issues.forEach(this.renderIssue, this);
  },

  onIssuesSeverityChange: function () {
    var that = this;
    this.addIssuesPerLineMeta(this.issues);
    this.$('.source-line-with-issues').each(function () {
      var line = +$(this).data('line-number'),
          row = _.findWhere(that.model.get('source'), { line: line }),
          issue = _.first(row.issues);
      $(this).html('<i class="icon-severity-' + issue.severity.toLowerCase() + '"></i>');
    });
  },

  highlightUsages: function (e) {
    var highlighted = $(e.currentTarget).is('.highlighted'),
        key = e.currentTarget.className.split(/\s+/)[0];
    this.$('.sym.highlighted').removeClass('highlighted');
    if (!highlighted) {
      this.$('.sym.' + key).addClass('highlighted');
    }
  },

  showSCMPopup: function (e) {
    e.stopPropagation();
    $('body').click();
    var line = +$(e.currentTarget).data('line-number'),
        row = _.findWhere(this.model.get('source'), { line: line }),
        popup = new SCMPopupView({
          triggerEl: $(e.currentTarget),
          model: new Backbone.Model(row)
        });
    popup.render();
  },

  showCoveragePopup: function (e) {
    e.stopPropagation();
    $('body').click();
    this.clearTooltips();
    var line = $(e.currentTarget).data('line-number'),
        row = _.findWhere(this.model.get('source'), { line: line }),
        url = baseUrl + '/api/tests/list',
        options = {
          sourceFileId: this.model.id,
          sourceFileLineNumber: line,
          ps: 1000
        };
    return $.get(url, options).done(function (data) {
      var popup = new CoveragePopupView({
        collection: new Backbone.Collection(data.tests),
        row: row,
        tests: $(e.currentTarget).data('tests'),
        triggerEl: $(e.currentTarget)
      });
      popup.render();
    });
  },

  showDuplications: function (e) {
    var that = this,
        lineNumber = $(e.currentTarget).closest('.source-line').data('line-number');
    this.clearTooltips();
    this.requestDuplications().done(function () {
      that.render();
      that.$el.addClass('source-duplications-expanded');

      // immediately show dropdown popup if there is only one duplicated block
      if (that.model.get('duplications').length === 1) {
        var dupsBlock = that.$('.source-line[data-line-number=' + lineNumber + ']')
            .find('.source-line-duplications-extra');
        dupsBlock.click();
      }
    });
  },

  showDuplicationPopup: function (e) {
    e.stopPropagation();
    $('body').click();
    this.clearTooltips();
    var index = $(e.currentTarget).data('index'),
        line = $(e.currentTarget).data('line-number'),
        blocks = this.model.get('duplications')[index - 1].blocks,
        inRemovedComponent = _.some(blocks, function (b) {
          return b._ref == null;
        }),
        foundOne = false;
    blocks = _.filter(blocks, function (b) {
      var outOfBounds = b.from > line || b.from + b.size < line,
          currentFile = b._ref === '1',
          shouldDisplayForCurrentFile = outOfBounds || foundOne,
          shouldDisplay = !currentFile || (currentFile && shouldDisplayForCurrentFile),
          isOk = (b._ref != null) && shouldDisplay;
      if (b._ref === '1' && !outOfBounds) {
        foundOne = true;
      }
      return isOk;
    });
    var popup = new DuplicationPopupView({
      triggerEl: $(e.currentTarget),
      model: this.model,
      inRemovedComponent: inRemovedComponent,
      collection: new Backbone.Collection(blocks)
    });
    popup.render();
  },

  onLineIssuesClick: function (e) {
    var line = $(e.currentTarget).data('line-number'),
        issuesList = $(e.currentTarget).parent().find('.issue-list'),
        areIssuesRendered = issuesList.find('.issue-inner').length > 0;
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

  showLineActionsPopup: function (e) {
    e.stopPropagation();
    $('body').click();
    var that = this,
        line = $(e.currentTarget).data('line-number'),
        popup = new LineActionsPopupView({
          triggerEl: $(e.currentTarget),
          model: this.model,
          line: line,
          row: $(e.currentTarget).closest('.source-line')
        });
    popup.on('onManualIssueAdded', function (issue) {
      that.addIssue(issue);
    });
    popup.render();
  },

  onLineNumberClick: function (e) {
    var row = $(e.currentTarget).closest('.source-line'),
        line = row.data('line-number'),
        highlighted = row.is('.' + HIGHLIGHTED_ROW_CLASS);
    if (!highlighted) {
      this.highlightLine(line);
      this.showLineActionsPopup(e);
    } else {
      this.removeHighlighting();
    }
  },

  removeHighlighting: function () {
    this.highlightedLine = null;
    this.$('.' + HIGHLIGHTED_ROW_CLASS).removeClass(HIGHLIGHTED_ROW_CLASS);
  },

  highlightLine: function (line) {
    var row = this.$('.source-line[data-line-number=' + line + ']');
    this.removeHighlighting();
    this.highlightedLine = line;
    row.addClass(HIGHLIGHTED_ROW_CLASS);
    return this;
  },

  bindScrollEvents: function () {
    var that = this;
    this.$el.scrollParent().on('scroll.source-viewer', function () {
      that.onScroll();
    });
  },

  unbindScrollEvents: function () {
    this.$el.scrollParent().off('scroll.source-viewer');
  },

  onScroll: function () {
    var p = this.$el.scrollParent();
    if (p.is(document)) {
      p = $(window);
    }
    var pTopOffset = p.offset() != null ? p.offset().top : 0,
        pPosition = p.scrollTop() + pTopOffset;
    if (this.model.get('hasSourceBefore') && (pPosition <= this.ui.sourceBeforeSpinner.offset().top)) {
      this.loadSourceBeforeThrottled();
    }
    if (this.model.get('hasSourceAfter') && (pPosition + p.height() >= this.ui.sourceAfterSpinner.offset().top)) {
      return this.loadSourceAfterThrottled();
    }
  },

  scrollToLine: function (line) {
    var row = this.$('.source-line[data-line-number=' + line + ']');
    if (row.length > 0) {
      var p = this.$el.scrollParent();
      if (p.is(document)) {
        p = $(window);
      }
      var pTopOffset = p.offset() != null ? p.offset().top : 0,
          pHeight = p.height(),
          goal = row.offset().top - pHeight / 3 - pTopOffset;
      p.scrollTop(goal);
    }
    return this;
  },

  scrollToFirstLine: function (line) {
    var row = this.$('.source-line[data-line-number=' + line + ']');
    if (row.length > 0) {
      var p = this.$el.scrollParent();
      if (p.is(document)) {
        p = $(window);
      }
      var pTopOffset = p.offset() != null ? p.offset().top : 0,
          goal = row.offset().top - pTopOffset;
      p.scrollTop(goal);
    }
    return this;
  },

  scrollToLastLine: function (line) {
    var row = this.$('.source-line[data-line-number=' + line + ']');
    if (row.length > 0) {
      var p = this.$el.scrollParent();
      if (p.is(document)) {
        p = $(window);
      }
      var pTopOffset = p.offset() != null ? p.offset().top : 0,
          pHeight = p.height(),
          goal = row.offset().top - pTopOffset - pHeight + row.height();
      p.scrollTop(goal);
    }
    return this;
  },

  loadSourceBefore: function () {
    this.unbindScrollEvents();
    var that = this,
        source = this.model.get('source'),
        firstLine = _.first(source).line,
        url = baseUrl + '/api/sources/lines',
        options = {
          uuid: this.model.id,
          from: firstLine - this.LINES_AROUND,
          to: firstLine - 1
        };
    return $.get(url, options).done(function (data) {
      source = (data.sources || []).concat(source);
      if (source.length > that.TOTAL_LINES_LIMIT + 1) {
        source = source.slice(0, that.TOTAL_LINES_LIMIT);
        that.model.set({ hasSourceAfter: true });
      }
      if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
        source.unshift({ line: 0 });
      }
      source = source.map(function (row) {
        return _.extend(row, {
          utCoverageStatus: that.getUTCoverageStatus(row),
          itCoverageStatus: that.getItCoverageStatus(row)
        });
      });
      that.model.set({
        source: source,
        hasUTCoverage: that.model.hasUTCoverage(source),
        hasITCoverage: that.model.hasITCoverage(source),
        hasSourceBefore: (data.sources.length === that.LINES_AROUND) && (_.first(source).line > 0)
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

  loadSourceAfter: function () {
    this.unbindScrollEvents();
    var that = this,
        source = this.model.get('source'),
        lastLine = _.last(source).line,
        url = baseUrl + '/api/sources/lines',
        options = {
          uuid: this.model.id,
          from: lastLine + 1,
          to: lastLine + this.LINES_AROUND
        };
    return $.get(url, options).done(function (data) {
      source = source.concat(data.sources);
      if (source.length > that.TOTAL_LINES_LIMIT + 1) {
        source = source.slice(source.length - that.TOTAL_LINES_LIMIT);
        that.model.set({ hasSourceBefore: true });
      }
      source = source.map(function (row) {
        return _.extend(row, {
          utCoverageStatus: that.getUTCoverageStatus(row),
          itCoverageStatus: that.getItCoverageStatus(row)
        });
      });
      that.model.set({
        source: source,
        hasUTCoverage: that.model.hasUTCoverage(source),
        hasITCoverage: that.model.hasITCoverage(source),
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
    }).fail(function () {
      that.model.set({
        hasSourceAfter: false
      });
      that.render();
      if (that.model.get('hasSourceBefore') || that.model.get('hasSourceAfter')) {
        that.bindScrollEvents();
      }
    });
  },

  filterLines: function (func) {
    var lines = this.model.get('source'),
        $lines = this.$('.source-line');
    this.model.set('filterLinesFunc', func);
    lines.forEach(function (line, idx) {
      var $line = $($lines[idx]),
          filtered = func(line) && line.line > 0;
      $line.toggleClass('source-line-shadowed', !filtered);
      $line.toggleClass('source-line-filtered', filtered);
    });
  },

  filterLinesByDate: function (date, label) {
    var sinceDate = moment(date).toDate();
    this.sinceLabel = label;
    this.filterLines(function (line) {
      var scmDate = moment(line.scmDate).toDate();
      return scmDate >= sinceDate;
    });
  },

  showFilteredTooltip: function (e) {
    $(e.currentTarget).tooltip({
      container: 'body',
      placement: 'right',
      title: window.tp('source_viewer.tooltip.new_code', this.sinceLabel),
      trigger: 'manual'
    }).tooltip('show');
  },

  hideFilteredTooltip: function (e) {
    $(e.currentTarget).tooltip('destroy');
  },

  toggleIssueLocations: function (issue) {
    if (this.locationsShowFor === issue) {
      this.hideIssueLocations();
    } else {
      this.hideIssueLocations();
      this.showIssueLocations(issue);
    }
  },

  showIssueLocations: function (issue) {
    this.locationsShowFor = issue;
    var primaryLocation = {
          msg: issue.get('message'),
          textRange: issue.get('textRange')
        },
        _locations = [primaryLocation];
    issue.get('flows').forEach(function (flow) {
      var flowLocationsCount = _.size(flow.locations);
      var flowLocations = flow.locations.map(function (location, index) {
        var _location = _.extend({}, location);
        if (flowLocationsCount > 1) {
          _.extend(_location, { index: flowLocationsCount - index });
        }
        return _location;
      });
      _locations = [].concat(_locations, flowLocations);
    });
    _locations.forEach(this.showIssueLocation, this);
  },

  showIssueLocation: function (location, index) {
    if (location && location.textRange) {
      var line = location.textRange.startLine,
          row = this.$('.source-line-code[data-line-number="' + line + '"]');

      if (index > 0 && _.size(location.msg)) {
        // render location marker only for
        // secondary locations and execution flows
        // and only if message is not empty
        var renderedFlowLocation = this.renderIssueLocation(location);
        row.find('.source-line-issue-locations').prepend(renderedFlowLocation);
      }

      this.highlightIssueLocationInCode(location);
    }
  },

  renderIssueLocation: function (location) {
    location.msg = location.msg ? location.msg : ' ';
    return this.issueLocationTemplate(location);
  },

  highlightIssueLocationInCode: function (location) {
    for (var line = location.textRange.startLine; line <= location.textRange.endLine; line++) {
      var row = this.$('.source-line-code[data-line-number="' + line + '"]');

      // get location for the current line
      var from = line === location.textRange.startLine ? location.textRange.startOffset : 0,
          to = line === location.textRange.endLine ? location.textRange.endOffset : 999999,
          _location = { from: from, to: to };

      // mark issue location in the source code
      var codeEl = row.find('.source-line-code-inner > pre'),
          code = codeEl.html(),
          newCode = highlightLocations(code, [_location], 'source-line-code-secondary-issue');
      codeEl.html(newCode);
    }
  },

  hideIssueLocations: function () {
    this.locationsShowFor = null;
    this.$('.source-line-issue-locations').empty();
    this.$('.source-line-code-secondary-issue').removeClass('source-line-code-secondary-issue');
  }
});
