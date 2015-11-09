import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import DrilldownLink from '../helpers/drilldown-link';
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

  renderNewCoverage () {
    if (this.props.leak['new_overall_coverage'] != null) {
      return <DrilldownLink component={this.props.component.key} metric="new_overall_coverage" period="1">
        {formatMeasure(this.props.leak['new_overall_coverage'], 'PERCENT')}
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

  render () {
    if (this.props.measures['overall_coverage'] == null) {
      return null;
    }

    return <Domain>
      <DomainHeader title="Tests"/>

      <DomainPanel domain="coverage">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('coverage')}>
              <DrilldownLink component={this.props.component.key} metric="overall_coverage">
                {formatMeasure(this.props.measures['overall_coverage'], 'PERCENT')}
              </DrilldownLink>
            </Measure>
            <Measure label={getMetricName('tests')}>
              <DrilldownLink component={this.props.component.key} metric="tests">
                {formatMeasure(this.props.measures['tests'], 'SHORT_INT')}
              </DrilldownLink>
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before')}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
