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

import { BeforeHighlightContext, HighlightResult } from 'highlight.js';
import { hljsIssueIndicatorPlugin, HljsIssueIndicatorPlugin } from '../HljsIssueIndicatorPlugin';

describe('HljsIssueIndicatorPlugin', () => {
  it('should prepend to the line the issues that were found', () => {
    expect(
      hljsIssueIndicatorPlugin.addIssuesToLines(['line1', 'line2', 'line3', `line4`, 'line5'], {
        1: ['123abd', '234asd'],
      }),
    ).toEqual(['line1', '[ISSUE_KEYS:123abd,234asd]line2', 'line3', `line4`, 'line5']);

    expect(
      hljsIssueIndicatorPlugin.addIssuesToLines(['line1', 'line2', 'line3', `line4`, 'line5'], {
        1: ['123abd'],
      }),
    ).toEqual(['line1', '[ISSUE_KEYS:123abd]line2', 'line3', `line4`, 'line5']);
  });
  describe('when tokens exist in the code snippet', () => {
    it('should indicate an issue on a line', () => {
      const inputHtml = {
        code: hljsIssueIndicatorPlugin
          .addIssuesToLines(['line1', 'line2', 'line3', `line4`, 'line5'], { 1: ['123abd'] })
          .join('\n'),
      } as BeforeHighlightContext;
      const result = {
        value: ['line1', `line2`, 'line3', `line4`, 'line5'].join('\n'),
      } as HighlightResult;

      //find issue keys
      hljsIssueIndicatorPlugin['before:highlight'](inputHtml);
      //add the issue indicator html
      hljsIssueIndicatorPlugin['after:highlight'](result);

      expect(result.value).toEqual(
        [
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line1</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}<div id="issue-key-123abd"></div><div>line2</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line3</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line4</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line5</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
        ].join('\n'),
      );
    });

    it('should support multiple issues found on one line', () => {
      const inputHtml = {
        code: hljsIssueIndicatorPlugin
          .addIssuesToLines(['line1', 'line2 issue2', 'line3', `line4`, 'line5'], {
            1: ['123abd', '234asd'],
          })
          .join('\n'),
      } as BeforeHighlightContext;
      const result = {
        value: ['line1', `line2 issue2`, 'line3', `line4`, 'line5'].join('\n'),
      } as HighlightResult;

      //find issue keys
      hljsIssueIndicatorPlugin['before:highlight'](inputHtml);
      //add the issue indicator html
      hljsIssueIndicatorPlugin['after:highlight'](result);

      expect(result.value).toEqual(
        [
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line1</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}<div id="issue-key-123abd"></div><div>line2 issue2</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line3</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line4</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
          `${HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG}${HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN}<div>line5</div>${HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG}`,
        ].join('\n'),
      );
    });

    it('should not render anything if no source code is passed', () => {
      const inputHtml = {
        code: '',
      } as BeforeHighlightContext;
      const result = {
        value: '',
      } as HighlightResult;

      //find issue keys
      hljsIssueIndicatorPlugin['before:highlight'](inputHtml);
      //add the issue indicator html
      hljsIssueIndicatorPlugin['after:highlight'](result);

      expect(result.value).toEqual('');
    });
  });

  describe('when no tokens exist in the code snippet', () => {
    it('should not change the source', () => {
      const inputHtml = {
        code: ['line1', `line2`, 'line3', `line4`, 'line5'].join('\n'),
      } as BeforeHighlightContext;
      const result = {
        value: ['line1', `line2`, 'line3', `line4`, 'line5'].join('\n'),
      } as HighlightResult;

      //find issue keys
      hljsIssueIndicatorPlugin['before:highlight'](inputHtml);
      //add the issue indicator html
      hljsIssueIndicatorPlugin['after:highlight'](result);

      expect(result.value).toEqual(['line1', 'line2', 'line3', 'line4', 'line5'].join('\n'));
    });
  });
});
