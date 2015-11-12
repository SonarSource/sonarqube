import React from 'react';

import { getComponentIssuesUrl } from '../../helpers/urls';


export const IssuesLink = React.createClass({
  render() {
    let url = getComponentIssuesUrl(this.props.component, this.props.params);
    return <a className={this.props.className} href={url}>{this.props.children}</a>;
  }
});
