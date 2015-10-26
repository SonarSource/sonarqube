import React from 'react';

import { DomainMeasuresList } from '../domain/measures-list';


const METRICS = [
  'tests',
  'skipped_tests',
  'test_errors',
  'test_failures',
  'test_execution_time',
  'test_success_density'
];


export class TestsDetails extends React.Component {
  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Tests</h2>
      <DomainMeasuresList {...this.props} metricsToDisplay={METRICS}/>
    </div>;
  }
}
