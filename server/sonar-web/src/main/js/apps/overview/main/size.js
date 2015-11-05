import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import DrilldownLink from '../helpers/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';


export const GeneralSize = React.createClass({
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
        <Measure label={getMetricName('ncloc')}>
          {formatMeasureVariation(this.props.leak['ncloc'], 'SHORT_INT')}
        </Measure>
        <Measure label={getMetricName('files')}>
          {formatMeasureVariation(this.props.leak['files'], 'SHORT_INT')}
        </Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  render () {
    return <Domain>
      <DomainHeader {...this.props} title="Size" linkTo="/size"/>

      <DomainPanel domain="size">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('ncloc')}>
              <DrilldownLink component={this.props.component.key} metric="ncloc">
                {formatMeasure(this.props.measures['ncloc'], 'SHORT_INT')}
              </DrilldownLink>
            </Measure>
            <Measure label={getMetricName('files')}>
              <DrilldownLink component={this.props.component.key} metric="files">
                {formatMeasure(this.props.measures['files'], 'SHORT_INT')}
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
