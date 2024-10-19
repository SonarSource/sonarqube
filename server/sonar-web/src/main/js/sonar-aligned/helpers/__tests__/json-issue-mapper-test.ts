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
import fs from 'fs';
import path from 'path';
import { JsonIssueMapper } from '../json-issue-mapper';

const FIXTURES_PATH = path.join(__dirname, '..', 'mocks', 'fixtures', 'json');

// Load all json files in fixtures and save them with their file names
function loadFixtures() {
  const fixtureFiles = fs.readdirSync(FIXTURES_PATH);
  const fixtures: { [filename: string]: string } = {};
  fixtureFiles.forEach((file: string) => {
    fixtures[file] = fs.readFileSync(path.join(FIXTURES_PATH, file), 'utf8');
  });
  return fixtures;
}

const fixtures = loadFixtures();

describe('JsonIssueMapper', () => {
  it('should return cursor position in file from line offset', () => {
    const parser = new JsonIssueMapper(fixtures['00-object-simple.json']);
    expect(parser.lineOffsetToCursorPosition(12, 31)).toEqual(225);
  });

  describe('should not fail on invalid json', () => {
    it('should not fail on invalid json', () => {
      expect(new JsonIssueMapper(`This { is ] not " JSON :`).get(3)).toEqual([]);
      expect(new JsonIssueMapper(`AAAAAAAAAAAAAAAAAAA`).get(3)).toEqual([]);
    });

    it('should not fail on non-terminated literals', () => {
      const parser = new JsonIssueMapper(`{"key": nul`);
      expect(parser.get(150)).toEqual([]);
    });
  });

  describe('should return correct path in strings', () => {
    it('gets cursor path in a string value', () => {
      const parser = new JsonIssueMapper(fixtures['00-object-simple.json']);
      for (const cursor of [20, 21]) {
        expect(parser.get(cursor)).toEqual([
          { type: 'object', key: 'first-key' },
          { type: 'string', index: 2 },
        ]);
      }
    });

    it('ignores false-flag characters in strings', () => {
      const parser = new JsonIssueMapper(fixtures['01-object-false-flags.json']);
      const cursor = 111;
      const path = parser.get(cursor);
      expect(path).toEqual([
        { type: 'object', key: '\\"{}}[]]].:-]\\\\\\\\' },
        { type: 'array', index: 0 },
        { type: 'array', index: 0 },
        { type: 'object', key: '\\"{}}[]]].:-]\\\\\\\\' },
        { type: 'string', index: 17 },
      ]);

      const object = JSON.parse(fixtures['01-object-false-flags.json']);
      const value = object['"{}}[]]].:-]\\\\'][0][0]['"{}}[]]].:-]\\\\'];
      expect(value[17]).toEqual('u');
    });

    it('detects cursor in empty strings', () => {
      const parser = new JsonIssueMapper(fixtures['01-object-false-flags.json']);
      for (let cursor = 180; cursor < 186; cursor++) {
        expect(parser.get(cursor)).toEqual([
          { type: 'object', key: 'empty-key-values' },
          { type: 'object', key: '' },
        ]);
      }
    });

    it('detects cursor in empty strings in arrays', () => {
      const parser = new JsonIssueMapper(fixtures['01-object-false-flags.json']);
      for (const cursor of [209, 210, 211, 212]) {
        expect(parser.get(cursor)).toEqual([
          { type: 'object', key: 'empty-key-values' },
          { type: 'object', key: 'list' },
          { type: 'array', index: 1 },
          { type: 'array', index: 0 },
        ]);
      }
    });

    it('beautify stringified path when key is alphanum', () => {
      const parser = new JsonIssueMapper(fixtures['02-object-jupyter-notebook.json']);
      const cursor = 233331;
      expect(parser.get(cursor)).toEqual([
        { type: 'object', key: 'cells' },
        { type: 'array', index: 21 },
        { type: 'object', key: 'outputs' },
        { type: 'array', index: 1 },
        { type: 'object', key: 'data' },
        { type: 'object', key: 'image/png' },
        { type: 'string', index: 23 },
      ]);
    });

    it('gets cursor in minified jupyter notebook files', () => {
      const parser = new JsonIssueMapper(fixtures['04-object-jupyter-notebook-oneline.json']);
      const cursor = 77354;
      expect(parser.get(cursor)).toEqual([
        { type: 'object', key: 'cells' },
        { type: 'array', index: 1 },
        { type: 'object', key: 'source' },
        { type: 'array', index: 8 },
        { type: 'string', index: 14 },
      ]);
    });
  });

  describe('should return correct path in numbers', () => {
    it('gets cursor path in a number value', () => {
      const parser = new JsonIssueMapper(fixtures['00-object-simple.json']);
      for (let cursor = 126; cursor < 138; cursor++) {
        expect(parser.get(cursor)).toEqual([
          { type: 'object', key: 'nested-object' },
          { type: 'object', key: 'second-nested-object' },
          { type: 'object', key: 'foo' },
        ]);
      }
    });

    it('gets cursor path in a number value in arrays', () => {
      const parser = new JsonIssueMapper(fixtures['03-array-simple.json']);
      for (let cursor = 78; cursor < 79; cursor++) {
        expect(parser.get(cursor)).toEqual([
          { type: 'array', index: 0 },
          { type: 'array', index: 3 },
          { type: 'array', index: 2 },
        ]);
      }
    });
  });
});
