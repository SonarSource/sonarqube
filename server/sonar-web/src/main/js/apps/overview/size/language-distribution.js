import _ from 'underscore';
import React from 'react';

import { BarChart } from '../../../components/charts/bar-chart';
import { getMeasures } from '../../../api/measures';
import { getLanguages } from '../../../api/languages';


const HEIGHT = 180;
const COMPLEXITY_DISTRIBUTION_METRIC = 'ncloc_language_distribution';


export class LanguageDistribution extends React.Component {
  constructor (props) {
    super(props);
    this.state = { loading: true };
  }

  componentDidMount () {
    this.requestData();
  }

  requestData () {
    return Promise.all([
      getMeasures(this.props.component.key, [COMPLEXITY_DISTRIBUTION_METRIC]),
      getLanguages()
    ]).then(responses => {
      this.setState({
        loading: false,
        distribution: responses[0][COMPLEXITY_DISTRIBUTION_METRIC],
        languages: responses[1]
      });
    });
  }

  getLanguageName (langKey) {
    let lang = _.findWhere(this.state.languages, { key: langKey });
    return lang ? lang.name : window.t('unknown');
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderBarChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let data = this.state.distribution.split(';').map((d, index) => {
      let tokens = d.split('=');
      return { x: index, y: parseInt(tokens[1], 10), lang: tokens[0] };
    });

    let xTicks = data.map(d => this.getLanguageName(d.lang));

    let xValues = data.map(d => window.formatMeasure(d.y, 'INT'));

    return <BarChart data={data}
                     xTicks={xTicks}
                     xValues={xValues}
                     height={HEIGHT}
                     padding={[25, 30, 50, 30]}/>;
  }

  render () {
    return <div className="overview-bar-chart">
      <div className="overview-domain-header">
        <h2 className="overview-title">&nbsp;</h2>
        <ul className="list-inline small">
          <li>Size: Lines of Code</li>
        </ul>
      </div>
      <div>
        {this.renderBarChart()}
      </div>
    </div>;
  }
}
