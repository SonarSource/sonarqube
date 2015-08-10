import React from 'react';
import Card from './card';
import Measure from './helpers/measure';
import Rating from './helpers/rating';
import IssuesLink from './helpers/issues-link';
import DrilldownLink from './helpers/drilldown-link';
import SeverityIcon from 'components/shared/severity-icon';
import SeverityHelper from 'components/shared/severity-helper';
import StatusIcon from 'components/shared/status-icon';

export default React.createClass({
  render() {
    const
        debt = this.props.measures.debt,
        rating = this.props.measures.sqaleRating,
        issues = this.props.measures.issues,
        blockerIssues = this.props.measures.blockerIssues,
        criticalIssues = this.props.measures.criticalIssues,
        issuesToReview = this.props.measures.openIssues + this.props.measures.reopenedIssues;

    return (
        <Card>
          <div className="measures">
            <div className="measure measure-big" data-metric="sqale_rating">
              <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                <Rating value={rating}/>
              </DrilldownLink>
            </div>
            <div className="measure measure-big" data-metric="sqale_index">
              <span className="measure-value">
                <IssuesLink component={this.props.component.key} params={{ resolved: 'false', facetMode: 'debt' }}>
                  <Measure value={debt} type="SHORT_WORK_DUR"/>
                </IssuesLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.debt')}</span>
            </div>
            <div className="measure measure-big" data-metric="violations">
              <span className="measure-value">
                <IssuesLink component={this.props.component.key} params={{ resolved: 'false' }}>
                  <Measure value={issues} type="SHORT_INT"/>
                </IssuesLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.issues')}</span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top">
            <li>
              <span><SeverityIcon severity="BLOCKER"/></span>&nbsp;
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false', severities: 'BLOCKER' }}>
                <Measure value={blockerIssues} type="SHORT_INT"/>
              </IssuesLink>
            </li>
            <li>
              <span><SeverityIcon severity="CRITICAL"/></span>&nbsp;
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false', severities: 'CRITICAL' }}>
                <Measure value={criticalIssues} type="SHORT_INT"/>
              </IssuesLink>
            </li>
            <li>
              <span><StatusIcon status="OPEN"/></span>&nbsp;
              <IssuesLink component={this.props.component.key} params={{ resolved: 'false', statuses: 'OPEN,REOPENED' }}>
                <Measure value={issuesToReview} type="SHORT_INT"/>
              </IssuesLink>
            </li>
          </ul>
        </Card>
    );
  }
});
