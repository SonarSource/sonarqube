import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { LineChart } from '../../../components/charts/line-chart';
import { getTimeMachineData } from '../../../api/time-machine';
import { getEvents } from '../../../api/events';


const HEIGHT = 280;


function parseValue (value, type) {
  return type === 'RATING' && typeof value === 'string' ?
         value.charCodeAt(0) - 'A'.charCodeAt(0) + 1 :
         value;
}


export class DomainTimeline extends React.Component {
  constructor (props) {
    super(props);
    this.state = { loading: true, currentMetric: props.initialMetric };
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
    let metric = this.refs.metricSelect.value;
    this.setState({ currentMetric: metric }, this.requestTimeMachineData);
  }

  groupMetricsByDomain () {
    return _.sortBy(
        _.map(
            _.groupBy(this.props.metrics, 'domain'),
            (metricList, domain) => {
              return {
                domain: domain,
                metrics: _.sortBy(metricList, 'name')
              };
            }),
        'domain'
    );
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderWhenNoHistoricalData () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      There is no historical data.
    </div>;
  }

  renderLineChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let events = this.prepareEvents();

    if (!events.length) {
      return this.renderWhenNoHistoricalData();
    }

    let currentMetricType = _.findWhere(this.props.metrics, { key: this.state.currentMetric }).type;

    let data = events.map((event, index) => {
      return { x: index, y: parseValue(event.value, currentMetricType) };
    });

    let xTicks = events.map(event => event.version.substr(0, 6));

    let xValues = events.map(event => {
      return currentMetricType === 'RATING' ? event.value : window.formatMeasure(event.value, currentMetricType);
    });

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

  renderMetricOption (metric) {
    return <option key={metric.key} value={metric.key}>{metric.name}</option>;
  }

  renderTimelineMetricSelect () {
    if (this.state.loading) {
      return null;
    }
    let groupedMetrics = this.groupMetricsByDomain();
    let inner;
    if (groupedMetrics.length > 1) {
      inner = groupedMetrics.map(metricGroup => {
        let options = metricGroup.metrics.map(this.renderMetricOption);
        return <optgroup key={metricGroup.domain} label={metricGroup.domain}>{options}</optgroup>;
      });
    } else {
      inner = groupedMetrics[0].metrics.map(this.renderMetricOption);
    }
    return <select ref="metricSelect"
                   className="overview-timeline-select"
                   onChange={this.handleMetricChange.bind(this)}
                   value={this.state.currentMetric}>{inner}</select>;
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

DomainTimeline.propTypes = {
  metrics: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  initialMetric: React.PropTypes.string.isRequired
};
