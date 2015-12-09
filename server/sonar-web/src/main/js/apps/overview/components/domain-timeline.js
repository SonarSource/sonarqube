import _ from 'underscore';
import moment from 'moment';
import React from 'react';
import Select from 'react-select';

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

  handleMetricChange (selected) {
    let newMetric = selected.value;
    let comparisonMetric = this.state.comparisonMetric;
    if (newMetric === comparisonMetric) {
      comparisonMetric = '';
    }
    this.requestTimeMachineData(newMetric, comparisonMetric).then(snapshots => {
      this.setState({ currentMetric: newMetric, comparisonMetric: comparisonMetric, snapshots });
    });
  },

  handleComparisonMetricChange (selected) {
    let newMetric = selected && selected.value;
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

  renderTimelineMetricSelect () {
    if (this.state.loading) {
      return null;
    }

    const options = this.props.metrics.map(metric => {
      return {
        value: metric.key,
        label: metric.name,
        domain: metric.domain
      };
    });

    // use "disabled" property to emulate optgroups
    const optionsWithDomains = [];
    options.forEach((option, index, options) => {
      const previous = index > 0 ? options[index - 1] : null;
      if (!previous || previous.domain !== option.domain) {
        optionsWithDomains.push({
          value: option.domain,
          label: option.domain,
          disabled: true
        });
      }
      optionsWithDomains.push(option);
    });

    return <span>
      <span className="overview-timeline-sample overview-timeline-sample-0"/>
      <Select value={this.state.currentMetric}
              options={optionsWithDomains}
              clearable={false}
              style={{ width: 170 }}
              onChange={this.handleMetricChange}/>
    </span>;
  },

  renderComparisonMetricSelect () {
    if (this.state.loading) {
      return null;
    }

    const options = _.sortBy(this.props.allMetrics, 'domain')
        .filter(metric => metric.key !== this.state.currentMetric)
        .map(metric => {
          return {
            value: metric.key,
            label: metric.name,
            domain: metric.domain
          };
        });

    // use "disabled" property to emulate optgroups
    const optionsWithDomains = [];
    options.forEach((option, index, options) => {
      const previous = index > 0 ? options[index - 1] : null;
      if (!previous || previous.domain !== option.domain) {
        optionsWithDomains.push({
          value: option.domain,
          label: option.domain,
          disabled: true
        });
      }
      optionsWithDomains.push(option);
    });

    return <span>
      {this.state.comparisonMetric ? <span className="overview-timeline-sample overview-timeline-sample-1"/> : null}
      <Select value={this.state.comparisonMetric}
              options={optionsWithDomains}
              placeholder="Compare with..."
              style={{ width: 170 }}
              onChange={this.handleComparisonMetricChange}/>
    </span>;
  },

  render () {
    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <div>
          <h2 className="overview-title">{window.t('overview.chart.history')}</h2>
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
