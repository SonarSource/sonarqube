import React from 'react';
import GlobalNav from './global/global-nav';
import ComponentNav from './component/component-nav';

export default {
  start(options) {
    window.requestMessages().done(() => {
      this.renderGlobalNav(options);
      options.space === 'component' && this.renderComponentNav(options);
    });
  },

  renderGlobalNav(options) {
    const el = document.getElementById('global-navigation');
    React.render(<GlobalNav {...options}/>, el);
  },

  renderComponentNav(options) {
    const el = document.getElementById('context-navigation');
    React.render(<ComponentNav {...options}/>, el);
  }
};
