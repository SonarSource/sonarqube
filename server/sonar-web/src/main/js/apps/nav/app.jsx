import React from 'react';
import GlobalNav from './global/global-nav';
import ComponentNav from './component/component-nav';
import SettingsNav from './settings/settings-nav';
import '../../components/workspace/main';

export default {
  start(options) {
    window.requestMessages().done(() => {
      this.renderGlobalNav(options);
      options.space === 'component' && this.renderComponentNav(options);
      options.space === 'settings' && this.renderSettingsNav(options);
    });
  },

  renderGlobalNav(options) {
    const el = document.getElementById('global-navigation');
    if (el) {
      React.render(<GlobalNav {...options}/>, el);
    }
  },

  renderComponentNav(options) {
    const el = document.getElementById('context-navigation');
    if (el) {
      React.render(<ComponentNav {...options}/>, el);
    }
  },

  renderSettingsNav(options) {
    const el = document.getElementById('context-navigation');
    if (el) {
      React.render(<SettingsNav {...options}/>, el);
    }
  }
};
