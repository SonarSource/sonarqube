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

const STORAGE_KEY = 'sonar_recent_history';
const HISTORY_LIMIT = 10;

export default class RecentHistory {
  static get () {
    let history = localStorage.getItem(STORAGE_KEY);
    if (history == null) {
      history = [];
    } else {
      try {
        history = JSON.parse(history);
      } catch (e) {
        RecentHistory.clear();
        history = [];
      }
    }
    return history;
  }

  static set (newHistory) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newHistory));
  }

  static clear () {
    localStorage.removeItem(STORAGE_KEY);
  }

  static add (componentKey, componentName, icon) {
    const sonarHistory = RecentHistory.get();

    if (componentKey) {
      const newEntry = { key: componentKey, name: componentName, icon };
      let newHistory = _.reject(sonarHistory, entry => entry.key === newEntry.key);
      newHistory.unshift(newEntry);
      newHistory = _.first(newHistory, HISTORY_LIMIT);
      RecentHistory.set(newHistory);
    }
  }

  static remove (componentKey) {
    const history = RecentHistory.get();
    const newHistory = _.reject(history, entry => entry.key === componentKey);
    RecentHistory.set(newHistory);
  }
}
