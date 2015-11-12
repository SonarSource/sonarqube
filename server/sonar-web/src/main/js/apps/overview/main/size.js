import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';
import { LanguageDistribution } from '../components/language-distribution';


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
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  renderLanguageDistribution() {
    return <div style={{ width: 200 }}>
      <LanguageDistribution lines={this.props.measures['ncloc']}
                            distribution={this.props.measures['ncloc_language_distribution']}/>
    </div>;
  },

  render () {
    return <Domain>
      <DomainHeader title="Size" linkTo="/size"/>

      <DomainPanel domain="size">
        <DomainNutshell>
          <MeasuresList>
            <Measure label={getMetricName('ncloc')}>
              <DrilldownLink component={this.props.component.key} metric="ncloc">
                {formatMeasure(this.props.measures['ncloc'], 'SHORT_INT')}
              </DrilldownLink>
            </Measure>
            <Measure composite={true}>
              {this.renderLanguageDistribution()}
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before')}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
