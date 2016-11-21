/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import _ from 'underscore';
import React from 'react';
import ReactDOM from 'react-dom';

import GlobalNav from './global/global-nav';
import ComponentNav from './component/component-nav';
import SettingsNav from './settings/settings-nav';
import { getGlobalNavigation, getComponentNavigation, getSettingsNavigation } from '../../../api/nav';

export default class App {
  start () {
    const options = window.sonarqube;

    require('../../../components/workspace/main');

    return new Promise((resolve) => {
      const response = {};
      const requests = [];

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
        ReactDOM.render(<GlobalNav {...options} {...r}/>, el);
      }
      return r;
    });
  }

  static renderComponentNav (options) {
    return getComponentNavigation(options.componentKey).then(component => {
      const el = document.getElementById('context-navigation');
      const nextComponent = {
        ...component,
        qualifier: _.last(component.breadcrumbs).qualifier
      };
      if (el) {
        ReactDOM.render(<ComponentNav component={nextComponent} conf={component.configuration || {}}/>, el);
      }
      return component;
    });
  }

  static renderSettingsNav (options) {
    return getSettingsNavigation().then(r => {
      const el = document.getElementById('context-navigation');
      const opts = _.extend(r, options);
      if (el) {
        ReactDOM.render(<SettingsNav {...opts}/>, el);
      }
      return r;
    });
  }
}
