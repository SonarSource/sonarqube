import React from 'react';
import Checkbox from '../shared/checkbox';

export default React.createClass({
  propTypes: {
    item: React.PropTypes.any.isRequired,
    renderItem: React.PropTypes.func.isRequired,
    selectItem: React.PropTypes.func.isRequired,
    deselectItem: React.PropTypes.func.isRequired
  },

  onCheck(checked) {
    checked ? this.props.selectItem(this.props.item) : this.props.deselectItem(this.props.item);
  },

  render() {
    let renderedItem = this.props.renderItem(this.props.item);
    return (
        <li className="panel panel-vertical">
          <div className="display-inline-block text-middle spacer-right">
            <Checkbox onCheck={this.onCheck} initiallyChecked={!!this.props.item.selected}/>
          </div>
          <div className="display-inline-block text-middle" dangerouslySetInnerHTML={{ __html: renderedItem }}/>
        </li>
    );
  }
});
