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

  initialize () {
    const that = this;
    const requests = [this.requestMeasures(), this.requestIssues()];
    if (this.model.get('isUnitTest')) {
      requests.push(this.requestTests());
    }
    this.testsScroll = 0;
    $.when.apply($, requests).done(function () {
      that.render();
    });
  },

  events () {
    return _.extend(ModalView.prototype.events.apply(this, arguments), {
      'click .js-sort-tests-by-duration': 'sortTestsByDuration',
      'click .js-sort-tests-by-name': 'sortTestsByName',
      'click .js-sort-tests-by-status': 'sortTestsByStatus',
      'click .js-show-test': 'showTest',
      'click .js-show-all-measures': 'showAllMeasures'
    });
  },

  initPieChart () {
    const trans = function (left, top) {
      return `translate(${left}, ${top})`;
    };

    const defaults = {
      size: 40,
      thickness: 8,
      color: '#1f77b4',
      baseColor: '#e6e6e6'
    };

    this.$('.js-pie-chart').each(function () {
      const data = [
        $(this).data('value'),
        $(this).data('max') - $(this).data('value')
      ];
      const options = _.defaults($(this).data(), defaults);
      const radius = options.size / 2;

      const container = d3.select(this);
      const svg = container.append('svg')
          .attr('width', options.size)
          .attr('height', options.size);
      const plot = svg.append('g')
          .attr('transform', trans(radius, radius));
      const arc = d3.svg.arc()
          .innerRadius(radius - options.thickness)
          .outerRadius(radius);
      const pie = d3.layout.pie()
          .sort(null)
          .value(function (d) {
            return d;
          });
      const colors = function (i) {
        return i === 0 ? options.color : options.baseColor;
      };
      const sectors = plot.selectAll('path')
          .data(pie(data));

      sectors.enter()
          .append('path')
          .style('fill', function (d, i) {
            return colors(i);
          })
          .attr('d', arc);
    });
  },

  onRender () {
    ModalView.prototype.onRender.apply(this, arguments);
    this.initPieChart();
    this.$('.js-test-list').scrollTop(this.testsScroll);
  },

  getMetrics () {
    let metrics = '';
    const url = window.baseUrl + '/api/metrics/search';
    $.ajax({
      url,
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


  calcAdditionalMeasures (measures) {
    if (measures.lines_to_cover && measures.uncovered_lines) {
      measures.covered_lines = measures.lines_to_cover_raw - measures.uncovered_lines_raw;
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


  prepareMetrics (metrics) {
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


  requestMeasures () {
    const that = this;
    const url = window.baseUrl + '/api/resources';
    const metrics = this.getMetrics();
    const options = {
      resource: this.model.key(),
      metrics: _.pluck(metrics, 'key').join()
    };
    return $.get(url, options).done(function (data) {
      const measuresList = data[0].msr || [];
      let measures = that.model.get('measures') || {};
      measuresList.forEach(function (m) {
        const metric = _.findWhere(metrics, { key: m.key });
        metric.value = m.frmt_val || m.data;
        measures[m.key] = m.frmt_val || m.data;
        measures[m.key + '_raw'] = m.val;
      });
      measures = that.calcAdditionalMeasures(measures);
      that.model.set({
        measures,
        measuresToDisplay: that.prepareMetrics(metrics)
      });
    });
  },

  requestIssues () {
    const that = this;
    const url = window.baseUrl + '/api/issues/search';
    const options = {
      componentUuids: this.model.id,
      resolved: false,
      ps: 1,
      facets: 'types,severities,tags'
    };
    return $.get(url, options).done(function (data) {
      const typesFacet = data.facets.find(facet => facet.property === 'types').values;
      const typesOrder = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
      const sortedTypesFacet = _.sortBy(typesFacet, function (v) {
        return typesOrder.indexOf(v.val);
      });

      const severitiesFacet = data.facets.find(facet => facet.property === 'severities').values;
      const sortedSeveritiesFacet = _.sortBy(severitiesFacet, facet => window.severityComparator(facet.val));

      const tagsFacet = data.facets.find(facet => facet.property === 'tags').values;

      that.model.set({
        tagsFacet,
        typesFacet: sortedTypesFacet,
        severitiesFacet: sortedSeveritiesFacet,
        issuesCount: data.total
      });
    });
  },

  requestTests () {
    const that = this;
    const url = window.baseUrl + '/api/tests/list';
    const options = { testFileId: this.model.id };
    return $.get(url, options).done(function (data) {
      that.model.set({ tests: data.tests });
      that.testSorting = 'status';
      that.testAsc = true;
      that.sortTests(function (test) {
        return `${that.testsOrder.indexOf(test.status)}_______${test.name}`;
      });
    });
  },

  sortTests (condition) {
    let tests = this.model.get('tests');
    if (_.isArray(tests)) {
      tests = _.sortBy(tests, condition);
      if (!this.testAsc) {
        tests.reverse();
      }
      this.model.set({ tests });
    }
  },

  sortTestsByDuration () {
    if (this.testSorting === 'duration') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests('durationInMs');
    this.testSorting = 'duration';
    this.render();
  },

  sortTestsByName () {
    if (this.testSorting === 'name') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests('name');
    this.testSorting = 'name';
    this.render();
  },

  sortTestsByStatus () {
    const that = this;
    if (this.testSorting === 'status') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests(function (test) {
      return `${that.testsOrder.indexOf(test.status)}_______${test.name}`;
    });
    this.testSorting = 'status';
    this.render();
  },

  showTest (e) {
    const that = this;
    const testId = $(e.currentTarget).data('id');
    const url = window.baseUrl + '/api/tests/covered_files';
    const options = { testId };
    this.testsScroll = $(e.currentTarget).scrollParent().scrollTop();
    return $.get(url, options).done(function (data) {
      that.coveredFiles = data.files;
      that.selectedTest = _.findWhere(that.model.get('tests'), { id: testId });
      that.render();
    });
  },

  showAllMeasures () {
    this.$('.js-all-measures').removeClass('hidden');
    this.$('.js-show-all-measures').remove();
  },

  serializeData () {
    return _.extend(ModalView.prototype.serializeData.apply(this, arguments), {
      testSorting: this.testSorting,
      selectedTest: this.selectedTest,
      coveredFiles: this.coveredFiles || []
    });
  }
});


