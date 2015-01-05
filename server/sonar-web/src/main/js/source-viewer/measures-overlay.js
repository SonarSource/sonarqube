define([
  'common/overlay',
  'templates/source-viewer'
], function (Overlay, Templates) {

  var $ = jQuery;


  return Overlay.extend({
    className: 'overlay-popup source-viewer-measures-overlay',
    template: Templates['source-viewer-measures'],
    testsOrder: ['ERROR', 'FAILURE', 'OK', 'SKIPPED'],

    events: function () {
      return _.extend(Overlay.prototype.events.apply(this, arguments), {
        'click .js-sort-tests-by-duration': 'sortTestsByDuration',
        'click .js-sort-tests-by-name': 'sortTestsByName',
        'click .js-sort-tests-by-status': 'sortTestsByStatus',
        'click .js-show-test': 'showTest'
      });
    },

    onRender: function () {
      Overlay.prototype.onRender.apply(this, arguments);
      this.$('.js-pie-chart').pieChart();
      if (this.selectedTest != null) {
        this.scrollToTest();
      }
    },

    scrollToTest: function () {
      var position = this.$('.js-selected-test').offset().top - this.$el.offset().top - 50;
      this.$el.scrollTop(position);
    },

    show: function () {
      var that = this,
          p = window.process.addBackgroundProcess(),
          requests = [this.requestMeasures(), this.requestIssues()];
      if (this.model.get('isUnitTest')) {
        requests.push(this.requestTests());
      }
      $.when.apply($, requests).done(function () {
        that.render();
        window.process.finishBackgroundProcess(p);
      }).fail(function () {
        window.process.failBackgroundProcess(p);
      });
    },

    getMetrics: function () {
      var metrics = '',
          url = baseUrl + '/api/metrics';
      $.ajax({
        url: url,
        async: false
      }).done(function (data) {
        metrics = _.filter(data, function (metric) {
          return !metric.hidden && metric.val_type !== 'DATA';
        });
        metrics = _.sortBy(metrics, 'name');
      });
      return metrics;
    },


    calcAdditionalMeasures: function (measures) {
      if (measures.lines_to_cover && measures.uncovered_lines) {
        measures.covered_lines = measures.lines_to_cover - measures.uncovered_lines;
      }
      if (measures.conditions_to_cover && measures.uncovered_conditions) {
        measures.covered_conditions = measures.conditions_to_cover - measures.uncovered_conditions;
      }
      if (measures.it_lines_to_cover && measures.it_uncovered_lines) {
        measures.it_covered_lines = measures.it_lines_to_cover - measures.it_uncovered_lines;
      }
      if (measures.it_conditions_to_cover && measures.it_uncovered_conditions) {
        measures.it_covered_conditions = measures.it_conditions_to_cover - measures.it_uncovered_conditions;
      }
      return measures;
    },


    prepareMetrics: function (metrics) {
      metrics = _.filter(metrics, function (metric) {
        return metric.value != null;
      });
      return _.map(_.pairs(_.groupBy(metrics, 'domain')), function (domain) {
        return {
          name: domain[0],
          metrics: domain[1]
        };
      });
    },


    requestMeasures: function () {
      var that = this,
          url = baseUrl + '/api/resources',
          metrics = this.getMetrics(),
          options = {
            resource: this.model.key(),
            metrics: _.pluck(metrics, 'key').join()
          };
      return $.get(url, options).done(function (data) {
        var measuresList = data[0].msr || [],
            measures = that.model.get('measures') || {};
        measuresList.forEach(function (m) {
          var metric = _.findWhere(metrics, { key: m.key });
          metric.value = m.frmt_val || m.data;
          measures[m.key] = m.frmt_val || m.data;
          measures[m.key + '_raw'] = m.val;
        });
        measures = that.calcAdditionalMeasures(measures);
        that.model.set({
          measures: measures,
          measuresToDisplay: that.prepareMetrics(metrics)
        });
      });
    },

    requestIssues: function () {
      var that = this,
          url = baseUrl + '/api/issues/search',
          options = {
            componentUuids: this.model.id,
            resolved: false,
            ps: 1,
            facets: 'severities,tags'
          };
      return $.get(url, options).done(function (data) {
        var issuesFacets = {};
        data.facets.forEach(function (facet) {
          issuesFacets[facet.property] = facet.values;
        });
        var severityOrder = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
            maxCountBySeverity = _.max(issuesFacets.severities, function (s) {
              return s.count;
            }).count,
            maxCountByTag = _.max(issuesFacets.tags, function (s) {
              return s.count;
            }).count;
        issuesFacets.severities = _.sortBy(issuesFacets.severities, function (s) {
          return severityOrder.indexOf(s.val);
        });
        that.model.set({
          issuesFacets: issuesFacets,
          issuesCount: data.total,
          maxCountBySeverity: maxCountBySeverity,
          maxCountByTag: maxCountByTag
        });
      });
    },

    requestTests: function () {
      var that = this,
          url = baseUrl + '/api/tests/show',
          options = { key: this.model.key() };
      return $.get(url, options).done(function (data) {
        that.model.set({ tests: data.tests });
        that.testSorting = 'status';
        that.testAsc = true;
        that.sortTests(function (test) {
          return '' + that.testsOrder.indexOf(test.status) + '_______' + test.name;
        });
      });
    },

    sortTests: function (condition) {
      var tests = this.model.get('tests');
      if (_.isArray(tests)) {
        tests = _.sortBy(tests, condition);
        if (!this.testAsc) {
          tests.reverse();
        }
        this.model.set({ tests: tests });
      }
    },

    sortTestsByDuration: function () {
      if (this.testSorting === 'duration') {
        this.testAsc = !this.testAsc;
      }
      this.sortTests('durationInMs');
      this.testSorting = 'duration';
      this.render();
    },

    sortTestsByName: function () {
      if (this.testSorting === 'name') {
        this.testAsc = !this.testAsc;
      }
      this.sortTests('name');
      this.testSorting = 'name';
      this.render();
    },

    sortTestsByStatus: function () {
      var that = this;
      if (this.testSorting === 'status') {
        this.testAsc = !this.testAsc;
      }
      this.sortTests(function (test) {
        return '' + that.testsOrder.indexOf(test.status) + '_______' + test.name;
      });
      this.testSorting = 'status';
      this.render();
    },

    showTest: function (e) {
      var that = this,
          p = window.process.addBackgroundProcess(),
          name = $(e.currentTarget).data('name'),
          url = baseUrl + '/api/tests/covered_files',
          options = {
            key: this.model.key(),
            test: name
          };
      return $.get(url, options).done(function (data) {
        that.coveredFiles = data.files;
        that.selectedTest = _.findWhere(that.model.get('tests'), { name: name });
        that.render();
        window.process.finishBackgroundProcess(p);
      });
    },

    serializeData: function () {
      return _.extend(Overlay.prototype.serializeData.apply(this, arguments), {
        testSorting: this.testSorting,
        selectedTest: this.selectedTest,
        coveredFiles: this.coveredFiles || []
      });
    }
  });

});
