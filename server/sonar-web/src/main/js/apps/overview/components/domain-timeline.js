import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { getTimeMachineData } from '../../../api/time-machine';
import { getEvents } from '../../../api/events';
import { formatMeasure, groupByDomain } from '../../../helpers/measures';
import { getShortType } from '../helpers/metrics';
import { Timeline } from './timeline-chart';


const HEIGHT = 280;


function parseValue (value, type) {
  return type === 'RATING' && typeof value === 'string' ? value.charCodeAt(0) - 'A'.charCodeAt(0) + 1 : value;
}


export const DomainTimeline = React.createClass({
  propTypes: {
    allMetrics: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    metrics: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    initialMetric: React.PropTypes.string.isRequired
  },

  getInitialState() {
    return {
      loading: true,
      currentMetric: this.props.initialMetric,
      comparisonMetric: ''
    };
  },

  componentDidMount () {
    Promise.all([
      this.requestTimeMachineData(this.state.currentMetric, this.state.comparisonMetric),
      this.requestEvents()
    ]).then(responses => {
      this.setState({
        loading: false,
        snapshots: responses[0],
        events: responses[1]
      });
    });
  },

  requestTimeMachineData (currentMetric, comparisonMetric) {
    let metricsToRequest = [currentMetric];
    if (comparisonMetric) {
      metricsToRequest.push(comparisonMetric);
    }
    return getTimeMachineData(this.props.component.key, metricsToRequest.join()).then(r => {
      return r[0].cells.map(cell => {
        return { date: moment(cell.d).toDate(), values: cell.v };
      });
    });
  },

  requestEvents () {
    return getEvents(this.props.component.key, 'Version').then(r => {
      let events = r.map(event => {
        return { version: event.n, date: moment(event.dt).toDate() };
      });
      return _.sortBy(events, 'date');
    });
  },

  handleMetricChange (e) {
    let newMetric = e.target.value,
        comparisonMetric = this.state.comparisonMetric;
    if (newMetric === comparisonMetric) {
      comparisonMetric = '';
    }
    this.requestTimeMachineData(newMetric, comparisonMetric).then(snapshots => {
      this.setState({ currentMetric: newMetric, comparisonMetric: comparisonMetric, snapshots });
    });
  },

  handleComparisonMetricChange (e) {
    let newMetric = e.target.value;
    this.requestTimeMachineData(this.state.currentMetric, newMetric).then(snapshots => {
      this.setState({ comparisonMetric: newMetric, snapshots });
    });
  },

  groupMetricsByDomain () {
    return groupByDomain(this.props.metrics);
  },

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  },

  renderWhenNoHistoricalData () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      There is no historical data.
    </div>;
  },

  renderLineCharts () {
    if (this.state.loading) {
      return this.renderLoading();
    }
    return <div>
      {this.renderLineChart(this.state.snapshots, this.state.currentMetric, 0)}
      {this.renderLineChart(this.state.snapshots, this.state.comparisonMetric, 1)}
    </div>;
  },

  renderLineChart (snapshots, metric, index) {
    if (!metric) {
      return null;
    }

    if (snapshots.length < 2) {
      return this.renderWhenNoHistoricalData();
    }

    let metricType = _.findWhere(this.props.allMetrics, { key: metric }).type;
    let data = snapshots.map(snapshot => {
      return {
        x: snapshot.date,
        y: parseValue(snapshot.values[index], metricType)
      };
    });

    let formatValue = (value) => formatMeasure(value, metricType);
    let formatYTick = (tick) => formatMeasure(tick, getShortType(metricType));

    return <div className={'overview-timeline-' + index}>
      <Timeline key={metric}
                data={data}
                metricType={metricType}
                events={this.state.events}
                height={HEIGHT}
                interpolate="linear"
                formatValue={formatValue}
                formatYTick={formatYTick}
                leakPeriodDate={this.props.leakPeriodDate}
                padding={[25, 25, 25, 60]}/>
    </div>;
  },

  renderMetricOption (metric) {
    return <option key={metric.key} value={metric.key}>{metric.name}</option>;
  },

  renderMetricOptions (metrics) {
    let groupedMetrics = groupByDomain(metrics);
    return groupedMetrics.map(metricGroup => {
      let options = metricGroup.metrics.map(this.renderMetricOption);
      return <optgroup key={metricGroup.domain} label={metricGroup.domain}>{options}</optgroup>;
    });
  },

  renderTimelineMetricSelect () {
    if (this.state.loading) {
      return null;
    }
    return <span>
      <span className="overview-timeline-sample overview-timeline-sample-0"/>
      <select ref="metricSelect"
              className="overview-timeline-select"
              onChange={this.handleMetricChange}
              value={this.state.currentMetric}>{this.renderMetricOptions(this.props.metrics)}</select>
    </span>;
  },

  renderComparisonMetricSelect () {
    if (this.state.loading) {
      return null;
    }
    let metrics = this.props.allMetrics.filter(metric => metric.key !== this.state.currentMetric);
    return <span>
      {this.state.comparisonMetric ? <span className="overview-timeline-sample overview-timeline-sample-1"/> : null}
      <select ref="comparisonMetricSelect"
              className="overview-timeline-select"
              onChange={this.handleComparisonMetricChange}
              value={this.state.comparisonMetric}>
        <option value="">Compare with...</option>
        {this.renderMetricOptions(metrics)}
      </select>
    </span>;
  },

  render () {
    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <div>
          <h2 className="overview-title">Timeline</h2>
          {this.renderTimelineMetricSelect()}
        </div>
        {this.renderComparisonMetricSelect()}
      </div>
      <div className="overview-timeline">
        {this.renderLineCharts()}
      </div>
    </div>;
  }
});
