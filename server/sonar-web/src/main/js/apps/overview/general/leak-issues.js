import moment from 'moment';
import React from 'react';
import Card from './card';
import Measure from './../helpers/measure';
import MeasureVariation from './../helpers/measure-variation';
import IssuesLink from './../helpers/issues-link';
import SeverityIcon from '../../../components/shared/severity-icon';
import StatusIcon from '../../../components/shared/status-icon';
import {getPeriodDate} from './../helpers/period-label';

export default React.createClass({
  render() {
    let
        newDebt = this.props.leak.newDebt,
        issues = this.props.leak.newIssues,
        blockerIssues = this.props.leak.newBlockerIssues,
        criticalIssues = this.props.leak.newCriticalIssues,
        issuesToReview = this.props.leak.newOpenIssues + this.props.leak.newReopenedIssues,
        periodDate = moment(getPeriodDate(this.props.component.periods, '1')).format('YYYY-MM-DDTHH:mm:ssZZ');

    return (
        <Card>
          <div className="measures">
            <div className="measure measure-big" data-metric="sqale_index">
              <span className="measure-name">{window.t('overview.metric.new_debt')}</span>
              <span className="measure-value">
                <IssuesLink component={this.props.component.key}
                            params={{ resolved: 'false', createdAfter: periodDate, facetMode: 'debt' }}>
                  <Measure value={newDebt} type="SHORT_WORK_DUR"/>
                </IssuesLink>
              </span>
            </div>
            <div className="measure measure-big" data-metric="violations">
              <span className="measure-name">{window.t('overview.metric.new_issues')}</span>
              <span className="measure-value">
                <IssuesLink component={this.props.component.key}
                            params={{ resolved: 'false', createdAfter: periodDate }}>
                  <Measure value={issues} type="SHORT_INT"/>
                </IssuesLink>
              </span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top">
            <li>
              <span><SeverityIcon severity="BLOCKER"/></span>&nbsp;
              <IssuesLink component={this.props.component.key}
                          params={{ resolved: 'false', createdAfter: periodDate, severities: 'BLOCKER' }}>
                <MeasureVariation value={blockerIssues} type="SHORT_INT"/>
              </IssuesLink>
            </li>
            <li>
              <span><SeverityIcon severity="CRITICAL"/></span>&nbsp;
              <IssuesLink component={this.props.component.key}
                          params={{ resolved: 'false', createdAfter: periodDate, severities: 'CRITICAL' }}>
                <MeasureVariation value={criticalIssues} type="SHORT_INT"/>
              </IssuesLink>
            </li>
            <li>
              <span><StatusIcon status="OPEN"/></span>&nbsp;
              <IssuesLink component={this.props.component.key}
                          params={{ resolved: 'false', createdAfter: periodDate, statuses: 'OPEN,REOPENED' }}>
                <MeasureVariation value={issuesToReview} type="SHORT_INT"/>
              </IssuesLink>
            </li>
          </ul>
        </Card>
    );
  }
});
