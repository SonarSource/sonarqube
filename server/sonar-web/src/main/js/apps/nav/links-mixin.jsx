import React from 'react';

export default {
  activeLink(url) {
    return window.location.pathname.indexOf(window.baseUrl + url) === 0 ? 'active' : null;
  },

  renderLink(url, title, highlightUrl = url) {
    let fullUrl = window.baseUrl + url;
    return (
        <li key={highlightUrl} className={this.activeLink(highlightUrl)}>
          <a href={fullUrl}>{title}</a>
        </li>
    );
  }
};
