import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';


const SIZE_SCALE = d3.scale.linear()
                     .domain([3, 15])
                     .range([11, 18])
                     .clamp(true);


function mostCommitPrefix (strings) {
  var sortedStrings = strings.slice(0).sort(),
      firstString = sortedStrings[0],
      firstStringLength = firstString.length,
      lastString = sortedStrings[sortedStrings.length - 1],
      i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  var prefix = firstString.substr(0, i),
      lastPrefixPart = _.last(prefix.split(/[\s\\\/]/));
  return prefix.substr(0, prefix.length - lastPrefixPart.length);
}


export const TreemapRect = React.createClass({
  propTypes: {
    x: React.PropTypes.number.isRequired,
    y: React.PropTypes.number.isRequired,
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number.isRequired,
    fill: React.PropTypes.string.isRequired,
    label: React.PropTypes.string.isRequired,
    prefix: React.PropTypes.string
  },

  render () {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'title': this.props.tooltip
      };
    }
    let cellStyles = {
      left: this.props.x,
      top: this.props.y,
      width: this.props.width,
      height: this.props.height,
      backgroundColor: this.props.fill,
      fontSize: SIZE_SCALE(this.props.width / this.props.label.length),
      lineHeight: `${this.props.height}px`
    };
    let isTextVisible = this.props.width >= 40 && this.props.height >= 40;
    return <div className="treemap-cell" {...tooltipAttrs} style={cellStyles}>
      <div className="treemap-inner" dangerouslySetInnerHTML={{ __html: this.props.label }}
           style={{ maxWidth: this.props.width, visibility: isTextVisible ? 'visible': 'hidden' }}/>
    </div>;
  }
});


export const Treemap = React.createClass({
  mixins: [ResizeMixin, TooltipsMixin],

  propTypes: {
    items: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    height: React.PropTypes.number
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  render () {
    if (!this.state.width || !this.state.height || !this.props.items.length) {
      return <div>&nbsp;</div>;
    }

    let treemap = d3.layout.treemap()
                    .round(true)
                    .value(d => d.size)
                    .sort((a, b) => a.value - b.value)
                    .size([this.state.width, this.state.height]);
    let nodes = treemap
        .nodes({ children: this.props.items })
        .filter(d => !d.children)
        .filter(d => !!d.dx && !!d.dy);

    let prefix = mostCommitPrefix(this.props.items.map(item => item.label)),
        prefixLength = prefix.length;

    let rectangles = nodes.map((node, index) => {
      let label = prefixLength ? `${prefix}<br>${node.label.substr(prefixLength)}` : node.label;
      return <TreemapRect key={index}
                          x={node.x}
                          y={node.y}
                          width={node.dx}
                          height={node.dy}
                          fill={node.color}
                          label={label}
                          prefix={prefix}
                          tooltip={node.tooltip}/>;
    });

    return <div className="sonar-d3">
      <div className="treemap-container" style={{ width: this.state.width, height: this.state.height }}>
        {rectangles}
      </div>
    </div>;
  }
});
