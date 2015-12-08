import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { TooltipsMixin } from './../mixins/tooltips-mixin';

export const Word = React.createClass({
  propTypes: {
    size: React.PropTypes.number.isRequired,
    text: React.PropTypes.string.isRequired,
    tooltip: React.PropTypes.string,
    link: React.PropTypes.string.isRequired
  },

  render () {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'title': this.props.tooltip
      };
    }
    return <a {...tooltipAttrs} style={{ fontSize: this.props.size }} href={this.props.link}>{this.props.text}</a>;
  }
});


export const WordCloud = React.createClass({
  propTypes: {
    items: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    sizeRange: React.PropTypes.arrayOf(React.PropTypes.number)
  },

  mixins: [TooltipsMixin],

  getDefaultProps() {
    return {
      sizeRange: [10, 24]
    };
  },

  render () {
    let len = this.props.items.length;
    let sortedItems = _.sortBy(this.props.items, (item, idx) => {
      let index = len - idx;
      return (index % 2) * (len - index) + index / 2;
    });

    let sizeScale = d3.scale.linear()
                      .domain([0, d3.max(this.props.items, d => d.size)])
                      .range(this.props.sizeRange);
    let words = sortedItems
        .map((item, index) => <Word key={index}
                                    text={item.text}
                                    size={sizeScale(item.size)}
                                    link={item.link}
                                    tooltip={item.tooltip}/>);
    return <div className="word-cloud">{words}</div>;
  }
});
