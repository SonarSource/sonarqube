import _ from 'underscore';
import React from 'react';
import { BubbleChart } from '../../../components/charts/bubble-chart';
import { getProjectUrl } from '../../../helpers/Url';
import { getFiles } from '../../../api/components';
import { formatMeasure } from '../formatting';


const X_METRIC = 'ncloc';
const Y_METRIC = 'duplicated_blocks';
const SIZE_METRIC = 'duplicated_lines';
const COMPONENTS_METRICS = [X_METRIC, Y_METRIC, SIZE_METRIC];
const HEIGHT = 360;


function formatInt (d) {
  return window.formatMeasure(d, 'SHORT_INT');
}

function getMeasure (component, metric) {
  return component.measures[metric] || 0;
}


export class DuplicationsBubbleChart extends React.Component {
  constructor () {
    super();
    this.state = { loading: true, files: [] };
  }

  componentDidMount () {
    this.requestFiles();
  }

  requestFiles () {
    return getFiles(this.props.component.key, COMPONENTS_METRICS).then(r => {
      let files = r.map(file => {
        let measures = {};
        (file.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(file, { measures });
      });
      this.setState({ loading: false, files });
    });
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderBubbleChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let items = this.state.files.map(component => {
      return {
        x: getMeasure(component, X_METRIC),
        y: getMeasure(component, Y_METRIC),
        size: getMeasure(component, SIZE_METRIC),
        link: getProjectUrl(component.key)
      };
    });
    let xGrid = this.state.files.map(component => component.measures[X_METRIC]);
    let tooltips = this.state.files.map(component => {
      let inner = [
        component.name,
        `Lines of Code: ${formatMeasure(getMeasure(component, X_METRIC), X_METRIC)}`,
        `Duplicated Blocks: ${formatMeasure(getMeasure(component, Y_METRIC), Y_METRIC)}`,
        `Duplicated Lines: ${formatMeasure(getMeasure(component, SIZE_METRIC), SIZE_METRIC)}`
      ].join('<br>');
      return `<div class="text-left">${inner}</div>`;
    });
    return <BubbleChart items={items}
                        xGrid={xGrid}
                        tooltips={tooltips}
                        height={HEIGHT}
                        padding={[25, 30, 50, 60]}
                        formatXTick={formatInt}
                        formatYTick={formatInt}/>;
  }

  render () {
    return <div className="overview-bubble-chart overview-domain-dark">
      <div className="overview-domain-header">
        <h2 className="overview-title">Project Files</h2>
        <ul className="list-inline small">
          <li>X: Lines of Code</li>
          <li>Y: Duplicated Blocks</li>
          <li>Size: Duplicated Lines</li>
        </ul>
      </div>
      <div>
        {this.renderBubbleChart()}
      </div>
    </div>;
  }
}
