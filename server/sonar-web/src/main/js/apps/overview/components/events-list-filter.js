import React from 'react';
import Select from 'react-select';


const TYPES = ['All', 'Version', 'Alert', 'Profile', 'Other'];


export const EventsListFilter = React.createClass({
  propTypes: {
    onFilter: React.PropTypes.func.isRequired,
    currentFilter: React.PropTypes.string.isRequired
  },

  handleChange(selected) {
    this.props.onFilter(selected.value);
  },

  render () {
    const options = TYPES.map(type => {
      return {
        value: type,
        label: window.t('event.category', type)
      };
    });
    return <Select value={this.props.currentFilter}
                   options={options}
                   clearable={false}
                   searchable={false}
                   onChange={this.handleChange}
                   style={{ width: '125px' }}/>;
  }
});
