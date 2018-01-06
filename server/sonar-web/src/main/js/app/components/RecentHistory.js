/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
const STORAGE_KEY = 'sonar_recent_history';
const HISTORY_LIMIT = 10;

/*::
type History = Array<{
  key: string,
  name: string,
  icon: string,
  organization?: string
}>;
*/

export default class RecentHistory {
  static get() /*: History */ {
    if (!window.localStorage) {
      return [];
    }
    let history = window.localStorage.getItem(STORAGE_KEY);
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

  static set(newHistory /*: History */) /*: void */ {
    if (window.localStorage) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(newHistory));
    }
  }

  static clear() /*: void */ {
    if (window.localStorage) {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  }

  static add(
    componentKey /*: string */,
    componentName /*: string */,
    icon /*: string */,
    organization /*: string | void */
  ) /*: void */ {
    const sonarHistory = RecentHistory.get();
    const newEntry = { key: componentKey, name: componentName, icon, organization };
    let newHistory = sonarHistory.filter(entry => entry.key !== newEntry.key);
    newHistory.unshift(newEntry);
    newHistory = newHistory.slice(0, HISTORY_LIMIT);
    RecentHistory.set(newHistory);
  }

  static remove(componentKey /*: string */) /*: void */ {
    const history = RecentHistory.get();
    const newHistory = history.filter(entry => entry.key !== componentKey);
    RecentHistory.set(newHistory);
  }
}
