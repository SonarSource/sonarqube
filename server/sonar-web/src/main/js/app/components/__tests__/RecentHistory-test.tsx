/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { get, remove, save } from '../../../helpers/storage';
import RecentHistory, { History } from '../RecentHistory';

jest.mock('../../../helpers/storage', () => ({
  get: jest.fn(),
  remove: jest.fn(),
  save: jest.fn(),
}));

beforeEach(() => {
  jest.mocked(get).mockClear();
  jest.mocked(remove).mockClear();
  jest.mocked(save).mockClear();
});

it('should get existing history', () => {
  const history = [{ key: 'foo', name: 'Foo', icon: ComponentQualifier.Project }];
  jest.mocked(get).mockReturnValueOnce(JSON.stringify(history));
  expect(RecentHistory.get()).toEqual(history);
  expect(get).toHaveBeenCalledWith('sonar_recent_history');
});

it('should get empty history', () => {
  jest.mocked(get).mockReturnValueOnce(null);
  expect(RecentHistory.get()).toEqual([]);
  expect(get).toHaveBeenCalledWith('sonar_recent_history');
});

it('should return [] and clear history in case of failure', () => {
  jest.mocked(get).mockReturnValueOnce('not a json');
  expect(RecentHistory.get()).toEqual([]);
  expect(get).toHaveBeenCalledWith('sonar_recent_history');
  expect(remove).toHaveBeenCalledWith('sonar_recent_history');
});

it('should save history', () => {
  const history = [{ key: 'foo', name: 'Foo', icon: ComponentQualifier.Project }];
  RecentHistory.set(history);
  expect(save).toHaveBeenCalledWith('sonar_recent_history', JSON.stringify(history));
});

it('should clear history', () => {
  RecentHistory.clear();
  expect(remove).toHaveBeenCalledWith('sonar_recent_history');
});

it('should add item to history', () => {
  const history = [{ key: 'foo', name: 'Foo', icon: ComponentQualifier.Project }];
  jest.mocked(get).mockReturnValueOnce(JSON.stringify(history));
  RecentHistory.add('bar', 'Bar', ComponentQualifier.Portfolio);
  expect(save).toHaveBeenCalledWith(
    'sonar_recent_history',
    JSON.stringify([{ key: 'bar', name: 'Bar', icon: ComponentQualifier.Portfolio }, ...history]),
  );
});

it('should keep 10 items maximum', () => {
  const history: History = [];
  for (let i = 0; i < 10; i++) {
    history.push({ key: `key-${i}`, name: `name-${i}`, icon: ComponentQualifier.Project });
  }
  jest.mocked(get).mockReturnValueOnce(JSON.stringify(history));
  RecentHistory.add('bar', 'Bar', ComponentQualifier.Portfolio);
  expect(save).toHaveBeenCalledWith(
    'sonar_recent_history',
    JSON.stringify([
      { key: 'bar', name: 'Bar', icon: ComponentQualifier.Portfolio },
      ...history.slice(0, 9),
    ]),
  );
});

it('should remove component from history', () => {
  const history: History = [];
  for (let i = 0; i < 10; i++) {
    history.push({ key: `key-${i}`, name: `name-${i}`, icon: ComponentQualifier.Project });
  }
  jest.mocked(get).mockReturnValueOnce(JSON.stringify(history));
  RecentHistory.remove('key-5');
  expect(save).toHaveBeenCalledWith(
    'sonar_recent_history',
    JSON.stringify([...history.slice(0, 5), ...history.slice(6)]),
  );
});
