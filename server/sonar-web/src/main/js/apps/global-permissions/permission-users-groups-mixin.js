import React from 'react';

export default {
  propTypes: {
    permission: React.PropTypes.object.isRequired,
    max: React.PropTypes.number.isRequired,
    items: React.PropTypes.array,
    total: React.PropTypes.number,
    refresh: React.PropTypes.func.isRequired
  },

  renderNotDisplayed() {
    const notDisplayedCount = this.props.total - this.props.max;
    return notDisplayedCount > 0 ? <span className="note spacer-right" href="#">and {notDisplayedCount} more</span> : null;
  },

  renderItems() {
    const displayed = this.props.items.map(item => {
      return <li key={item.name} className="spacer-left little-spacer-bottom">{this.renderItem(item)}</li>;
    });
    return (
        <ul className="overflow-hidden bordered-left">
          {displayed}
          <li className="spacer-left little-spacer-bottom">
            {this.renderNotDisplayed()}
            {this.renderUpdateLink()}
          </li>
        </ul>
    );
  },

  renderCount() {
    return (
        <ul className="overflow-hidden bordered-left">
          <li className="spacer-left little-spacer-bottom">
            <span className="spacer-right">{this.props.total}</span>
            {this.renderUpdateLink()}
          </li>
        </ul>
    );
  },

  render() {
    return (
        <li className="abs-width-400">
          <div className="pull-left spacer-right">
            <strong>{this.renderTitle()}</strong>
          </div>
          {this.props.items ? this.renderItems() : this.renderCount()}
        </li>
    );
  }
};
