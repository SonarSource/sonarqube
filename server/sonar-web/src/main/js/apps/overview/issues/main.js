import React from 'react';

import IssuesSeverities from './severities';
import IssuesAssignees from './assignees';
import IssuesTags from './tags';
import { IssuesBubbleChart } from './bubble-chart';
import { IssuesTimeline } from './timeline';
import { IssuesTreemap } from './treemap';

import { getSeverities, getTags, getAssignees } from '../../../api/issues';


export default class OverviewDomain extends React.Component {
  constructor () {
    super();
    this.state = { severities: [], tags: [], assignees: [] };
  }

  componentDidMount () {
    Promise.all([
      this.requestSeverities(),
      this.requestTags(),
      this.requestAssignees()
    ]).then(responses => {
      this.setState({
        severities: responses[0],
        tags: responses[1],
        assignees: responses[2]
      });
    });
  }

  requestSeverities () {
    return getSeverities({ resolved: 'false', componentUuids: this.props.component.id });
  }

  requestTags () {
    return getTags({ resolved: 'false', componentUuids: this.props.component.id });
  }

  requestAssignees () {
    return getAssignees({ statuses: 'OPEN,REOPENED', componentUuids: this.props.component.id });
  }

  render () {
    return <div className="overview-domain">

      <IssuesTimeline {...this.props}/>

      <div className="flex-columns">
        <div className="flex-column flex-column-third">
          <IssuesSeverities {...this.props} severities={this.state.severities}/>
        </div>
        <div className="flex-column flex-column-third">
          <IssuesTags {...this.props} tags={this.state.tags}/>
        </div>
        <div className="flex-column flex-column-third">
          <IssuesAssignees {...this.props} assignees={this.state.assignees}/>
        </div>
      </div>

      <IssuesBubbleChart {...this.props}/>
      <IssuesTreemap {...this.props}/>
    </div>;
  }
}
