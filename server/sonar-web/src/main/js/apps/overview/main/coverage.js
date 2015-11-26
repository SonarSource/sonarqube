import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { DonutChart } from '../../../components/charts/donut-chart';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';


export const GeneralCoverage = React.createClass({
  mixins: [TooltipsMixin, DomainMixin],

  propTypes: {
    measures: React.PropTypes.object.isRequired,
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object,
    coverageMetricPrefix: React.PropTypes.string.isRequired
  },

  getCoverageMetric () {
    return this.props.coverageMetricPrefix + 'coverage';
  },

  getNewCoverageMetric () {
    return 'new_' + this.props.coverageMetricPrefix + 'coverage';
  },

  renderNewCoverage () {
    let newCoverageMetric = this.getNewCoverageMetric();

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
    let tests = this.props.measures['tests'];
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
    let coverageMetric = this.getCoverageMetric();
    if (this.props.measures[coverageMetric] == null) {
      return null;
    }

    let donutData = [
      { value: this.props.measures[coverageMetric], fill: '#85bb43' },
      { value: 100 - this.props.measures[coverageMetric], fill: '#d4333f' }
    ];

    return <Domain>
      <DomainHeader component={this.props.component} title={window.t('overview.domain.coverage')} linkTo="/tests"/>

      <DomainPanel domain="coverage">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('coverage')}>
              <span className="big-spacer-right">
                <DonutChart width="40" height="40" thickness="3" data={donutData}/>
              </span>
              <DrilldownLink component={this.props.component.key} metric={coverageMetric}>
                <span className="js-overview-main-coverage">
                  {formatMeasure(this.props.measures[coverageMetric], 'PERCENT')}
                </span>
              </DrilldownLink>
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
