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
import groupBy from 'lodash/groupBy';
import sortBy from 'lodash/sortBy';
import toPairs from 'lodash/toPairs';
import ModalView from '../common/modals';
import Template from './templates/source-viewer-measures.hbs';
import { getMeasures } from '../../api/measures';
import { getMetrics } from '../../api/metrics';
import { formatMeasure } from '../../helpers/measures';

export default ModalView.extend({
  template: Template,
  testsOrder: ['ERROR', 'FAILURE', 'OK', 'SKIPPED'],

  initialize () {
    this.testsScroll = 0;
    const requests = [this.requestMeasures(), this.requestIssues()];
    if (this.model.get('isUnitTest')) {
      requests.push(this.requestTests());
    }
    Promise.all(requests).then(() => this.render());
  },

  events () {
    return {
      ...ModalView.prototype.events.apply(this, arguments),
      'click .js-sort-tests-by-duration': 'sortTestsByDuration',
      'click .js-sort-tests-by-name': 'sortTestsByName',
      'click .js-sort-tests-by-status': 'sortTestsByStatus',
      'click .js-show-test': 'showTest',
      'click .js-show-all-measures': 'showAllMeasures'
    };
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
      const options = { ...defaults, ...$(this).data() };
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
          .value(d => d);
      const colors = function (i) {
        return i === 0 ? options.color : options.baseColor;
      };
      const sectors = plot.selectAll('path')
          .data(pie(data));

      sectors.enter()
          .append('path')
          .style('fill', (d, i) => colors(i))
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
    }).done(data => {
      metrics = data.metrics.filter(metric => metric.type !== 'DATA' && !metric.hidden);
      metrics = sortBy(metrics, 'name');
    });
    return metrics;
  },

  calcAdditionalMeasures (measures) {
    measures.issuesRemediationEffort =
        (Number(measures.sqale_index_raw) || 0) +
        (Number(measures.reliability_remediation_effort_raw) || 0) +
        (Number(measures.security_remediation_effort_raw) || 0);

    if (measures.lines_to_cover && measures.uncovered_lines) {
      measures.covered_lines = measures.lines_to_cover_raw - measures.uncovered_lines_raw;
    }
    if (measures.conditions_to_cover && measures.uncovered_conditions) {
      measures.covered_conditions = measures.conditions_to_cover - measures.uncovered_conditions;
    }
    return measures;
  },

  prepareMetrics (metrics) {
    metrics = metrics.filter(metric => metric.value != null);
    return sortBy(
        toPairs(groupBy(metrics, 'domain')).map(domain => {
          return {
            name: domain[0],
            metrics: domain[1]
          };
        }),
        'name'
    );
  },

  requestMeasures () {
    return getMetrics().then(metrics => {
      const metricsToRequest = metrics
          .filter(metric => metric.type !== 'DATA' && !metric.hidden)
          .map(metric => metric.key);

      return getMeasures(this.model.key(), metricsToRequest).then(measures => {
        let nextMeasures = this.model.get('measures') || {};
        measures.forEach(measure => {
          const metric = metrics.find(metric => metric.key === measure.metric);
          nextMeasures[metric.key] = formatMeasure(measure.value, metric.type);
          nextMeasures[metric.key + '_raw'] = measure.value;
          metric.value = nextMeasures[metric.key];
        });
        nextMeasures = this.calcAdditionalMeasures(nextMeasures);
        this.model.set({
          measures: nextMeasures,
          measuresToDisplay: this.prepareMetrics(metrics)
        });
      });
    });
  },

  requestIssues () {
    return new Promise(resolve => {
      const that = this;
      const url = window.baseUrl + '/api/issues/search';
      const options = {
        componentUuids: this.model.id,
        resolved: false,
        ps: 1,
        facets: 'types,severities,tags'
      };

      $.get(url, options).done(data => {
        const typesFacet = data.facets.find(facet => facet.property === 'types').values;
        const typesOrder = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
        const sortedTypesFacet = sortBy(typesFacet, v => typesOrder.indexOf(v.val));

        const severitiesFacet = data.facets.find(facet => facet.property === 'severities').values;
        const sortedSeveritiesFacet = sortBy(severitiesFacet, facet => window.severityComparator(facet.val));

        const tagsFacet = data.facets.find(facet => facet.property === 'tags').values;

        that.model.set({
          tagsFacet,
          typesFacet: sortedTypesFacet,
          severitiesFacet: sortedSeveritiesFacet,
          issuesCount: data.total
        });

        resolve();
      });
    });
  },

  requestTests () {
    return new Promise(resolve => {
      const that = this;
      const url = window.baseUrl + '/api/tests/list';
      const options = { testFileId: this.model.id };

      $.get(url, options).done(data => {
        that.model.set({ tests: data.tests });
        that.testSorting = 'status';
        that.testAsc = true;
        that.sortTests(test => `${that.testsOrder.indexOf(test.status)}_______${test.name}`);
        resolve();
      });
    });
  },

  sortTests (condition) {
    let tests = this.model.get('tests');
    if (Array.isArray(tests)) {
      tests = sortBy(tests, condition);
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
    this.sortTests(test => `${that.testsOrder.indexOf(test.status)}_______${test.name}`);
    this.testSorting = 'status';
    this.render();
  },

  showTest (e) {
    const that = this;
    const testId = $(e.currentTarget).data('id');
    const url = window.baseUrl + '/api/tests/covered_files';
    const options = { testId };
    this.testsScroll = $(e.currentTarget).scrollParent().scrollTop();
    return $.get(url, options).done(data => {
      that.coveredFiles = data.files;
      that.selectedTest = that.model.get('tests').find(test => test.id === testId);
      that.render();
    });
  },

  showAllMeasures () {
    this.$('.js-all-measures').removeClass('hidden');
    this.$('.js-show-all-measures').remove();
  },

  serializeData () {
    return {
      ...ModalView.prototype.serializeData.apply(this, arguments),
      testSorting: this.testSorting,
      selectedTest: this.selectedTest,
      coveredFiles: this.coveredFiles || []
    };
  }
});

