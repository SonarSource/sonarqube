import _ from 'underscore';
import React from 'react';

export default {
  activeLink(url) {
    return window.location.pathname.indexOf(window.baseUrl + url) === 0 ? 'active' : null;
  },

  renderLink(url, title, highlightUrl = url) {
    let fullUrl = window.baseUrl + url;
    let check = _.isFunction(highlightUrl) ? highlightUrl : this.activeLink;
    return (
        <li key={highlightUrl} className={check(highlightUrl)}>
          <a href={fullUrl}>{title}</a>
        </li>
    );
  }
};
