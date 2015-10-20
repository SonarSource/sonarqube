import $ from 'jquery';
import _ from 'underscore';
import d3 from 'd3';
import React from 'react';


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


export class TreemapRect extends React.Component {
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
    return <div className="treemap-cell" {...tooltipAttrs} style={cellStyles}>
      <div className="treemap-inner" dangerouslySetInnerHTML={{ __html: this.props.label }}
           style={{ maxWidth: this.props.width }}/>
    </div>;
  }
}

TreemapRect.propTypes = {
  x: React.PropTypes.number.isRequired,
  y: React.PropTypes.number.isRequired,
  width: React.PropTypes.number.isRequired,
  height: React.PropTypes.number.isRequired,
  fill: React.PropTypes.string.isRequired
};


export class Treemap extends React.Component {
  constructor (props) {
    super();
    this.state = { width: props.width, height: props.height };
  }

  componentDidMount () {
    if (!this.props.width || !this.props.height) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize.bind(this));
    }
    this.initTooltips();
  }

  componentDidUpdate () {
    this.initTooltips();
  }

  componentWillUnmount () {
    if (!this.props.width || !this.props.height) {
      window.removeEventListener('resize', this.handleResize.bind(this));
    }
  }

  initTooltips () {
    $('[data-toggle="tooltip"]', React.findDOMNode(this))
        .tooltip({ container: 'body', placement: 'top', html: true });
  }

  handleResize () {
    let boundingClientRect = React.findDOMNode(this).parentNode.getBoundingClientRect();
    let newWidth = this.props.width || boundingClientRect.width;
    let newHeight = this.props.height || boundingClientRect.height;
    this.setState({ width: newWidth, height: newHeight });
  }

  render () {
    if (!this.state.width || !this.state.height || !this.props.items.length) {
      return <div>&nbsp;</div>;
    }

    let sizeScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.size)])
        .range([5, 45]);
    let treemap = d3.layout.treemap()
        .round(true)
        .value(d => sizeScale(d.size))
        .size([this.state.width, 360]);
    let nodes = treemap
        .nodes({ children: this.props.items })
        .filter(d => !d.children);

    let prefix = mostCommitPrefix(this.props.labels),
        prefixLength = prefix.length;

    let rectangles = nodes.map((node, index) => {
      let label = prefixLength ? `${prefix}<br>${this.props.labels[index].substr(prefixLength)}` :
          this.props.labels[index];
      let tooltip = index < this.props.tooltips.length ? this.props.tooltips[index] : null;
      return <TreemapRect key={index}
                          x={node.x}
                          y={node.y}
                          width={node.dx}
                          height={node.dy}
                          fill={node.color}
                          label={label}
                          prefix={prefix}
                          tooltip={tooltip}/>;
    });

    return <div className="sonar-d3">
      <div className="treemap-container" style={{ width: this.state.width, height: this.state.height }}>
        {rectangles}
      </div>
    </div>;
  }
}

Treemap.propTypes = {
  labels: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
  tooltips: React.PropTypes.arrayOf(React.PropTypes.string)
};
