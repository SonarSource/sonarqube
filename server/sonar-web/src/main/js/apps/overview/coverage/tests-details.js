import React from 'react';
import { getMeasures } from '../../../api/measures';
import { formatMeasure } from '../formatting';


const METRICS = [
  'tests',
  'skipped_tests',
  'test_errors',
  'test_failures',
  'test_execution_time',
  'test_success_density'
];


function format (value, metric) {
  return value != null ? formatMeasure(value, metric) : '—';
}


export class TestsDetails extends React.Component {
  constructor () {
    super();
    this.state = { measures: {} };
  }

  componentDidMount () {
    this.requestDetails();
  }

  requestDetails () {
    return getMeasures(this.props.component.key, METRICS).then(measures => {
      this.setState({ measures });
    });
  }

  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Tests Details</h2>
      <table className="data zebra">
        <tbody>
        <tr>
          <td>Tests</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['tests'], 'tests')}
          </td>
        </tr>
        <tr>
          <td>Skipped Tests</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['skipped_tests'], 'skipped_tests')}
          </td>
        </tr>
        <tr>
          <td>Test Errors</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['test_errors'], 'test_errors')}
          </td>
        </tr>
        <tr>
          <td>Test Failures</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['test_failures'], 'test_failures')}
          </td>
        </tr>
        <tr>
          <td>Tests Execution Time</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['test_execution_time'], 'test_execution_time')}
          </td>
        </tr>
        <tr>
          <td>Tests Success</td>
          <td className="thin nowrap text-right">
            {format(this.state.measures['test_success_density'], 'test_success_density')}
          </td>
        </tr>
        </tbody>
      </table>
    </div>;
  }
}
