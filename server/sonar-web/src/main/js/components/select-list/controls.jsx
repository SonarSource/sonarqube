import _ from 'underscore';
import React from 'react';
import RadioToggle from '../shared/radio-toggle';

export default React.createClass({
  componentWillMount() {
    this.search = _.debounce(this.search, 100);
  },

  search() {
    let query = this.refs.search.value;
    this.props.search(query);
  },

  onCheck(value) {
    switch (value) {
      case 'selected':
        this.props.loadSelected();
        break;
      case 'deselected':
        this.props.loadDeselected();
        break;
      default:
        this.props.loadAll();
    }
  },

  render() {
    let selectionDisabled = !!this.props.query;

    let selectionOptions = [
      { value: 'selected', label: 'Selected' },
      { value: 'deselected', label: 'Not Selected' },
      { value: 'all', label: 'All' }
    ];

    return (
        <div className="select-list-control">
          <div className="pull-left">
            <RadioToggle
                name="select-list-selection"
                options={selectionOptions}
                onCheck={this.onCheck}
                value={this.props.selection}
                disabled={selectionDisabled}/>
          </div>
          <div className="pull-right">
            <input onChange={this.search} ref="search" type="search" placeholder="Search" initialValue={this.props.query}/>
          </div>
        </div>
    );
  }
});
