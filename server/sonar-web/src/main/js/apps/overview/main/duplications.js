import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';


export const GeneralDuplications = React.createClass({
  mixins: [TooltipsMixin, DomainMixin],

  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }
    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('duplications')}>
          {formatMeasureVariation(this.props.leak['duplicated_lines_density'], 'PERCENT')}
        </Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  render () {
    return <Domain>
      <DomainHeader title="Duplications" linkTo="/duplications"/>

      <DomainPanel domain="duplications">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('duplications')}>
              <DrilldownLink component={this.props.component.key} metric="duplicated_lines_density">
                {formatMeasure(this.props.measures['duplicated_lines_density'], 'PERCENT')}
              </DrilldownLink>
            </Measure>
            <Measure label={getMetricName('duplicated_blocks')}>
              <DrilldownLink component={this.props.component.key} metric="duplicated_blocks">
                {formatMeasure(this.props.measures['duplicated_blocks'], 'SHORT_INT')}
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
