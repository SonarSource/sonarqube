import React from 'react';

export default React.createClass({
  propTypes: {
    onCheck: React.PropTypes.func.isRequired,
    initiallyChecked: React.PropTypes.bool,
    thirdState: React.PropTypes.bool
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
    let classNames = ['icon-checkbox'];
    if (this.state.checked) {
      classNames.push('icon-checkbox-checked');
    }
    if (this.props.thirdState) {
      classNames.push('icon-checkbox-single');
    }
    let className = classNames.join(' ');
    return <a onClick={this.toggle} className={className} href="#"/>;
  }
});
