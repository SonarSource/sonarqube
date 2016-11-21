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
 /* @flow */
import React from 'react';
import ReactDOM from 'react-dom';
import { Router, Route, Redirect, useRouterHistory } from 'react-router';
import { createHistory } from 'history';

import BackgroundTasksApp from './components/BackgroundTasksApp';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + (options.component ? '/project/background_tasks' : '/background_tasks')
  });

  const App = props => <BackgroundTasksApp {...props} component={options.component}/>;

  ReactDOM.render((
      <Router history={history}>
        <Redirect from="/index" to="/"/>
        <Route path="/" component={App}/>
      </Router>
  ), el);
});
