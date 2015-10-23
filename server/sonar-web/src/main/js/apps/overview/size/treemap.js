import React from 'react';

import { DomainTreemap } from '../domain/treemap';


export class SizeTreemap extends React.Component {
  render () {
    return <DomainTreemap {...this.props} sizeMetric="ncloc"/>;
  }
}
