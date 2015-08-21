import React from 'react';
import Item from './item';

export default React.createClass({
  propTypes: {
    items: React.PropTypes.array.isRequired,
    renderItem: React.PropTypes.func.isRequired,
    getItemKey: React.PropTypes.func.isRequired,
    selectItem: React.PropTypes.func.isRequired,
    deselectItem: React.PropTypes.func.isRequired
  },

  render() {
    let renderedItems = this.props.items.map(item => {
      let key = this.props.getItemKey(item);
      return <Item key={key} {...this.props} item={item} />;
    });
    return (
        <ul>{renderedItems}</ul>
    );
  }
});
