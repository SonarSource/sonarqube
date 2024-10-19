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
import { HighlightResult } from 'highlight.js';
import { HljsUnderlinePlugin, hljsUnderlinePlugin } from '../HljsUnderlinePlugin';

const START_TOKEN = HljsUnderlinePlugin.TOKEN_START;
const END_TOKEN = HljsUnderlinePlugin.TOKEN_END;

describe('should add tokens', () => {
  it('with multiple overlapping ranges', () => {
    expect(
      hljsUnderlinePlugin.tokenize(
        ['line1', 'line2', 'line3', 'line4', 'line5'],
        [
          {
            start: { line: 1, cursorOffset: 2 },
            end: { line: 2, cursorOffset: 2 },
          },
          {
            start: { line: 3, cursorOffset: 2 },
            end: { line: 3, cursorOffset: 4 },
          },
          {
            start: { line: 1, cursorOffset: 1 },
            end: { line: 1, cursorOffset: 3 },
          },
        ],
      ),
    ).toEqual([
      'line1',
      `l${START_TOKEN}ine2${END_TOKEN}`,
      `${START_TOKEN}li${END_TOKEN}ne3`,
      `li${START_TOKEN}ne${END_TOKEN}4`,
      'line5',
    ]);
  });

  it('highlight multiple issues on the same line', () => {
    expect(
      hljsUnderlinePlugin.tokenize(
        ['line1', 'line2', 'line3', 'line4', 'line5'],
        [
          {
            start: { line: 1, cursorOffset: 1 },
            end: { line: 1, cursorOffset: 2 },
          },
          {
            start: { line: 1, cursorOffset: 3 },
            end: { line: 1, cursorOffset: 4 },
          },
        ],
      ),
    ).toEqual([
      'line1',
      `l${START_TOKEN}i${END_TOKEN}n${START_TOKEN}e${END_TOKEN}2`,
      'line3',
      'line4',
      'line5',
    ]);
  });

  it('highlight multiple successive lines', () => {
    expect(
      hljsUnderlinePlugin.tokenize(
        ['line1', 'line2', 'line3', 'line4', 'line5'],
        [
          {
            start: { line: 1, cursorOffset: 2 },
            end: { line: 4, cursorOffset: 4 },
          },
        ],
      ),
    ).toEqual([
      'line1',
      `li${START_TOKEN}ne2${END_TOKEN}`,
      `${START_TOKEN}line3${END_TOKEN}`,
      `${START_TOKEN}line4${END_TOKEN}`,
      `${START_TOKEN}line${END_TOKEN}5`,
    ]);
  });
});

describe('should detect html markup intersection', () => {
  it.each([
    '... <span a="b"> ....',
    '... </span> ...',
    '<span> ...',
    '... </span>',
    '... </span> ... <span a="b"> ...',
    '... <span><span a="b"> ... </span> ...',
    '... <span> ... <span a="b"> ... </span> ... </span> ... </span> ...',
  ])('should detect intersection (%s)', (code) => {
    expect(hljsUnderlinePlugin.isIntersectingHtmlMarkup(code)).toBe(true);
  });

  it.each([
    '... <span a="b"> ... </span> ...',
    '<span> ... </span> ... <span> ... <span class="abc"><span> ... </span></span> ... </span>',
  ])('should not detect intersection (%s)', (code) => {
    expect(hljsUnderlinePlugin.isIntersectingHtmlMarkup(code)).toBe(false);
  });
});

describe('underline plugin should work', () => {
  it('should underline on different lines', () => {
    const result = {
      value: ['line1', `l${START_TOKEN}ine2`, 'line3', `lin${END_TOKEN}e4`, 'line5'].join('\n'),
    } as HighlightResult;

    hljsUnderlinePlugin['after:highlight'](result);

    expect(result.value).toEqual(
      [
        'line1',
        `l${HljsUnderlinePlugin.OPEN_TAG}ine2`,
        'line3',
        `lin${HljsUnderlinePlugin.CLOSE_TAG}e4`,
        'line5',
      ].join('\n'),
    );
  });

  it('should underline on same lines', () => {
    const result = {
      value: ['line1', `l${START_TOKEN}ine${END_TOKEN}2`, 'line3'].join('\n'),
    } as HighlightResult;

    hljsUnderlinePlugin['after:highlight'](result);

    expect(result.value).toEqual(
      [
        'line1',
        `l${HljsUnderlinePlugin.OPEN_TAG}ine${HljsUnderlinePlugin.CLOSE_TAG}2`,
        'line3',
      ].join('\n'),
    );
  });

  it('should not underline if end tag is before start tag', () => {
    const result = {
      value: ['line1', `l${END_TOKEN}ine${START_TOKEN}2`, 'line3'].join('\n'),
    } as HighlightResult;

    hljsUnderlinePlugin['after:highlight'](result);

    expect(result.value).toEqual(['line1', `l${END_TOKEN}ine${START_TOKEN}2`, 'line3'].join('\n'));
  });

  it('should not underline if there is no end tag', () => {
    const result = {
      value: ['line1', `l${START_TOKEN}ine2`, 'line3'].join('\n'),
    } as HighlightResult;

    hljsUnderlinePlugin['after:highlight'](result);

    expect(result.value).toEqual(['line1', `l${START_TOKEN}ine2`, 'line3'].join('\n'));
  });

  it('should underline even when intersecting html markup', () => {
    const result = {
      value: `.. <span class="hljs-keyword"> .${START_TOKEN}. <span class="hljs-keyword"> .. </span> .. </span> .. ${END_TOKEN} ..`,
    } as HighlightResult;

    hljsUnderlinePlugin['after:highlight'](result);

    expect(result.value).toEqual(
      `.. <span class="hljs-keyword"> .${HljsUnderlinePlugin.OPEN_TAG}. ${HljsUnderlinePlugin.CLOSE_TAG}<span class="hljs-keyword">${HljsUnderlinePlugin.OPEN_TAG} .. ${HljsUnderlinePlugin.CLOSE_TAG}</span>${HljsUnderlinePlugin.OPEN_TAG} .. ${HljsUnderlinePlugin.CLOSE_TAG}</span>${HljsUnderlinePlugin.OPEN_TAG} .. ${HljsUnderlinePlugin.CLOSE_TAG} ..`,
    );
  });
});
