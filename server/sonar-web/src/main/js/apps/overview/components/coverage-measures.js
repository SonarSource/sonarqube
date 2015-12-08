import React from 'react';

import { DetailedMeasure } from './detailed-measure';
import { DonutChart } from '../../../components/charts/donut-chart';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';


export const CoverageMeasures = React.createClass({
  propTypes: {
    measures: React.PropTypes.object.isRequired,
    leak: React.PropTypes.object.isRequired,
    prefix: React.PropTypes.string.isRequired
  },

  getMetricName(metric) {
    const { prefix } = this.props;
    return prefix + metric;
  },

  getNewCoverageMetricName () {
    const { prefix } = this.props;
    return 'new_' + prefix + 'coverage';
  },

  getCoverageMeasure() {
    const coverageMetricName = this.getMetricName('coverage');
    return this.props.measures[coverageMetricName];
  },

  getCoverageLeak() {
    const coverageMetricName = this.getMetricName('coverage');
    return this.props.leak[coverageMetricName];
  },

  getNewCoverageMeasure() {
    const newCoverageMetricName = this.getNewCoverageMetricName();
    return this.props.leak[newCoverageMetricName];
  },

  renderCoverageLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    const coverageLeak = this.getCoverageLeak();
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">
        {formatMeasureVariation(coverageLeak, 'PERCENT')}
      </span>
    </div>;
  },

  renderCoverageOnNewCode() {
    const newCoverageMetricName = this.getNewCoverageMetricName();
    const newCoverage = this.getNewCoverageMeasure();

    if (!this.props.leakPeriodDate || newCoverage == null) {
      return null;
    }

    const donutData = [
      { value: newCoverage, fill: '#85bb43' },
      { value: 100 - newCoverage, fill: '#d4333f' }
    ];
    return <div className="overview-detailed-measure" style={{ lineHeight: '30px' }}>
      <div className="overview-detailed-measure-nutshell overview-leak">
        <span className="overview-detailed-measure-name">
          {window.t('metric', newCoverageMetricName, 'name')}
        </span>
      </div>

      <div className="overview-detailed-measure-leak">
        <span className="overview-detailed-measure-value">
          <span className="spacer-right">
            <DonutChart width="30"
                        height="30"
                        thickness="4"
                        data={donutData}/>
          </span>
          <DrilldownLink component={this.props.component.key}
                         metric={newCoverageMetricName}
                         period={this.props.leakPeriodIndex}>
            {formatMeasure(newCoverage, 'PERCENT')}
          </DrilldownLink>
        </span>
      </div>
    </div>;
  },

  renderDonut() {
    const coverage = this.getCoverageMeasure();
    const donutData = [
      { value: coverage, fill: '#85bb43' },
      { value: 100 - coverage, fill: '#d4333f' }
    ];
    return <DonutChart width="30"
                       height="30"
                       thickness="4"
                       data={donutData}/>;
  },

  render() {
    const coverageMetricName = this.getMetricName('coverage');
    const coverage = this.getCoverageMeasure();

    return (
        <div className="overview-detailed-measures-list">
          <div className="overview-detailed-measure" style={{ lineHeight: '30px' }}>
            <div className="overview-detailed-measure-nutshell">
              <span className="overview-detailed-measure-name big">
                {window.t('metric', coverageMetricName, 'name')}
              </span>
              <span className="overview-detailed-measure-value">
                <span className="spacer-right">{this.renderDonut()}</span>
                <DrilldownLink component={this.props.component.key} metric={coverageMetricName}>
                  {formatMeasure(coverage, 'PERCENT')}
                </DrilldownLink>
              </span>
            </div>

            {this.renderCoverageLeak()}
          </div>

          <DetailedMeasure {...this.props} {...this.props}
              metric={this.getMetricName('line_coverage')}
              type="PERCENT"/>

          <DetailedMeasure {...this.props} {...this.props}
              metric={this.getMetricName('uncovered_lines')}
              type="INT"/>

          <DetailedMeasure {...this.props} {...this.props}
              metric="lines_to_cover"
              type="INT"/>

          <DetailedMeasure {...this.props} {...this.props}
              metric={this.getMetricName('branch_coverage')}
              type="PERCENT"/>

          <DetailedMeasure {...this.props} {...this.props}
              metric={this.getMetricName('uncovered_conditions')}
              type="INT"/>

          <DetailedMeasure {...this.props} {...this.props}
              metric="conditions_to_cover"
              type="INT"/>

          {this.renderCoverageOnNewCode()}
        </div>
    );
  }
});
