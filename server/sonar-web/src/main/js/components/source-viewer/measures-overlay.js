/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import ModalView from '../common/modals';
import Template from './templates/source-viewer-measures.hbs';


export default ModalView.extend({
  template: Template,
  testsOrder: ['ERROR', 'FAILURE', 'OK', 'SKIPPED'],

  initialize: function () {
    var that = this,
        requests = [this.requestMeasures(), this.requestIssues()];
    if (this.model.get('isUnitTest')) {
      requests.push(this.requestTests());
    }
    this.testsScroll = 0;
    $.when.apply($, requests).done(function () {
      that.render();
    });
  },

  events: function () {
    return _.extend(ModalView.prototype.events.apply(this, arguments), {
      'click .js-sort-tests-by-duration': 'sortTestsByDuration',
      'click .js-sort-tests-by-name': 'sortTestsByName',
      'click .js-sort-tests-by-status': 'sortTestsByStatus',
      'click .js-show-test': 'showTest',
      'click .js-show-all-measures': 'showAllMeasures'
    });
  },

  initPieChart: function () {
    var trans = function (left, top) {
      return 'translate(' + left + ', ' + top + ')';
    };

    var defaults = {
      size: 40,
      thickness: 8,
      color: '#1f77b4',
      baseColor: '#e6e6e6'
    };

    this.$('.js-pie-chart').each(function () {
      var data = [
            $(this).data('value'),
            $(this).data('max') - $(this).data('value')
          ],
          options = _.defaults($(this).data(), defaults),
          radius = options.size / 2;

      var container = d3.select(this),
          svg = container.append('svg')
              .attr('width', options.size)
              .attr('height', options.size),
          plot = svg.append('g')
              .attr('transform', trans(radius, radius)),
          arc = d3.svg.arc()
              .innerRadius(radius - options.thickness)
              .outerRadius(radius),
          pie = d3.layout.pie()
              .sort(null)
              .value(function (d) {
                return d;
              }),
          colors = function (i) {
            return i === 0 ? options.color : options.baseColor;
          },
          sectors = plot.selectAll('path')
              .data(pie(data));

      sectors.enter()
          .append('path')
          .style('fill', function (d, i) {
            return colors(i);
          })
          .attr('d', arc);
    });
  },

  onRender: function () {
    ModalView.prototype.onRender.apply(this, arguments);
    this.initPieChart();
    this.$('.js-test-list').scrollTop(this.testsScroll);
  },

  getMetrics: function () {
    var metrics = '',
        url = '/api/metrics/search';
    $.ajax({
      url: url,
      async: false,
      data: { ps: 9999 }
    }).done(function (data) {
      metrics = _.filter(data.metrics, function (metric) {
        return metric.type !== 'DATA' && !metric.hidden;
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
    return _.sortBy(
        _.map(_.pairs(_.groupBy(metrics, 'domain')), function (domain) {
          return {
            name: domain[0],
            metrics: domain[1]
          };
        }),
        'name'
    );
  },


  requestMeasures: function () {
    var that = this,
        url = '/api/resources',
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
        url = '/api/issues/search',
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
        url = '/api/tests/list',
        options = { testFileId: this.model.id };
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
        testId = $(e.currentTarget).data('id'),
        url = '/api/tests/covered_files',
        options = { testId: testId };
    this.testsScroll = $(e.currentTarget).scrollParent().scrollTop();
    return $.get(url, options).done(function (data) {
      that.coveredFiles = data.files;
      that.selectedTest = _.findWhere(that.model.get('tests'), { id: testId });
      that.render();
    });
  },

  showAllMeasures: function () {
    this.$('.js-all-measures').removeClass('hidden');
    this.$('.js-show-all-measures').remove();
  },

  serializeData: function () {
    return _.extend(ModalView.prototype.serializeData.apply(this, arguments), {
      testSorting: this.testSorting,
      selectedTest: this.selectedTest,
      coveredFiles: this.coveredFiles || []
    });
  }
});


