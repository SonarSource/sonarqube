import d3 from 'd3';
import React from 'react';

import { LineChart } from '../../../components/charts/line-chart';


const HEIGHT = 80;


export class Timeline extends React.Component {
  filterSnapshots () {
    return this.props.history.filter(s => {
      let matchBefore = !this.props.before || s.date <= this.props.before;
      let matchAfter = !this.props.after || s.date >= this.props.after;
      return matchBefore && matchAfter;
    });
  }

  render () {
    let snapshots = this.filterSnapshots();

    if (snapshots.length < 2) {
      return null;
    }

    let data = snapshots.map((snapshot, index) => {
      return { x: index, y: snapshot.value };
    });

    let domain = [0, d3.max(this.props.history, d => d.value)];

    return <LineChart data={data}
                      domain={domain}
                      interpolate="basis"
                      displayBackdrop={true}
                      displayPoints={false}
                      displayVerticalGrid={false}
                      height={HEIGHT}
                      padding={[0, 0, 0, 0]}/>;
  }
}

Timeline.propTypes = {
  history: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  before: React.PropTypes.object,
  after: React.PropTypes.object
};
