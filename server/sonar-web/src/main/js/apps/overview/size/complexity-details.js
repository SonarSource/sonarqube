import React from 'react';

import { DomainMeasuresList } from '../domain/measures-list';


const METRICS = [
  'complexity',
  'class_complexity',
  'file_complexity',
  'function_complexity'
];


export class ComplexityDetails extends React.Component {
  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Complexity</h2>
      <DomainMeasuresList {...this.props} metricsToDisplay={METRICS}/>
    </div>;
  }
}
