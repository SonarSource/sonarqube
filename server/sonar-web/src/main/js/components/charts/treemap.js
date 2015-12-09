import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { TreemapBreadcrumbs } from './treemap-breadcrumbs';
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
    prefix: React.PropTypes.string,
    onClick: React.PropTypes.func
  },

  renderLink() {
    if (!this.props.link) {
      return null;
    }

    if (this.props.width < 24 || this.props.height < 24) {
      return null;
    }

    return <a onClick={e => e.stopPropagation()}
              className="treemap-link"
              href={this.props.link}
              style={{ fontSize: 12 }}><span className="icon-link"/></a>;
  },

  render () {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'data-title': this.props.tooltip
      };
    }
    let cellStyles = {
      left: this.props.x,
      top: this.props.y,
      width: this.props.width,
      height: this.props.height,
      backgroundColor: this.props.fill,
      fontSize: SIZE_SCALE(this.props.width / this.props.label.length),
      lineHeight: `${this.props.height}px`,
      cursor: typeof this.props.onClick === 'function' ? 'pointer' : 'default'
    };
    let isTextVisible = this.props.width >= 40 && this.props.height >= 40;
    return <div className="treemap-cell"
                {...tooltipAttrs}
                style={cellStyles}
                onClick={this.props.onClick}>
      <div className="treemap-inner" dangerouslySetInnerHTML={{ __html: this.props.label }}
           style={{ maxWidth: this.props.width, visibility: isTextVisible ? 'visible': 'hidden' }}/>
      {this.renderLink()}
    </div>;
  }
});


export const Treemap = React.createClass({
  propTypes: {
    items: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    height: React.PropTypes.number,
    onRectangleClick: React.PropTypes.func
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  renderWhenNoData () {
    return <div className="sonar-d3">
      <div className="treemap-container" style={{ width: this.state.width, height: this.state.height }}>
        {window.t('no_data')}
      </div>
      <TreemapBreadcrumbs {...this.props}/>
    </div>;
  },

  render () {
    if (!this.state.width || !this.state.height) {
      return <div>&nbsp;</div>;
    }

    if (!this.props.items.length) {
      return this.renderWhenNoData();
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

    let prefix = mostCommitPrefix(this.props.items.map(item => item.label));
    let prefixLength = prefix.length;

    let rectangles = nodes.map(node => {
      const key = node.label;
      let label = prefixLength ? `${prefix}<br>${node.label.substr(prefixLength)}` : node.label;
      const onClick = this.props.canBeClicked(node) ? () => this.props.onRectangleClick(node) : null;
      return <TreemapRect key={key}
                          x={node.x}
                          y={node.y}
                          width={node.dx}
                          height={node.dy}
                          fill={node.color}
                          label={label}
                          prefix={prefix}
                          tooltip={node.tooltip}
                          link={node.link}
                          onClick={onClick}/>;
    });

    return <div className="sonar-d3">
      <div className="treemap-container" style={{ width: this.state.width, height: this.state.height }}>
        {rectangles}
      </div>
      <TreemapBreadcrumbs {...this.props}/>
    </div>;
  }
});
