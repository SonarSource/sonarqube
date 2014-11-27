define([
  'backbone',
  'backbone.marionette',
  'templates/source-viewer',
  'source-viewer/source',
  'issue/models/issue',
  'issue/collections/issues',
  'issue/issue-view',
  'source-viewer/popups/coverage-popup',
  'source-viewer/popups/duplication-popup',
  'source-viewer/popups/line-actions-popup'
], function (Backbone, Marionette, Templates, Source, Issue, Issues, IssueView, CoveragePopupView, DuplicationPopupView, LineActionsPopupView) {

  var $ = jQuery,
      HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted',
      log = function (message) {
        return console.log('Source Viewer:', message);
      };

  return Marionette.ItemView.extend({
    className: 'source',
    template: Templates['source-viewer'],

    ISSUES_LIMIT: 100,
    LINES_LIMIT: 1000,
    LINES_AROUND: 500,

    ui: {
      sourceBeforeSpinner: '.js-component-viewer-source-before',
      sourceAfterSpinner: '.js-component-viewer-source-after'
    },

    events: function () {
      return {
        'click .source-line-covered': 'showCoveragePopup',
        'click .source-line-partially-covered': 'showCoveragePopup',
        'click .source-line-duplications': 'showDuplications',
        'click .source-line-duplications-extra': 'showDuplicationPopup',
        'click .source-line-number[data-line-number]': 'highlightLine'
      };
    },

    initialize: function () {
      if (this.model == null) {
        this.model = new Source();
      }
      this.issues = new Issues();
      this.issueViews = [];
      this.loadSourceBeforeThrottled = _.throttle(this.loadSourceBefore, 1000);
      this.loadSourceAfterThrottled = _.throttle(this.loadSourceAfter, 1000);
      this.scrollTimer = null;
    },

    onRender: function () {
      log('Render');
      this.renderIssues();
      return this;
    },

    onClose: function () {
      this.issueViews.forEach(function (view) {
        return view.close();
      });
      this.issueViews = [];
    },

    open: function (id, key) {
      var that = this;
      this.model.clear();
      this.model.set({
        uuid: id,
        key: key
      });
      this.requestComponent().done(function () {
        that.requestSource()
            .done(function () {
              that.requestCoverage().done(function () {
                that.requestDuplications().done(function () {
                  that.requestIssues().done(function () {
                    that.render();
                    that.trigger('loaded');
                  });
                });
              });
            })
            .fail(function () {
              that.model.set({
                source: [
                  { line: 0 }
                ]
              });
              that.requestIssues().done(function () {
                that.render();
                that.trigger('loaded');
              });
            });
      });
      return this;
    },

    requestComponent: function () {
      log('Request component details...');
      var that = this,
          url = baseUrl + '/api/components/app',
          options = { key: this.model.key() };
      return $.get(url, options).done(function (data) {
        that.model.set(data);
      });
    },

    linesLimit: function () {
      return {
        from: 1,
        to: this.LINES_LIMIT
      };
    },

    requestSource: function () {
      var options, url;
      log('Request source...');
      url = '' + baseUrl + '/api/sources/lines';
      options = _.extend({
        uuid: this.model.id
      }, this.linesLimit());
      return $.get(url, options, (function (_this) {
        return function (data) {
          var firstLine, source;
          source = data.sources || [];
          if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
            source.unshift({
              line: 0
            });
          }
          firstLine = _.first(source).line;
          _this.model.set({
            source: source,
            hasSourceBefore: firstLine > 1,
            hasSourceAfter: true
          });
          return log('Source loaded');
        };
      })(this));
    },

    requestCoverage: function () {
      var options, url;
      log('Request coverage');
      url = '' + baseUrl + '/api/coverage/show';
      options = {
        key: this.model.key()
      };
      return $.get(url, options, (function (_this) {
        return function (data) {
          var coverage, hasCoverage;
          hasCoverage = (data != null) && (data.coverage != null);
          _this.model.set({
            hasCoverage: hasCoverage
          });
          if (hasCoverage) {
            coverage = data.coverage.map(function (c) {
              var status;
              status = 'partially-covered';
              if (c[1] && c[3] === c[4]) {
                status = 'covered';
              }
              if (!c[1] || c[4] === 0) {
                status = 'uncovered';
              }
              return {
                line: +c[0],
                covered: status
              };
            });
          } else {
            coverage = [];
          }
          _this.model.addMeta(coverage);
          return log('Coverage loaded');
        };
      })(this));
    },

    requestDuplications: function () {
      var options, url;
      log('Request duplications');
      url = '' + baseUrl + '/api/duplications/show';
      options = {
        key: this.model.key()
      };
      return $.get(url, options, (function (_this) {
        return function (data) {
          var duplications, hasDuplications;
          hasDuplications = (data != null) && (data.duplications != null);
          if (hasDuplications) {
            duplications = {};
            data.duplications.forEach(function (d, i) {
              return d.blocks.forEach(function (b) {
                var lineFrom, lineTo, _i, _results;
                if (b._ref === '1') {
                  lineFrom = b.from;
                  lineTo = b.from + b.size;
                  _results = [];
                  for (i = _i = lineFrom; lineFrom <= lineTo ? _i <= lineTo : _i >= lineTo; i = lineFrom <= lineTo ? ++_i : --_i) {
                    _results.push(duplications[i] = true);
                  }
                  return _results;
                }
              });
            });
            duplications = _.pairs(duplications).map(function (line) {
              return {
                line: +line[0],
                duplicated: line[1]
              };
            });
          } else {
            duplications = [];
          }
          _this.model.addMeta(duplications);
          _this.model.addDuplications(data.duplications);
          _this.model.set({
            duplications: data.duplications,
            duplicationFiles: data.files
          });
          return log('Duplications loaded');
        };
      })(this));
    },

    requestIssues: function () {
      var options;
      log('Request issues');
      options = {
        data: {
          componentUuids: this.model.id,
          extra_fields: 'actions,transitions,assigneeName,actionPlanName',
          resolved: false,
          s: 'FILE_LINE',
          asc: true
        }
      };
      return this.issues.fetch(options).done((function (_this) {
        return function () {
          _this.issues.reset(_this.limitIssues(_this.issues));
          _this.addIssuesPerLineMeta(_this.issues);
          return log('Issues loaded');
        };
      })(this));
    },

    addIssuesPerLineMeta: function (issues) {
      var issuesPerLine, lines;
      lines = {};
      issues.forEach(function (issue) {
        var line;
        line = issue.get('line') || 0;
        if (!_.isArray(lines[line])) {
          lines[line] = [];
        }
        return lines[line].push(issue.toJSON());
      });
      issuesPerLine = _.pairs(lines).map(function (line) {
        return {
          line: +line[0],
          issues: line[1]
        };
      });
      return this.model.addMeta(issuesPerLine);
    },

    limitIssues: function (issues) {
      return issues.first(this.ISSUES_LIMIT);
    },

    renderIssues: function () {
      log('Render issues');
      this.issues.forEach(this.renderIssue, this);
      return log('Issues rendered');
    },

    renderIssue: function (issue) {
      var issueView;
      issueView = new IssueView({
        el: '#issue-' + issue.get('key'),
        model: issue
      });
      this.issueViews.push(issueView);
      return issueView.render();
    },

    addIssue: function (issue) {
      var code, issueList, line;
      line = issue.get('line') || 0;
      code = this.$('.source-line-code[data-line-number=' + line + ']');
      issueList = code.find('.issue-list');
      if (issueList.length === 0) {
        issueList = $('<div class="issue-list"></div>');
        code.append(issueList);
      }
      issueList.append('<div class="issue" id="issue-' + issue.id + '"></div>');
      return this.renderIssue(issue);
    },

    showCoveragePopup: function (e) {
      var line, options, r, url;
      r = window.process.addBackgroundProcess();
      e.stopPropagation();
      $('body').click();
      line = $(e.currentTarget).data('line-number');
      url = '' + baseUrl + '/api/tests/test_cases';
      options = {
        key: this.model.key(),
        line: line
      };
      return $.get(url, options).done((function (_this) {
        return function (data) {
          var popup;
          popup = new CoveragePopupView({
            model: new Backbone.Model(data),
            triggerEl: $(e.currentTarget)
          });
          popup.render();
          return window.process.finishBackgroundProcess(r);
        };
      })(this)).fail(function () {
        return window.process.failBackgroundProcess(r);
      });
    },

    showDuplications: function () {
      this.$('.source-line-duplications').addClass('hidden');
      return this.$('.source-line-duplications-extra').removeClass('hidden');
    },

    showDuplicationPopup: function (e) {
      var blocks, index, line, popup;
      e.stopPropagation();
      $('body').click();
      index = $(e.currentTarget).data('index');
      line = $(e.currentTarget).data('line-number');
      blocks = this.model.get('duplications')[index - 1].blocks;
      blocks = _.filter(blocks, function (b) {
        return (b._ref !== '1') || (b._ref === '1' && b.from > line) || (b._ref === '1' && b.from + b.size < line);
      });
      popup = new DuplicationPopupView({
        triggerEl: $(e.currentTarget),
        model: this.model,
        collection: new Backbone.Collection(blocks)
      });
      return popup.render();
    },

    showLineActionsPopup: function (e) {
      var line, popup;
      e.stopPropagation();
      $('body').click();
      line = $(e.currentTarget).data('line-number');
      popup = new LineActionsPopupView({
        triggerEl: $(e.currentTarget),
        model: this.model,
        line: line,
        row: $(e.currentTarget).closest('.source-line')
      });
      popup.on('onManualIssueAdded', (function (_this) {
        return function (data) {
          return _this.addIssue(new Issue(data));
        };
      })(this));
      return popup.render();
    },

    highlightLine: function (e) {
      var highlighted, row;
      row = $(e.currentTarget).closest('.source-line');
      highlighted = row.is('.' + HIGHLIGHTED_ROW_CLASS);
      this.$('.' + HIGHLIGHTED_ROW_CLASS).removeClass(HIGHLIGHTED_ROW_CLASS);
      if (!highlighted) {
        row.addClass(HIGHLIGHTED_ROW_CLASS);
        return this.showLineActionsPopup(e);
      }
    },

    bindScrollEvents: function () {
      return this.$el.scrollParent().on('scroll.source-viewer', ((function (_this) {
        return function () {
          return _this.onScroll();
        };
      })(this)));
    },

    unbindScrollEvents: function () {
      return this.$el.scrollParent().off('scroll.source-viewer');
    },

    disablePointerEvents: function () {
      clearTimeout(this.scrollTimer);
      $('body').addClass('disabled-pointer-events');
      this.scrollTimer = setTimeout((function () {
        return $('body').removeClass('disabled-pointer-events');
      }), 250);
    },

    onScroll: function () {
      var p, pTopOffset;
      this.disablePointerEvents();
      p = this.$el.scrollParent();
      if (p.is(document)) {
        p = $(window);
      }
      pTopOffset = p.offset() != null ? p.offset().top : 0;
      if (this.model.get('hasSourceBefore') && (p.scrollTop() + pTopOffset <= this.ui.sourceBeforeSpinner.offset().top)) {
        this.loadSourceBeforeThrottled();
      }
      if (this.model.get('hasSourceAfter') && (p.scrollTop() + pTopOffset + p.height() >= this.ui.sourceAfterSpinner.offset().top)) {
        return this.loadSourceAfterThrottled();
      }
    },

    loadSourceBefore: function () {
      var firstLine, options, source, url;
      this.unbindScrollEvents();
      source = this.model.get('source');
      firstLine = _.first(source).line;
      url = '' + baseUrl + '/api/sources/lines';
      options = {
        uuid: this.model.id,
        from: firstLine - this.LINES_AROUND,
        to: firstLine - 1
      };
      return $.get(url, options, (function (_this) {
        return function (data) {
          source = (data.sources || []).concat(source);
          if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
            source.unshift({
              line: 0
            });
          }
          _this.model.set({
            source: source,
            hasSourceBefore: data.sources.length === _this.LINES_AROUND
          });
          _this.render();
          _this.scrollToLine(firstLine);
          if (_this.model.get('hasSourceBefore') || _this.model.get('hasSourceAfter')) {
            return _this.bindScrollEvents();
          }
        };
      })(this));
    },

    loadSourceAfter: function () {
      var lastLine, options, source, url;
      this.unbindScrollEvents();
      source = this.model.get('source');
      lastLine = _.last(source).line;
      url = '' + baseUrl + '/api/sources/lines';
      options = {
        uuid: this.model.id,
        from: lastLine + 1,
        to: lastLine + this.LINES_AROUND
      };
      return $.get(url, options).done((function (_this) {
        return function (data) {
          source = source.concat(data.sources);
          _this.model.set({
            source: source,
            hasSourceAfter: data.sources.length === _this.LINES_AROUND
          });
          _this.render();
          if (_this.model.get('hasSourceBefore') || _this.model.get('hasSourceAfter')) {
            return _this.bindScrollEvents();
          }
        };
      })(this)).fail((function (_this) {
        return function () {
          _this.model.set({
            hasSourceAfter: false
          });
          _this.render();
          if (_this.model.get('hasSourceBefore') || _this.model.get('hasSourceAfter')) {
            return _this.bindScrollEvents();
          }
        };
      })(this));
    }
  });

});

