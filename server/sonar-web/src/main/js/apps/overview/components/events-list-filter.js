import React from 'react';


const TYPES = ['All', 'Version', 'Alert', 'Profile', 'Other'];


export const EventsListFilter = React.createClass({
  propTypes: {
    onFilter: React.PropTypes.func.isRequired,
    currentFilter: React.PropTypes.string.isRequired
  },

  handleChange() {
    const value = this.refs.select.value;
    this.props.onFilter(value);
  },

  render () {
    const options = TYPES.map(type => <option key={type} value={type}>{window.t('event.category', type)}</option>);
    return <select ref="select" onChange={this.handleChange} value={this.props.currentFilter}>{options}</select>;
  }
});
