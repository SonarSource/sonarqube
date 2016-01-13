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
import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import ReactDOM from 'react-dom';

import { Overview, EmptyOverview } from './overview';


const LEAK_PERIOD = '1';


class App {
  start (options) {
    let opts = _.extend({}, options, window.sonarqube.overview);
    _.extend(opts.component, options.component);
    opts.urlRoot = window.baseUrl + '/overview';

    $('html').toggleClass('dashboard-page', opts.component.hasSnapshot);
    let el = document.querySelector(opts.el);

    if (opts.component.hasSnapshot) {
      ReactDOM.render(<Overview {...opts} leakPeriodIndex={LEAK_PERIOD}/>, el);
    } else {
      ReactDOM.render(<EmptyOverview/>, el);
    }
  }
}

window.sonarqube.appStarted.then(options => new App().start(options));
