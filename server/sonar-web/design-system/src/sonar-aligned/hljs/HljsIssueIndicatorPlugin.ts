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

const BREAK_LINE_REGEXP = /\n/g;

/**
 * Plugin for HLJS to add issue indicators to the code.
 *
 * In order to add issue indicators (sidebar with indicators to issue count on each line), the input source code must be preprocessed using the addIssuesToLines() method. This method will update the source code to prepend the list of issues key of this line.
 * Then, the `before` hook of the HLJS plugin will extract the issue keys from the source code, store them in memory for later use, and drop the issue keys token from the source code.
 * Finally, the `after` hook will add some HTML markup with a reference to the issue key in the transformed code source. The actual interactive issue indicators will be attached inside this HTML markup later on with a React Portal.
 *
 * Each line is wrapped with a div element that contains the issue indicators and the line content.
 */
export class HljsIssueIndicatorPlugin {
  static readonly LINE_WRAPPER_STYLE = [
    'display: inline-grid',
    'grid-template-rows: auto',
    'grid-template-columns: 26px 1fr',
    'align-items: center',
  ].join(';');

  private issueKeys: { [line: string]: string[] };
  static readonly LINE_WRAPPER_OPEN_TAG = `<div style="${this.LINE_WRAPPER_STYLE}">`;
  static readonly LINE_WRAPPER_CLOSE_TAG = `</div>`;
  static readonly EMPTY_INDICATOR_COLUMN = `<div></div>`;
  public lineIssueIndicatorElement(issueKey: string) {
    return `<div id="issue-key-${issueKey}"></div>`;
  }

  constructor() {
    this.issueKeys = {};
  }

  'before:highlight'(data: BeforeHighlightContext) {
    data.code = this.extractIssue(data.code);
  }

  'after:highlight'(data: HighlightResult) {
    if (Object.keys(this.issueKeys).length > 0) {
      data.value = this.addIssueIndicator(data.value);
    }
    // reset issueKeys for next CodeSnippet
    this.issueKeys = {};
  }

  addIssuesToLines = (sourceLines: string[], issues: { [line: number]: string[] }) => {
    return sourceLines.map((line, lineIndex) => {
      const issuesByLine = issues[lineIndex];
      if (!issues || !issuesByLine) {
        return line;
      }

      return `[ISSUE_KEYS:${issuesByLine.join(',')}]${line}`;
    });
  };

  private getLines(text: string) {
    if (text.length === 0) {
      return [];
    }
    return text.split(BREAK_LINE_REGEXP);
  }

  private extractIssue(inputHtml: string) {
    const lines = this.getLines(inputHtml);
    const issueKeysPattern = /\[ISSUE_KEYS:([^\]]+)\](.+)/;
    const removeIssueKeysPattern = /\[ISSUE_KEYS:[^\]]+\](.+)/;

    const wrappedLines = lines.map((line, lineNumber) => {
      const match = issueKeysPattern.exec(line);

      if (match) {
        const issueKeys = match[1].split(',');
        if (!this.issueKeys[lineNumber]) {
          this.issueKeys[lineNumber] = issueKeys;
        } else {
          this.issueKeys[lineNumber].push(...issueKeys);
        }
      }

      const result = removeIssueKeysPattern.exec(line);

      return result ? result[1] : line;
    });

    return wrappedLines.join('\n');
  }

  private addIssueIndicator(inputHtml: string) {
    const lines = this.getLines(inputHtml);

    const wrappedLines = lines.map((line, lineNumber) => {
      const issueKeys = this.issueKeys[lineNumber];

      if (issueKeys) {
        // the react portal looks for the first issue key
        const referenceIssueKey = issueKeys[0];
        return [
          HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG,
          this.lineIssueIndicatorElement(referenceIssueKey),
          '<div>',
          line,
          '</div>',
          HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG,
        ].join('');
      }

      // Keep the correct structure when at least one line has issues
      return [
        HljsIssueIndicatorPlugin.LINE_WRAPPER_OPEN_TAG,
        HljsIssueIndicatorPlugin.EMPTY_INDICATOR_COLUMN,
        '<div>',
        line,
        '</div>',
        HljsIssueIndicatorPlugin.LINE_WRAPPER_CLOSE_TAG,
      ].join('');
    });

    return wrappedLines.join('\n');
  }
}

const hljsIssueIndicatorPlugin = new HljsIssueIndicatorPlugin();
export { hljsIssueIndicatorPlugin };
