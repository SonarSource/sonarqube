import React from 'react';
import md5 from 'blueimp-md5';

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
    const emailHash = md5.md5(this.props.email || '').trim();
    const url = ('' + window.SS.lf.gravatarServerUrl)
            .replace('{EMAIL_MD5}', emailHash)
            .replace('{SIZE}', this.props.size * 2);
    return <img className="rounded"
                src={url}
                width={this.props.size}
                height={this.props.size}
                alt={this.props.email}/>;
  }
});
