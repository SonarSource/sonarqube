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
import React from 'react';

import {
    Domain,
    DomainPanel,
    DomainNutshell,
    DomainLeak,
    MeasuresList,
    Measure,
    DomainMixin
} from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { DonutChart } from '../../../components/charts/donut-chart';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

export const GeneralCoverage = React.createClass({
  propTypes: {
    measures: React.PropTypes.object.isRequired,
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object,
    coverageMetricPrefix: React.PropTypes.string.isRequired
  },

  mixins: [TooltipsMixin, DomainMixin],

  getCoverageMetric () {
    return this.props.coverageMetricPrefix + 'coverage';
  },

  getNewCoverageMetric () {
    return 'new_' + this.props.coverageMetricPrefix + 'coverage';
  },

  renderNewCoverage () {
    const newCoverageMetric = this.getNewCoverageMetric();

    if (this.props.leak[newCoverageMetric] != null) {
      return <DrilldownLink component={this.props.component.key} metric={newCoverageMetric}
                            period={this.props.leakPeriodIndex}>
        <span className="js-overview-main-new-coverage">
          {formatMeasure(this.props.leak[newCoverageMetric], 'PERCENT')}
        </span>
      </DrilldownLink>;
    } else {
      return <span>—</span>;
    }
  },

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }

    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('new_coverage')}>{this.renderNewCoverage()}</Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  renderTests() {
    const tests = this.props.measures['tests'];
    if (tests == null) {
      return null;
    }
    return <Measure label={getMetricName('tests')}>
      <DrilldownLink component={this.props.component.key} metric="tests">
        <span className="js-overview-main-tests">{formatMeasure(tests, 'SHORT_INT')}</span>
      </DrilldownLink>
    </Measure>;
  },

  render () {
    const coverageMetric = this.getCoverageMetric();
    if (this.props.measures[coverageMetric] == null) {
      return null;
    }

    const donutData = [
      { value: this.props.measures[coverageMetric], fill: '#85bb43' },
      { value: 100 - this.props.measures[coverageMetric], fill: '#d4333f' }
    ];

    const domainUrl = window.baseUrl + '/component_measures/domain/Coverage?id=' +
        encodeURIComponent(this.props.component.key);

    return <Domain>
      <div className="overview-card-header">
        <div className="overview-title">
          <a href={domainUrl}>
            {translate('metric.coverage.name')}
          </a>
        </div>
      </div>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>

            <Measure composite={true}>
              <div className="display-inline-block text-middle big-spacer-right">
                <DonutChart width="40"
                            height="40"
                            thickness="4"
                            data={donutData}/>
              </div>
              <div className="display-inline-block text-middle">
                <div className="overview-domain-measure-value">
                  <DrilldownLink component={this.props.component.key} metric={coverageMetric}>
                    <span className="js-overview-main-coverage">
                      {formatMeasure(this.props.measures[coverageMetric], 'PERCENT')}
                    </span>
                  </DrilldownLink>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('coverage')}</div>
              </div>
            </Measure>

            {this.renderTests()}
          </MeasuresList>
          {this.renderTimeline('before')}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
