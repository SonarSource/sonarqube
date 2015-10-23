import React from 'react';

import { DomainMeasuresList } from '../domain/measures-list';


const METRICS = [
  'ncloc',
  'lines',
  'files',
  'directories',
  'functions',
  'classes',
  'accessors'
];


export class SizeDetails extends React.Component {
  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Size</h2>
      <DomainMeasuresList {...this.props} metricsToDisplay={METRICS}/>
    </div>;
  }
}
