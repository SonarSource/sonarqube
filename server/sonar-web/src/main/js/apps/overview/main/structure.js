import React from 'react';

import { Domain,
         DomainHeader,
         DomainPanel,
         DomainNutshell,
         DomainLeak,
         MeasuresList,
         Measure,
         DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';
import { LanguageDistribution } from '../components/language-distribution';


export const GeneralStructure = React.createClass({
  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  mixins: [TooltipsMixin, DomainMixin],

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }
    let measure = this.props.leak['ncloc'];
    let formatted = measure != null ? formatMeasureVariation(measure, 'SHORT_INT') : '—';
    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('ncloc')}>{formatted}</Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  renderLanguageDistribution() {
    if (!this.props.measures['ncloc'] || !this.props.measures['ncloc_language_distribution']) {
      return null;
    }
    return <Measure composite={true}>
      <div style={{ width: 200 }}>
        <LanguageDistribution lines={this.props.measures['ncloc']}
                              distribution={this.props.measures['ncloc_language_distribution']}/>
      </div>
    </Measure>;
  },

  render () {
    return <Domain>
      <DomainHeader component={this.props.component}
                    title={window.t('overview.domain.structure')}
                    linkTo="/structure"/>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>
            {this.renderLanguageDistribution()}
            <Measure label={getMetricName('ncloc')}>
              <DrilldownLink component={this.props.component.key} metric="ncloc">
                {formatMeasure(this.props.measures['ncloc'], 'SHORT_INT')}
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
