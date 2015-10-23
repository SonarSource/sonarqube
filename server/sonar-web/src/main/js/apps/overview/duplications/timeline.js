import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { LineChart } from '../../../components/charts/line-chart';
import { formatMeasure } from '../formatting';
import { getTimeMachineData } from '../../../api/time-machine';
import { getEvents } from '../../../api/events';


const DUPLICATIONS_METRICS = [
  'duplicated_blocks',
  'duplicated_files',
  'duplicated_lines',
  'duplicated_lines_density'
];

const HEIGHT = 280;


export class DuplicationsTimeline extends React.Component {
  constructor () {
    super();
    this.state = { loading: true, currentMetric: DUPLICATIONS_METRICS[0] };
  }

  componentDidMount () {
    Promise.all([
      this.requestTimeMachineData(),
      this.requestEvents()
    ]).then(() => this.setState({ loading: false }));
  }

  requestTimeMachineData () {
    return getTimeMachineData(this.props.component.key, this.state.currentMetric).then(r => {
      let snapshots = r[0].cells.map(cell => {
        return { date: moment(cell.d).toDate(), value: cell.v[0] };
      });
      this.setState({ snapshots });
    });
  }

  requestEvents () {
    return getEvents(this.props.component.key, 'Version').then(r => {
      let events = r.map(event => {
        return { version: event.n, date: moment(event.dt).toDate() };
      });
      events = _.sortBy(events, 'date');
      this.setState({ events });
    });
  }

  prepareEvents () {
    let events = this.state.events;
    let snapshots = this.state.snapshots;
    return events
        .map(event => {
          let snapshot = snapshots.find(s => s.date.getTime() === event.date.getTime());
          event.value = snapshot && snapshot.value;
          return event;
        })
        .filter(event => event.value != null);
  }

  handleMetricChange () {
    let metric = React.findDOMNode(this.refs.metricSelect).value;
    this.setState({ currentMetric: metric }, this.requestTimeMachineData);
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderLineChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let events = this.prepareEvents();

    let data = events.map((event, index) => {
      return { x: index, y: event.value };
    });

    let xTicks = events.map(event => event.version.substr(0, 6));

    let xValues = events.map(event => formatMeasure(event.value, this.state.currentMetric));

    // TODO use leak period
    let backdropConstraints = [
      this.state.events.length - 2,
      this.state.events.length - 1
    ];

    return <LineChart data={data}
                      xTicks={xTicks}
                      xValues={xValues}
                      backdropConstraints={backdropConstraints}
                      height={HEIGHT}
                      interpolate="linear"
                      padding={[25, 30, 50, 30]}/>;
  }

  renderTimelineMetricSelect () {
    if (this.state.loading) {
      return null;
    }

    let options = DUPLICATIONS_METRICS
        .map(metric => <option key={metric} value={metric}>{window.t('metric', metric, 'name')}</option>);

    return <select ref="metricSelect"
                   className="overview-timeline-select"
                   onChange={this.handleMetricChange.bind(this)}
                   value={this.state.currentMetric}>{options}</select>;
  }

  render () {
    return <div className="overview-timeline overview-domain-dark">
      <div className="overview-domain-header">
        <h2 className="overview-title">Project History</h2>
        {this.renderTimelineMetricSelect()}
      </div>
      <div>
        {this.renderLineChart()}
      </div>
    </div>;
  }
}
