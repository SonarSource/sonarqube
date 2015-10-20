import $ from 'jquery';
import _ from 'underscore';
import React from 'react';

export class Word extends React.Component {
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
}


export class WordCloud extends React.Component {
  componentDidMount () {
    this.initTooltips();
  }

  componentDidUpdate () {
    this.initTooltips();
  }

  initTooltips () {
    $('[data-toggle="tooltip"]', React.findDOMNode(this))
        .tooltip({ container: 'body', placement: 'bottom', html: true });
  }

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
}

WordCloud.defaultProps = {
  sizeRange: [10, 24]
};

WordCloud.propTypes = {
  sizeRange: React.PropTypes.arrayOf(React.PropTypes.number)
};
