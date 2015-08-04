import React from 'react';

export default React.createClass({
  render: function () {
    var params = Object.keys(this.props.params).map((key) => {
      return `${key}=${encodeURIComponent(this.props.params[key])}`;
    }).join('|');
    var url = `${baseUrl}/component_issues/index?id=${encodeURIComponent(this.props.component)}#${params}`;
    return <a href={url}>{this.props.children}</a>;
  }
});
