import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { Treemap } from '../../../components/charts/treemap';
import { formatMeasure } from '../formatting';
import { getChildren } from '../../../api/components';

const COMPONENTS_METRICS = [
  'ncloc',
  'duplicated_lines_density'
];

const HEIGHT = 360;

const COLORS_5 = ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000'];

export class DuplicationsTreemap extends React.Component {
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
        (component.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(component, { measures });
      });
      this.setState({ loading: false, components });
    });
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

    let colorScale = d3.scale.linear().domain([0, 25, 50, 75, 100]);
    colorScale.range(COLORS_5);

    let items = this.state.components.map(component => {
      let duplications = component.measures['duplicated_lines_density'];
      return {
        size: component.measures['ncloc'],
        color: duplications != null ? colorScale(duplications) : '#777'
      };
    });
    let labels = this.state.components.map(component => component.name);
    let tooltips = this.state.components.map(component => {
      let inner = [
        component.name,
        `Lines of Code: ${formatMeasure(component.measures['ncloc'], 'ncloc')}`,
        `Duplications: ${formatMeasure(component.measures['duplicated_lines_density'], 'duplicated_lines_density')}`
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
          <li>Size: Lines of Code</li>
          <li>Color: Duplications</li>
        </ul>
      </div>
      <div>
        {this.renderTreemap()}
      </div>
    </div>;
  }
}
