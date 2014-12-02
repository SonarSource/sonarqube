define([
      'backbone',
      'backbone.marionette',
      'templates/source-viewer',
      'source-viewer/source',
      'issue/models/issue',
      'issue/collections/issues',
      'issue/issue-view',
      'source-viewer/popups/scm-popup',
      'source-viewer/popups/coverage-popup',
      'source-viewer/popups/duplication-popup',
      'source-viewer/popups/line-actions-popup'
    ],
    function (Backbone,
              Marionette,
              Templates,
              Source,
              Issue,
              Issues,
              IssueView,
              SCMPopupView,
              CoveragePopupView,
              DuplicationPopupView,
              LineActionsPopupView) {

      var $ = jQuery,
          HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted';

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
            'click .source-line-scm': 'showSCMPopup',
            'click .source-line-covered': 'showCoveragePopup',
            'click .source-line-partially-covered': 'showCoveragePopup',
            'click .source-line-uncovered': 'showCoveragePopup',
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
          this.renderIssues();
        },

        onClose: function () {
          this.issueViews.forEach(function (view) {
            return view.close();
          });
          this.issueViews = [];
        },

        open: function (id, key) {
          var that = this,
              finalize = function () {
                that.requestIssues().done(function () {
                  that.render();
                  that.trigger('loaded');
                });
              };
          this.model.clear();
          this.model.set({
            uuid: id,
            key: key
          });
          this.requestComponent().done(function () {
            that.requestSource()
                .done(finalize)
                .fail(function () {
                  that.model.set({ source: [
                    { line: 0 }
                  ] });
                  finalize();
                });
          });
          return this;
        },

        requestComponent: function () {
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

        getCoverageStatus: function (row) {
          var status = null;
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

        requestSource: function () {
          var that = this,
              url = baseUrl + '/api/sources/lines',
              options = _.extend({ uuid: this.model.id }, this.linesLimit());
          return $.get(url, options, function (data) {
            var source = (data.sources || []).slice(0);
            if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
              source.unshift({ line: 0 });
            }
            source = source.map(function (row) {
              return _.extend(row, { coverageStatus: that.getCoverageStatus(row) });
            });
            var firstLine = _.first(source).line,
                linesRequested = options.to - options.from + 1;
            that.model.set({
              source: source,
              hasSourceBefore: firstLine > 1,
              hasSourceAfter: data.sources.length === linesRequested
            });
          });
        },

        requestDuplications: function () {
          var that = this,
              url = baseUrl + '/api/duplications/show',
              options = { key: this.model.key() };
          return $.get(url, options, function (data) {
            var hasDuplications = (data != null) && (data.duplications != null),
                duplications = [];
            if (hasDuplications) {
              duplications = {};
              data.duplications.forEach(function (d) {
                d.blocks.forEach(function (b) {
                  if (b._ref === '1') {
                    var lineFrom = b.from,
                        lineTo = b.from + b.size;
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
                  extra_fields: 'actions,transitions,assigneeName,actionPlanName',
                  resolved: false,
                  s: 'FILE_LINE',
                  asc: true
                }
              };
          return this.issues.fetch(options).done(function () {
            that.issues.reset(that.limitIssues(that.issues));
            that.addIssuesPerLineMeta(that.issues);
          });
        },

        addIssuesPerLineMeta: function (issues) {
          var lines = {};
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
              issues: line[1]
            };
          });
          this.model.addMeta(issuesPerLine);
        },

        limitIssues: function (issues) {
          return issues.first(this.ISSUES_LIMIT);
        },

        renderIssues: function () {
          this.issues.forEach(this.renderIssue, this);
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
              issueList = code.find('.issue-list');
          if (issueList.length === 0) {
            issueList = $('<div class="issue-list"></div>');
            code.append(issueList);
          }
          issueList.append('<div class="issue" id="issue-' + issue.id + '"></div>');
          this.renderIssue(issue);
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
          var r = window.process.addBackgroundProcess(),
              line = $(e.currentTarget).data('line-number'),
              row = _.findWhere(this.model.get('source'), { line: line }),
              url = baseUrl + '/api/tests/test_cases',
              options = {
                key: this.model.key(),
                line: line
              };
          return $.get(url, options).done(function (data) {
            var popup = new CoveragePopupView({
              model: new Backbone.Model(data),
              row: row,
              triggerEl: $(e.currentTarget)
            });
            popup.render();
            window.process.finishBackgroundProcess(r);
          }).fail(function () {
            window.process.failBackgroundProcess(r);
          });
        },

        showDuplications: function () {
          var that = this;
          this.requestDuplications().done(function () {
            that.render();
            that.$el.addClass('source-duplications-expanded');
          });
        },

        showDuplicationPopup: function (e) {
          e.stopPropagation();
          $('body').click();
          var index = $(e.currentTarget).data('index'),
              line = $(e.currentTarget).data('line-number'),
              blocks = this.model.get('duplications')[index - 1].blocks;
          blocks = _.filter(blocks, function (b) {
            return (b._ref !== '1') || (b._ref === '1' && b.from > line) || (b._ref === '1' && b.from + b.size < line);
          });
          var popup = new DuplicationPopupView({
            triggerEl: $(e.currentTarget),
            model: this.model,
            collection: new Backbone.Collection(blocks)
          });
          popup.render();
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
          popup.on('onManualIssueAdded', function (data) {
            that.addIssue(new Issue(data));
          });
          popup.render();
        },

        highlightLine: function (e) {
          var row = $(e.currentTarget).closest('.source-line'),
              highlighted = row.is('.' + HIGHLIGHTED_ROW_CLASS);
          this.$('.' + HIGHLIGHTED_ROW_CLASS).removeClass(HIGHLIGHTED_ROW_CLASS);
          if (!highlighted) {
            row.addClass(HIGHLIGHTED_ROW_CLASS);
            this.showLineActionsPopup(e);
          }
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

        disablePointerEvents: function () {
          clearTimeout(this.scrollTimer);
          $('body').addClass('disabled-pointer-events');
          this.scrollTimer = setTimeout((function () {
            $('body').removeClass('disabled-pointer-events');
          }), 250);
        },

        onScroll: function () {
          this.disablePointerEvents();
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
            if (source.length === 0 || (source.length > 0 && _.first(source).line === 1)) {
              source.unshift({ line: 0 });
            }
            that.model.set({
              source: source,
              hasSourceBefore: data.sources.length === that.LINES_AROUND
            });
            that.model.addDuplications(that.model.get('duplications'));
            that.model.addMeta(that.model.get('duplicationsParsed'));
            that.render();
            that.scrollToLine(firstLine);
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
            that.model.set({
              source: source,
              hasSourceAfter: data.sources.length === that.LINES_AROUND
            });
            that.model.addDuplications(that.model.get('duplications'));
            that.model.addMeta(that.model.get('duplicationsParsed'));
            that.render();
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
        }
      });

    });

