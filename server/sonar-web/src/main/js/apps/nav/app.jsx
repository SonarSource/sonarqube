import React from 'react';
import GlobalNav from './global-nav';

export default {
  start(options) {
    window.requestMessages().done(() => {
      this.renderGlobalNav(options);
    });
  },

  renderGlobalNav(options) {
    const el = document.getElementById('global-navigation');
    React.render(<GlobalNav {...options}/>, el);
  }
};
