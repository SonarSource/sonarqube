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
import { Router, Route, IndexRoute, Redirect, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { syncHistoryWithStore, routerReducer } from 'react-router-redux';

import QualityGatesAppContainer from './containers/QualityGatesAppContainer';
import Intro from './components/Intro';
import DetailsContainer from './containers/DetailsContainer';
import rootReducer from './store/reducers';
import configureStore from '../../components/store/configureStore';

window.sonarqube.appStarted.then(options => {
  const el = document.querySelector(options.el);

  const history = useRouterHistory(createHistory)({
    basename: window.baseUrl + '/quality_gates'
  });

  const finalReducer = combineReducers({
    rootReducer,
    routing: routerReducer
  });

  const store = configureStore(finalReducer);

  const finalHistory = syncHistoryWithStore(history, store);

  render((
      <Provider store={store}>
        <Router history={finalHistory}>
          <Route path="/" component={QualityGatesAppContainer}>
            <IndexRoute component={Intro}/>
            <Route path="show/:id" component={DetailsContainer}/>
            <Redirect from="/index" to="/"/>
          </Route>
        </Router>
      </Provider>
  ), el);
});
