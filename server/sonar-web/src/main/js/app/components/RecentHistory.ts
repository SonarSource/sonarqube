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
import { get, remove, save } from 'sonar-ui-common/helpers/storage';

const RECENT_HISTORY = 'sonar_recent_history';
const HISTORY_LIMIT = 10;

export type History = Array<{
  key: string;
  name: string;
  icon: string;
  organization?: string;
}>;

export default class RecentHistory {
  static get(): History {
    const history = get(RECENT_HISTORY);
    if (history == null) {
      return [];
    } else {
      try {
        return JSON.parse(history);
      } catch {
        remove(RECENT_HISTORY);
        return [];
      }
    }
  }

  static set(newHistory: History) {
    save(RECENT_HISTORY, JSON.stringify(newHistory));
  }

  static clear() {
    remove(RECENT_HISTORY);
  }

  static add(
    componentKey: string,
    componentName: string,
    icon: string,
    organization: string | undefined
  ) {
    const sonarHistory = RecentHistory.get();
    const newEntry = { key: componentKey, name: componentName, icon, organization };
    let newHistory = sonarHistory.filter(entry => entry.key !== newEntry.key);
    newHistory.unshift(newEntry);
    newHistory = newHistory.slice(0, HISTORY_LIMIT);
    RecentHistory.set(newHistory);
  }

  static remove(componentKey: string) {
    const history = RecentHistory.get();
    const newHistory = history.filter(entry => entry.key !== componentKey);
    RecentHistory.set(newHistory);
  }
}
