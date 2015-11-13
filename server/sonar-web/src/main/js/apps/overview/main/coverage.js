import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';


export const GeneralCoverage = React.createClass({
  mixins: [TooltipsMixin, DomainMixin],

  propTypes: {
    measures: React.PropTypes.object.isRequired,
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  getCoverageMetric () {
    if (this.props.measures['overall_coverage'] != null) {
      return 'overall_coverage';
    } else if (this.props.measures['coverage'] != null) {
      return 'coverage';
    } else {
      return 'it_coverage';
    }
  },

  getNewCoverageMetric () {
    if (this.props.leak['new_overall_coverage'] != null) {
      return 'new_overall_coverage';
    } else if (this.props.leak['new_coverage'] != null) {
      return 'new_coverage';
    } else {
      return 'new_it_coverage';
    }
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

    return <Domain>
      <DomainHeader title="Coverage" linkTo="/tests"/>

      <DomainPanel domain="coverage">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('coverage')}>
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
