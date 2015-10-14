import React from 'react';
import GlobalNav from './global/global-nav';
import ComponentNav from './component/component-nav';
import SettingsNav from './settings/settings-nav';
import {getGlobalNavigation, getComponentNavigation, getSettingsNavigation} from '../../api/nav';
import '../../components/workspace/main';
import '../../helpers/handlebars-helpers';

export default class App {
  start () {
    let options = window.sonarqube;

    return new Promise((resolve) => {
      let response = {},
          requests = [];

      requests.push(
          App.renderGlobalNav(options).then(r => response.global = r)
      );

      if (options.space === 'component') {
        requests.push(
            App.renderComponentNav(options).then(r => response.component = r)
        );
      } else if (options.space === 'settings') {
        requests.push(
            App.renderSettingsNav(options).then(r => response.settings = r)
        );
      }

      Promise.all(requests).then(() => resolve(response));
    });
  }

  static renderGlobalNav (options) {
    return getGlobalNavigation().then(r => {
      const el = document.getElementById('global-navigation');
      if (el) {
        React.render(<GlobalNav {...options} {...r}/>, el);
      }
      return r;
    });
  }

  static renderComponentNav (options) {
    return getComponentNavigation(options.componentKey).then(r => {
      const el = document.getElementById('context-navigation');
      if (el) {
        React.render(<ComponentNav component={r} conf={r.configuration || {}}/>, el);
      }
      return r;
    });
  }

  static renderSettingsNav (options) {
    return getSettingsNavigation().then(r => {
      const el = document.getElementById('context-navigation');
      if (el) {
        React.render(<SettingsNav {...options}/>, el);
      }
      return r;
    });
  }
}
