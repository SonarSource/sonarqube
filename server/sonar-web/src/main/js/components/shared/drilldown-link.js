import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { IssuesLink } from './issues-link';
import { getComponentDrilldownUrl } from '../../helpers/urls';


const ISSUE_MEASURES = [
  'violations',
  'new_violations',
  'blocker_violations',
  'critical_violations',
  'major_violations',
  'minor_violations',
  'info_violations',
  'new_blocker_violations',
  'new_critical_violations',
  'new_major_violations',
  'new_minor_violations',
  'new_info_violations',
  'open_issues',
  'reopened_issues',
  'confirmed_issues',
  'false_positive_issues'
];


const HIGHLIGHT_MAPPING = {
  'coverage': 'uncovered_lines',
  'line_coverage': 'uncovered_lines',
  'branch_coverage': 'uncovered_conditions',

  'new_coverage': 'new_uncovered_lines',
  'new_line_coverage': 'new_uncovered_lines',
  'new_branch_coverage': 'new_uncovered_conditions',

  'it_coverage': 'it_uncovered_lines',
  'it_line_coverage': 'it_uncovered_lines',
  'it_branch_coverage': 'it_uncovered_conditions',

  'new_it_coverage': 'new_it_uncovered_lines',
  'new_it_line_coverage': 'new_it_uncovered_lines',
  'new_it_branch_coverage': 'new_it_uncovered_conditions',

  'overall_coverage': 'overall_uncovered_lines',
  'overall_line_coverage': 'overall_uncovered_lines',
  'overall_branch_coverage': 'overall_uncovered_conditions',

  'new_overall_coverage': 'new_overall_uncovered_lines',
  'new_overall_line_coverage': 'new_overall_uncovered_lines',
  'new_overall_branch_coverage': 'new_overall_uncovered_conditions',

  'new_sqale_debt_ratio': 'new_technical_debt'
};


export const DrilldownLink = React.createClass({
  isIssueMeasure() {
    return ISSUE_MEASURES.indexOf(this.props.metric) !== -1;
  },

  propsToIssueParams() {
    let params = {};
    if (this.props.periodDate) {
      params.createdAfter = moment(this.props.periodDate).format('YYYY-MM-DDTHH:mm:ssZZ');
    }
    switch (this.props.metric) {
      case 'blocker_violations':
      case 'new_blocker_violations':
        _.extend(params, { resolved: 'false', severities: 'BLOCKER' });
        break;
      case 'critical_violations':
      case 'new_critical_violations':
        _.extend(params, { resolved: 'false', severities: 'CRITICAL' });
        break;
      case 'major_violations':
      case 'new_major_violations':
        _.extend(params, { resolved: 'false', severities: 'MAJOR' });
        break;
      case 'minor_violations':
      case 'new_minor_violations':
        _.extend(params, { resolved: 'false', severities: 'MINOR' });
        break;
      case 'info_violations':
      case 'new_info_violations':
        _.extend(params, { resolved: 'false', severities: 'INFO' });
        break;
      case 'open_issues':
        _.extend(params, { resolved: 'false', statuses: 'OPEN' });
        break;
      case 'reopened_issues':
        _.extend(params, { resolved: 'false', statuses: 'REOPENED' });
        break;
      case 'confirmed_issues':
        _.extend(params, { resolved: 'false', statuses: 'CONFIRMED' });
        break;
      case 'false_positive_issues':
        _.extend(params, { resolutions: 'FALSE-POSITIVE' });
        break;
      default:
        _.extend(params, { resolved: 'false' });
    }
    return params;
  },

  renderIssuesLink() {
    return <IssuesLink component={this.props.component} params={this.propsToIssueParams()}>
      {this.props.children}
    </IssuesLink>;
  },

  render() {
    if (this.isIssueMeasure()) {
      return this.renderIssuesLink();
    }

    let highlightedMetric = HIGHLIGHT_MAPPING[this.props.metric];
    let url = highlightedMetric ?
        getComponentDrilldownUrl(this.props.component, highlightedMetric, this.props.period, this.props.metric) :
        getComponentDrilldownUrl(this.props.component, this.props.metric, this.props.period);
    return <a className={this.props.className} href={url}>{this.props.children}</a>;
  }
});
