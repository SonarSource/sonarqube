import React from 'react';

export default React.createClass({
  propTypes: {
    onCheck: React.PropTypes.func.isRequired,
    initiallyChecked: React.PropTypes.bool
  },

  getInitialState() {
    return { checked: this.props.initiallyChecked || false };
  },

  componentWillReceiveProps(nextProps) {
    if (nextProps.initiallyChecked != null) {
      this.setState({ checked: nextProps.initiallyChecked });
    }
  },

  toggle(e) {
    e.preventDefault();
    this.props.onCheck(!this.state.checked);
    this.setState({ checked: !this.state.checked });
  },

  render() {
    const className = this.state.checked ? 'icon-checkbox icon-checkbox-checked' : 'icon-checkbox';
    return <a onClick={this.toggle} className={className} href="#"/>;
  }
});
