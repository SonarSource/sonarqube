import React from 'react';
import { getMeasures } from '../../../api/measures';
import { formatMeasure } from '../formatting';


const METRICS = [
  'duplicated_blocks',
  'duplicated_files',
  'duplicated_lines',
  'duplicated_lines_density'
];


export class DuplicationsDetails extends React.Component {
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
      <h2 className="overview-title">Details</h2>
      <table className="data zebra">
        <tbody>
        <tr>
          <td>Duplications</td>
          <td className="thin nowrap text-right">
            {formatMeasure(this.state.measures['duplicated_lines_density'], 'duplicated_lines_density')}
          </td>
        </tr>
        <tr>
          <td>Blocks</td>
          <td className="thin nowrap text-right">
            {formatMeasure(this.state.measures['duplicated_blocks'], 'duplicated_blocks')}
          </td>
        </tr>
        <tr>
          <td>Files</td>
          <td className="thin nowrap text-right">
            {formatMeasure(this.state.measures['duplicated_files'], 'duplicated_files')}
          </td>
        </tr>
        <tr>
          <td>Lines</td>
          <td className="thin nowrap text-right">
            {formatMeasure(this.state.measures['duplicated_lines'], 'duplicated_lines')}
          </td>
        </tr>
        </tbody>
      </table>
    </div>;
  }
}
