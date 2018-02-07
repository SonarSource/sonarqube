/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { select } from 'd3-selection';
import { arc as d3Arc, pie as d3Pie } from 'd3-shape';
import { groupBy, sortBy, toPairs } from 'lodash';
import Template from './templates/source-viewer-measures.hbs';
import ModalView from '../../common/modals';
import { searchIssues } from '../../../api/issues';
import { getMeasures } from '../../../api/measures';
import { getAllMetrics } from '../../../api/metrics';
import { getTests, getCoveredFiles } from '../../../api/tests';
import * as theme from '../../../app/theme';
import { getLocalizedMetricName, getLocalizedMetricDomain } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

const severityComparator = severity => {
  const SEVERITIES_ORDER = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
  return SEVERITIES_ORDER.indexOf(severity);
};

export default ModalView.extend({
  template: Template,
  testsOrder: ['ERROR', 'FAILURE', 'OK', 'SKIPPED'],

  initialize() {
    this.testsScroll = 0;
    const requests = [this.requestMeasures(), this.requestIssues()];
    if (this.options.component.q === 'UTS') {
      requests.push(this.requestTests());
    }
    Promise.all(requests).then(() => this.render());
  },

  events() {
    return {
      ...ModalView.prototype.events.apply(this, arguments),
      'click .js-sort-tests-by-duration': 'sortTestsByDuration',
      'click .js-sort-tests-by-name': 'sortTestsByName',
      'click .js-sort-tests-by-status': 'sortTestsByStatus',
      'click .js-show-test': 'showTest',
      'click .js-show-all-measures': 'showAllMeasures'
    };
  },

  initPieChart() {
    const trans = function(left, top) {
      return `translate(${left}, ${top})`;
    };

    const defaults = {
      size: 40,
      thickness: 8,
      color: '#1f77b4',
      baseColor: theme.barBorderColor
    };

    this.$('.js-pie-chart').each(function() {
      const data = [$(this).data('value'), $(this).data('max') - $(this).data('value')];
      const options = { ...defaults, ...$(this).data() };
      const radius = options.size / 2;

      const container = select(this);
      const svg = container
        .append('svg')
        .attr('width', options.size)
        .attr('height', options.size);
      const plot = svg.append('g').attr('transform', trans(radius, radius));
      const arc = d3Arc()
        .innerRadius(radius - options.thickness)
        .outerRadius(radius);
      const pie = d3Pie()
        .sort(null)
        .value(d => d);
      const colors = function(i) {
        return i === 0 ? options.color : options.baseColor;
      };
      const sectors = plot.selectAll('path').data(pie(data));

      sectors
        .enter()
        .append('path')
        .style('fill', (d, i) => colors(i))
        .attr('d', arc);
    });
  },

  onRender() {
    ModalView.prototype.onRender.apply(this, arguments);
    this.initPieChart();
    this.$('.js-test-list').scrollTop(this.testsScroll);
  },

  calcAdditionalMeasures(measures) {
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

  prepareMetrics(metrics) {
    metrics = metrics
      .filter(metric => metric.value != null)
      .map(metric => ({ ...metric, name: getLocalizedMetricName(metric) }));
    return sortBy(
      toPairs(groupBy(metrics, 'domain')).map(domain => {
        return {
          name: getLocalizedMetricDomain(domain[0]),
          metrics: domain[1]
        };
      }),
      'name'
    );
  },

  requestMeasures() {
    return getAllMetrics().then(metrics => {
      const metricsToRequest = metrics
        .filter(metric => metric.type !== 'DATA' && !metric.hidden)
        .map(metric => metric.key);

      return getMeasures(this.options.component.key, metricsToRequest, this.options.branch).then(
        measures => {
          let nextMeasures = this.options.component.measures || {};
          measures.forEach(measure => {
            const metric = metrics.find(metric => metric.key === measure.metric);
            nextMeasures[metric.key] = formatMeasure(measure.value, metric.type);
            nextMeasures[metric.key + '_raw'] = measure.value;
            metric.value = nextMeasures[metric.key];
          });
          nextMeasures = this.calcAdditionalMeasures(nextMeasures);
          this.measures = nextMeasures;
          this.measuresToDisplay = this.prepareMetrics(metrics);
        },
        () => {}
      );
    });
  },

  requestIssues() {
    const options = {
      branch: this.options.branch,
      componentKeys: this.options.component.key,
      resolved: false,
      ps: 1,
      facets: 'types,severities,tags'
    };

    return searchIssues(options).then(
      data => {
        const typesFacet = data.facets.find(facet => facet.property === 'types').values;
        const typesOrder = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
        const sortedTypesFacet = sortBy(typesFacet, v => typesOrder.indexOf(v.val));

        const severitiesFacet = data.facets.find(facet => facet.property === 'severities').values;
        const sortedSeveritiesFacet = sortBy(severitiesFacet, facet =>
          severityComparator(facet.val)
        );

        const tagsFacet = data.facets.find(facet => facet.property === 'tags').values;

        this.tagsFacet = tagsFacet;
        this.typesFacet = sortedTypesFacet;
        this.severitiesFacet = sortedSeveritiesFacet;
        this.issuesCount = data.total;
      },
      () => {}
    );
  },

  requestTests() {
    return getTests({ branch: this.options.branch, testFileKey: this.options.component.key }).then(
      data => {
        this.tests = data.tests;
        this.testSorting = 'status';
        this.testAsc = true;
        this.sortTests(test => `${this.testsOrder.indexOf(test.status)}_______${test.name}`);
      },
      () => {}
    );
  },

  sortTests(condition) {
    let tests = this.tests;
    if (Array.isArray(tests)) {
      tests = sortBy(tests, condition);
      if (!this.testAsc) {
        tests.reverse();
      }
      this.tests = tests;
    }
  },

  sortTestsByDuration() {
    if (this.testSorting === 'duration') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests('durationInMs');
    this.testSorting = 'duration';
    this.render();
  },

  sortTestsByName() {
    if (this.testSorting === 'name') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests('name');
    this.testSorting = 'name';
    this.render();
  },

  sortTestsByStatus() {
    if (this.testSorting === 'status') {
      this.testAsc = !this.testAsc;
    }
    this.sortTests(test => `${this.testsOrder.indexOf(test.status)}_______${test.name}`);
    this.testSorting = 'status';
    this.render();
  },

  showTest(e) {
    const testId = $(e.currentTarget).data('id');
    this.testsScroll = $(e.currentTarget)
      .scrollParent()
      .scrollTop();
    getCoveredFiles({ testId }).then(
      data => {
        this.coveredFiles = data.files;
        this.selectedTest = this.tests.find(test => test.id === testId);
        this.render();
      },
      () => {}
    );
  },

  showAllMeasures() {
    this.$('.js-all-measures').removeClass('hidden');
    this.$('.js-show-all-measures').remove();
  },

  serializeData() {
    return {
      ...ModalView.prototype.serializeData.apply(this, arguments),
      ...this.options.component,
      measures: this.measures,
      measuresToDisplay: this.measuresToDisplay,
      tests: this.tests,
      tagsFacet: this.tagsFacet,
      typesFacet: this.typesFacet,
      severitiesFacet: this.severitiesFacet,
      issuesCount: this.issuesCount,
      testSorting: this.testSorting,
      selectedTest: this.selectedTest,
      coveredFiles: this.coveredFiles || []
    };
  }
});
