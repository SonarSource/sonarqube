import React from 'react';

import { DomainMeasuresList } from '../domain/measures-list';


const METRICS = [
  'duplicated_blocks',
  'duplicated_files',
  'duplicated_lines',
  'duplicated_lines_density'
];


export class DuplicationsDetails extends React.Component {
  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Duplications</h2>
      <DomainMeasuresList {...this.props} metricsToDisplay={METRICS}/>
    </div>;
  }
}
