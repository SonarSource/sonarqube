import moment from 'moment';
import React from 'react';

import { Domain, DomainHeader, DomainPanel, DomainNutshell, DomainLeak, MeasuresList, Measure, DomainMixin } from './components';
import Rating from './../helpers/rating';
import IssuesLink from '../helpers/issues-link';
import DrilldownLink from '../helpers/drilldown-link';
import SeverityHelper from '../../../components/shared/severity-helper';
import SeverityIcon from '../../../components/shared/severity-icon';
import StatusIcon from '../../../components/shared/status-icon';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { SEVERITIES } from '../../../helpers/constants';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';


export const GeneralIssues = React.createClass({
  mixins: [TooltipsMixin, DomainMixin],

  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  renderSeverities() {
    let severities = SEVERITIES.map((s, index) => {
      let measure = this.props.measures.issuesSeverities[index];
      return <tr key={s}>
        <td>
          <SeverityHelper severity={s}/>
        </td>
        <td className="thin nowrap text-right">
          <IssuesLink component={this.props.component.key} params={{ resolved: 'false', severities: s }}>
            {formatMeasure(measure, 'SHORT_INT')}
          </IssuesLink>
        </td>
      </tr>;
    });

    return <div style={{ width: 120 }}>
      <table className="data">
        <tbody>{severities}</tbody>
      </table>
    </div>;
  },

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }

    let createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('new_issues')}>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', createdAfter: createdAfter }}>
            {formatMeasureVariation(this.props.leak.issues, 'SHORT_INT')}
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_debt')}>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', createdAfter: createdAfter, facetMode: 'debt' }}>
            {formatMeasureVariation(this.props.leak.debt, 'SHORT_WORK_DUR')}
          </IssuesLink>
        </Measure>
      </MeasuresList>
      <MeasuresList>
        <Measure label={getMetricName('new_blocker_issues')}>
          <span className="spacer-right"><SeverityIcon severity="BLOCKER"/></span>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', severities: 'BLOCKER', createdAfter: createdAfter }}>
            {formatMeasureVariation(this.props.leak.issuesSeverities[0], 'SHORT_INT')}
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_critical_issues')}>
          <span className="spacer-right"><SeverityIcon severity="CRITICAL"/></span>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', severities: 'CRITICAL', createdAfter: createdAfter }}>
            {formatMeasureVariation(this.props.leak.issuesSeverities[1], 'SHORT_INT')}
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_open_issues')}>
          <span className="spacer-right"><StatusIcon status="OPEN"/></span>
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', statuses: 'OPEN,REOPENED', createdAfter: createdAfter }}>
            {formatMeasureVariation(this.props.leak.issuesStatuses[0] + this.props.leak.issuesStatuses[1], 'SHORT_INT')}
          </IssuesLink>
        </Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  render () {
    return <Domain>
      <DomainHeader title="Technical Debt"
                    leakPeriodLabel={this.props.leakPeriodLabel} leakPeriodDate={this.props.leakPeriodDate}/>

      <DomainPanel domain="issues">
        <DomainNutshell>
          <MeasuresList>
            <Measure>
              <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                <Rating value={this.props.measures['sqale_rating']}/>
              </DrilldownLink>
            </Measure>
            <Measure label={getMetricName('issues')}>
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false' }}>
                {formatMeasure(this.props.measures.issues, 'SHORT_INT')}
              </IssuesLink>
            </Measure>
            <Measure label={getMetricName('debt')}>
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false', facetMode: 'debt' }}>
                {formatMeasure(this.props.measures.debt, 'SHORT_WORK_DUR')}
              </IssuesLink>
            </Measure>
            <Measure composite={true}>
              {this.renderSeverities()}
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before')}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
