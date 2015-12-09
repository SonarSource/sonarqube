import moment from 'moment';
import React from 'react';

import { Domain,
         DomainHeader,
         DomainPanel,
         DomainNutshell,
         DomainLeak,
         MeasuresList,
         Measure,
         DomainMixin } from './components';
import { Rating } from './../../../components/shared/rating';
import { IssuesLink } from '../../../components/shared/issues-link';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { Legend } from '../components/legend';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';


export const GeneralDebt = React.createClass({
  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  mixins: [TooltipsMixin, DomainMixin],

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }

    let createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    return <DomainLeak>
      <Legend leakPeriodLabel={this.props.leakPeriodLabel} leakPeriodDate={this.props.leakPeriodDate}/>

      <MeasuresList>
        <Measure label={getMetricName('new_debt')}>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', createdAfter: createdAfter, facetMode: 'debt' }}>
            {formatMeasure(this.props.leak.debt, 'SHORT_WORK_DUR')}
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_issues')}>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', createdAfter: createdAfter }}>
            {formatMeasure(this.props.leak.issues, 'SHORT_INT')}
          </IssuesLink>
        </Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  render () {
    return <Domain>
      <DomainHeader component={this.props.component}
                    title={window.t('overview.domain.debt')}
                    linkTo="/debt"/>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>

            <Measure composite={true}>
              <div className="display-inline-block text-middle big-spacer-right">
                <div className="overview-domain-measure-value">
                  <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                    <Rating value={this.props.measures['sqale_rating']}/>
                  </DrilldownLink>
                </div>
              </div>
              <div className="display-inline-block text-middle">
                <div className="overview-domain-measure-value">
                  <IssuesLink component={this.props.component.key}
                              params={{ resolved: 'false', facetMode: 'debt' }}>
                    {formatMeasure(this.props.measures.debt, 'SHORT_WORK_DUR')}
                  </IssuesLink>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('debt')}</div>
              </div>
            </Measure>

            <Measure label={getMetricName('issues')}>
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false' }}>
                {formatMeasure(this.props.measures.issues, 'SHORT_INT')}
              </IssuesLink>
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before', true)}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
