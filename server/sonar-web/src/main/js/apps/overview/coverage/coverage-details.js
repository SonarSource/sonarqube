import React from 'react';

import { getMeasures } from '../../../api/measures';
import DrilldownLink from '../helpers/drilldown-link';


const METRICS = [
  'coverage',
  'line_coverage',
  'branch_coverage',
  'it_coverage',
  'it_line_coverage',
  'it_branch_coverage',
  'overall_coverage',
  'overall_line_coverage',
  'overall_branch_coverage'
];


function formatCoverage (value) {
  return value != null ? window.formatMeasure(value, 'PERCENT') : '—';
}


export class CoverageDetails extends React.Component {
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

  renderValue (value, metricKey) {
    if (value != null) {
      return <DrilldownLink component={this.props.component.key} metric={metricKey}>
        {window.formatMeasure(value, 'PERCENT')}
      </DrilldownLink>;
    } else {
      return '—';
    }
  }

  renderCoverage (coverage, lineCoverage, branchCoverage, prefix) {
    return <table className="data zebra">
      <tbody>
      <tr>
        <td>Coverage</td>
        <td className="thin nowrap text-right">
          {this.renderValue(coverage, prefix + 'coverage')}
        </td>
      </tr>
      <tr>
        <td>Line Coverage</td>
        <td className="thin nowrap text-right">
          {this.renderValue(lineCoverage, prefix + 'line_coverage')}
        </td>
      </tr>
      <tr>
        <td>Branch Coverage</td>
        <td className="thin nowrap text-right">
          {this.renderValue(branchCoverage, prefix + 'branch_coverage')}
        </td>
      </tr>
      </tbody>
    </table>;
  }

  renderUTCoverage () {
    if (this.state.measures['coverage'] == null) {
      return null;
    }
    return <div className="big-spacer-top">
      <h4 className="spacer-bottom">Unit Tests</h4>
      {this.renderCoverage(
          this.state.measures['coverage'],
          this.state.measures['line_coverage'],
          this.state.measures['branch_coverage'],
          '')}
    </div>;
  }

  renderITCoverage () {
    if (this.state.measures['it_coverage'] == null) {
      return null;
    }
    return <div className="big-spacer-top">
      <h4 className="spacer-bottom">Integration Tests</h4>
      {this.renderCoverage(
          this.state.measures['it_coverage'],
          this.state.measures['it_line_coverage'],
          this.state.measures['it_branch_coverage'],
          'it_')}
    </div>;
  }

  renderOverallCoverage () {
    if (this.state.measures['coverage'] == null ||
        this.state.measures['it_coverage'] == null ||
        this.state.measures['overall_coverage'] == null) {
      return null;
    }
    return <div className="big-spacer-top">
      <h4 className="spacer-bottom">Overall</h4>
      {this.renderCoverage(
          this.state.measures['overall_coverage'],
          this.state.measures['overall_line_coverage'],
          this.state.measures['overall_branch_coverage'],
          'overall_')}
    </div>;
  }

  render () {
    return <div className="overview-domain-section">
      <h2 className="overview-title">Coverage Details</h2>
      {this.renderUTCoverage()}
      {this.renderITCoverage()}
      {this.renderOverallCoverage()}
    </div>;
  }
}
