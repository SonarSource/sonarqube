import React from 'react';

export default React.createClass({
  propTypes: {
    email: React.PropTypes.string,
    size: React.PropTypes.number.isRequired
  },

  render() {
    const shouldShowAvatar = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
    if (!shouldShowAvatar) {
      return null;
    }
    const emailHash = window.md5(this.props.email || '').trim();
    const url = ('' + window.SS.lf.gravatarServerUrl)
            .replace('{EMAIL_MD5}', emailHash)
            .replace('{SIZE}', this.props.size * 2);
    return <img className="rounded" src={url} width={this.props.size} height={this.props.size} alt={this.props.email}/>;
  }
});
