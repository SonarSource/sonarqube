import React from 'react';

export default React.createClass({
  propTypes: {
    value: React.PropTypes.string,
    options: React.PropTypes.array.isRequired,
    name: React.PropTypes.string.isRequired,
    onCheck: React.PropTypes.func.isRequired
  },

  getDefaultProps: function () {
    return { disabled: false, value: null };
  },

  onChange(e) {
    let newValue = e.currentTarget.value;
    this.props.onCheck(newValue);
  },

  renderOption(option) {
    let checked = option.value === this.props.value;
    let htmlId = this.props.name + '__' + option.value;
    return (
        <li key={option.value}>
          <input onChange={this.onChange}
                 type="radio"
                 name={this.props.name}
                 value={option.value}
                 id={htmlId}
                 checked={checked}
                 disabled={this.props.disabled}/>
          <label htmlFor={htmlId}>{option.label}</label>
        </li>
    );
  },

  render() {
    let options = this.props.options.map(this.renderOption);
    return (
        <ul className="radio-toggle">{options}</ul>
    );
  }
});
