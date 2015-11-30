import React from 'react';

import QualifierIcon from '../shared/qualifier-icon';


export const TreemapBreadcrumbs = React.createClass({
  propTypes: {
    breadcrumbs: React.PropTypes.arrayOf(React.PropTypes.shape({
      key: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired,
      qualifier: React.PropTypes.string.isRequired
    }).isRequired).isRequired
  },

  handleItemClick(item, e) {
    e.preventDefault();
    this.props.onRectangleClick(item);
  },

  handleReset(e) {
    e.preventDefault();
    this.props.onReset();
  },

  renderHome() {
    return <span className="treemap-breadcrumbs-item">
      <a onClick={this.handleReset} className="icon-home" href="#"/>
    </span>;
  },

  renderBreadcrumbsItems(b) {
    return <span key={b.key} className="treemap-breadcrumbs-item" title={b.name}>
      <i className="icon-chevron-right"/>
      <QualifierIcon qualifier={b.qualifier}/>
      <a onClick={this.handleItemClick.bind(this, b)} href="#">{b.name}</a>
    </span>;
  },

  render() {
    let breadcrumbs = this.props.breadcrumbs.map(this.renderBreadcrumbsItems);
    return <div className="treemap-breadcrumbs">
      {this.props.breadcrumbs.length ? this.renderHome() : null}
      {breadcrumbs}
    </div>;
  }
});
