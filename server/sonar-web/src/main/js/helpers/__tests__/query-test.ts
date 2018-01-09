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
import * as query from '../query';
import { parseDate } from '../dates';

describe('queriesEqual', () => {
  it('should correctly test equality of two queries', () => {
    expect(query.queriesEqual({ a: 'test', b: 'test' }, { a: 'test', b: 'test' })).toBeTruthy();
    expect(query.queriesEqual({ a: [1, 2], b: 'test' }, { a: [1, 2], b: 'test' })).toBeTruthy();
    expect(query.queriesEqual({ a: 'a' }, { a: 'test', b: 'test' })).toBeFalsy();
    expect(query.queriesEqual({ a: [1, 2], b: 'test' }, { a: [1], b: 'test' })).toBeFalsy();
  });
});

describe('cleanQuery', () => {
  it('should remove undefined and null query items', () => {
    expect(query.cleanQuery({ a: 'b', b: undefined, c: null, d: '', e: 0 })).toMatchSnapshot();
  });
});

describe('parseAsBoolean', () => {
  it('should parse booleans correctly', () => {
    expect(query.parseAsBoolean('false')).toBeFalsy();
    expect(query.parseAsBoolean('true')).toBeTruthy();
  });

  it('should return a default value', () => {
    expect(query.parseAsBoolean('1')).toBeTruthy();
    expect(query.parseAsBoolean('foo')).toBeTruthy();
  });
});

describe('parseAsFacetMode', () => {
  it('should facets modes correctly', () => {
    expect(query.parseAsFacetMode('debt')).toBe('effort');
    expect(query.parseAsFacetMode('effort')).toBe('effort');
    expect(query.parseAsFacetMode('count')).toBe('count');
    expect(query.parseAsFacetMode('random')).toBe('count');
  });
});

describe('parseAsString', () => {
  it('should parse strings correctly', () => {
    expect(query.parseAsString('random')).toBe('random');
    expect(query.parseAsString('')).toBe('');
    expect(query.parseAsString(undefined)).toBe('');
  });
});

describe('parseAsArray', () => {
  it('should parse string arrays correctly', () => {
    expect(query.parseAsArray('1,2,3', query.parseAsString)).toEqual(['1', '2', '3']);
  });
});

describe('parseAsDate', () => {
  it('should parse string date correctly', () => {
    expect(query.parseAsDate('2016-06-20T13:09:48.256Z')).toMatchSnapshot();
    expect(query.parseAsDate('')).toBeFalsy();
    expect(query.parseAsDate()).toBeFalsy();
  });
});

describe('serializeDate', () => {
  const date = parseDate('2016-06-20T13:09:48.256Z');
  it('should serialize string correctly', () => {
    expect(query.serializeDate(date)).toBe('2016-06-20T13:09:48+0000');
    expect(query.serializeDate()).toBeUndefined();
  });
});

describe('serializeString', () => {
  it('should serialize string correctly', () => {
    expect(query.serializeString('foo')).toBe('foo');
    expect(query.serializeString('')).toBeUndefined();
  });
});

describe('serializeStringArray', () => {
  it('should serialize array of string correctly', () => {
    expect(query.serializeStringArray(['1', '2', '3'])).toBe('1,2,3');
    expect(query.serializeStringArray([])).toBeUndefined();
  });
});
