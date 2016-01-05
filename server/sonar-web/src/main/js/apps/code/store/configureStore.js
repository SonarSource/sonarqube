/*
 * SonarQube :: Web
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
import { createStore, applyMiddleware, combineReducers } from 'redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';
import { routeReducer } from 'redux-simple-router';
import { current, bucket } from '../reducers';

const logger = createLogger({
  predicate: () => process.env.NODE_ENV !== 'production'
});

const createStoreWithMiddleware = applyMiddleware(
    thunk,
    logger
)(createStore);

const reducer = combineReducers({
  routing: routeReducer,
  current,
  bucket
});

export default function configureStore () {
  return createStoreWithMiddleware(reducer);
}
