import React from 'react';

import { DomainMeasuresList } from '../domain/measures-list';


const METRICS = [
  'comment_lines',
  'comment_lines_density'
];


export class CommentsDetails extends React.Component {
  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Comments</h2>
      <DomainMeasuresList {...this.props} metricsToDisplay={METRICS}/>
    </div>;
  }
}
