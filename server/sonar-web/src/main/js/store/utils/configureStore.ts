/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import { createStore, applyMiddleware, compose } from 'redux';
import thunk, { ThunkMiddleware } from 'redux-thunk';

type RootReducer = typeof import('../rootReducer').default;
type State = import('../rootReducer').Store;

const middlewares = [thunk as ThunkMiddleware<State, any>];
const composed = [];

if (process.env.NODE_ENV === 'development') {
  const { createLogger } = require('redux-logger');
  middlewares.push(createLogger());

  const { devToolsExtension } = window as any;
  composed.push(devToolsExtension ? devToolsExtension() : (f: Function) => f);
}

const finalCreateStore = compose(
  applyMiddleware(...middlewares),
  ...composed
)(createStore);

export default function configureStore(rootReducer: RootReducer, initialState?: State) {
  return finalCreateStore(rootReducer, initialState);
}
