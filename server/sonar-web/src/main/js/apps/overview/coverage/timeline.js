import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { LineChart } from '../../../components/charts/line-chart';
import { formatMeasure } from '../formatting';
import { getTimeMachineData } from '../../../api/time-machine';
import { getEvents } from '../../../api/events';


const COVERAGE_METRICS = [
  'coverage',
  'line_coverage',
  'branch_coverage',
  'lines_to_cover',
  'conditions_to_cover',
  'uncovered_lines',
  'uncovered_conditions',

  'it_coverage',
  'it_line_coverage',
  'it_branch_coverage',
  'it_lines_to_cover',
  'it_conditions_to_cover',
  'it_uncovered_lines',
  'it_uncovered_conditions',

  'overall_coverage',
  'overall_line_coverage',
  'overall_branch_coverage',
  'overall_lines_to_cover',
  'overall_conditions_to_cover',
  'overall_uncovered_lines',
  'overall_uncovered_conditions'
];

const TESTS_METRICS = [
  'tests',
  'skipped_tests',
  'test_errors',
  'test_failures',
  'test_execution_time',
  'test_success_density'
];

const HEIGHT = 280;


export class CoverageTimeline extends React.Component {
  constructor () {
    super();
    this.state = { loading: true, currentMetric: COVERAGE_METRICS[0] };
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

    let issueOptions = COVERAGE_METRICS
        .map(metric => <option key={metric} value={metric}>{window.t('metric', metric, 'name')}</option>);
    let debtOptions = TESTS_METRICS
        .map(metric => <option key={metric} value={metric}>{window.t('metric', metric, 'name')}</option>);

    return <select ref="metricSelect"
                   className="overview-timeline-select"
                   onChange={this.handleMetricChange.bind(this)}
                   value={this.state.currentMetric}>
      <optgroup label="Coverage">{issueOptions}</optgroup>
      <optgroup label="Tests">{debtOptions}</optgroup>
    </select>;
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
