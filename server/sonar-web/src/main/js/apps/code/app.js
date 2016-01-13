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
import React from 'react';
import { render } from 'react-dom';
import { Provider } from 'react-redux';
import { Router, Route } from 'react-router';
import { createHashHistory } from 'history';
import { syncReduxAndRouter } from 'redux-simple-router';

import Code from './components/Code';
import configureStore from './store/configureStore';

import './styles/code.css';


const store = configureStore();
const history = createHashHistory({
  queryKey: false
});

syncReduxAndRouter(history, store);


window.sonarqube.appStarted.then(({ el, component }) => {
  const CodeWithComponent = () => {
    return <Code component={component}/>;
  };

  render(
      <Provider store={store}>
        <Router history={history}>
          <Route path="/" component={CodeWithComponent}/>
          <Route path="/:path" component={CodeWithComponent}/>
        </Router>
      </Provider>,
      document.querySelector(el));
});
