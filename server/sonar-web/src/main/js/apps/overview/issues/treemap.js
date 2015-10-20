import _ from 'underscore';
import React from 'react';

import { Treemap } from '../../../components/charts/treemap';
import { formatMeasure } from '../formatting';
import { getChildren } from '../../../api/components';

const COMPONENTS_METRICS = [
  'lines',
  'sqale_rating'
];

const HEIGHT = 360;

export class IssuesTreemap extends React.Component {
  constructor () {
    super();
    this.state = { loading: true, components: [] };
  }

  componentDidMount () {
    this.requestComponents();
  }

  requestComponents () {
    return getChildren(this.props.component.key, COMPONENTS_METRICS).then(r => {
      let components = r.map(component => {
        let measures = {};
        component.msr.forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(component, { measures });
      });
      this.setState({ loading: false, components });
    });
  }

  // TODO use css
  getRatingColor (rating) {
    switch (rating) {
      case 1:
        return '#00AA00';
      case 2:
        return '#80CC00';
      case 3:
        return '#FFEE00';
      case 4:
        return '#F77700';
      case 5:
        return '#EE0000';
      default:
        return '#777';
    }
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderTreemap () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let items = this.state.components.map(component => {
      return {
        size: component.measures['lines'],
        color: this.getRatingColor(component.measures['sqale_rating'])
      };
    });
    let labels = this.state.components.map(component => component.name);
    let tooltips = this.state.components.map(component => {
      let inner = [
        component.name,
        `Lines: ${formatMeasure(component.measures['lines'], 'lines')}`,
        `SQALE Rating: ${formatMeasure(component.measures['sqale_rating'], 'sqale_rating')}`
      ].join('<br>');
      return `<div class="text-left">${inner}</div>`;
    });
    return <Treemap items={items} labels={labels} tooltips={tooltips} height={HEIGHT}/>;
  }

  render () {
    return <div className="overview-domain-section overview-treemap">
      <div className="overview-domain-header">
        <h2 className="overview-title">Project Components</h2>
        <ul className="list-inline small">
          <li>Size: Lines</li>
          <li>Color: SQALE Rating</li>
        </ul>
      </div>
      <div>
        {this.renderTreemap()}
      </div>
    </div>;
  }
}
