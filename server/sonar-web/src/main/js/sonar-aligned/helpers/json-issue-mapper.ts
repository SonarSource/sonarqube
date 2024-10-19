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
import { Issue } from '../../types/types';

export type PathEntry =
  | {
      key: string;
      type: 'object';
    }
  | {
      index: number;
      type: 'array';
    }
  | {
      index: number;
      type: 'string';
    };

export type PathToCursor = PathEntry[];

type ParseStepResult = {
  endIndex: number;
  found: boolean;
  path?: PathToCursor;
};

export class JsonIssueMapper {
  /**
   * Stop-character for literals (first chars of true, false, null, undefined)
   */
  static readonly TOKEN_LITERAL = 'tfnu'.split('');

  static readonly TOKEN_SCOPE_ENTRY = `{["0123456789${JsonIssueMapper.TOKEN_LITERAL}`.split('');

  static readonly TOKEN_SCOPE_EXIT = ',}]'.split('');

  private readonly code: string;

  private splitCode: string[] | undefined;

  /**
   * Internal cursor position, used during parsing
   */
  private cursorPosition = 0;

  /**
   * Current path to the cursor. used during parsing
   */
  private path: PathToCursor = [];

  constructor(code: string) {
    this.code = code;
  }

  lineOffsetToCursorPosition(startLine: number, startOffset: number): number {
    if (!this.splitCode) {
      this.splitCode = this.code.split('\n');
    }
    const charsBeforeStartLine = this.splitCode.slice(0, startLine - 1).join('\n').length;
    return charsBeforeStartLine + startOffset + (startLine > 1 ? 1 : 0);
  }

  get(cursorPosition: number): PathToCursor {
    this.cursorPosition = cursorPosition;
    this.path = [];

    const result = this.parseValue(0);

    if (!result.found) {
      return [];
    }

    const path = [...this.path];

    // Reset internal state for cleanup
    this.cursorPosition = 0;
    this.path = [];

    return path;
  }

  /**
   * Parse an array. Place the index at the end square bracket or stop as soon as the cursor is found.
   */
  private parseArray(startIndex: number, index = 0): ParseStepResult {
    // Check whether the array is empty
    if (index === 0) {
      const firstTokenIndex = this.parseUntilToken(startIndex + 1, [
        ...JsonIssueMapper.TOKEN_SCOPE_ENTRY,
        ']',
      ]);
      if (this.code[firstTokenIndex] === ']') {
        return {
          endIndex: firstTokenIndex,
          found: this.cursorWithin(startIndex, firstTokenIndex),
        };
      }
    }

    this.path.push({
      type: 'array',
      index,
    });

    // Parse a single value in the array
    const result = this.parseValue(startIndex + 1);
    if (result.found || this.cursorWithin(startIndex, result.endIndex)) {
      return result;
    }

    this.path.pop();

    if (this.code[result.endIndex] === ']') {
      return result;
    }

    // Parse next value if there is one
    return this.parseArray(result.endIndex, index + 1);
  }

  /**
   * Parse an object. Place the index at the end curly bracket or stop as soon as the cursor is found.
   */
  private parseObject(openBracketIndex: number): ParseStepResult {
    const keyResult = this.parseObjectKey(openBracketIndex);
    if (typeof keyResult.key === 'undefined') {
      return keyResult;
    }

    this.path.push({
      type: 'object',
      key: keyResult.key,
    });

    if (keyResult.found) {
      return keyResult;
    }

    const result = this.parseValue(keyResult.endIndex + 1);
    if (result.found) {
      return result;
    }

    this.path.pop();

    if (this.code[result.endIndex] === '}') {
      return result;
    }

    // Handle next key or stop if there are no more
    return this.parseObject(result.endIndex + 1);
  }

  /**
   * Parse an object key. Place the index at the `:` before the value.
   */
  private parseObjectKey(startIndex: number): ParseStepResult & { key?: string } {
    const keyStart = this.parseUntilToken(startIndex, '"}'.split(''));
    if (this.code[keyStart] === '}' || keyStart >= this.code.length) {
      // No entries in the object
      return {
        endIndex: keyStart,
        found: this.cursorWithin(startIndex, keyStart),
      };
    }
    const keyEnd = this.parseUntilToken(keyStart + 1, '"', true);
    const key = this.code.slice(keyStart + 1, keyEnd);

    const colonIndex = this.parseUntilToken(keyEnd, ':');

    return {
      key,
      endIndex: colonIndex,
      found: this.cursorWithin(keyStart, colonIndex),
    };
  }

  /**
   * Parse any JSON value. Place the cursor at the separator after the value (could be one of `,]}`).
   */
  private parseValue(index: number): ParseStepResult {
    // Then, it's either an object, a number or a string
    const valueStart = this.parseUntilToken(index, JsonIssueMapper.TOKEN_SCOPE_ENTRY);
    const valueChar = this.code[valueStart];
    let valueEnd: number;
    if (valueChar === '{') {
      // Object
      const result = this.parseObject(valueStart);
      valueEnd = result.endIndex;
      if (result.found) {
        return result;
      }
    } else if (valueChar === '[') {
      // Array
      const result = this.parseArray(valueStart);
      valueEnd = result.endIndex;
      if (result.found) {
        return result;
      }
    } else if (valueChar === '"') {
      // String
      const result = this.parseString(valueStart);
      valueEnd = result.endIndex;
      if (result.found) {
        return result;
      }
    } else if (JsonIssueMapper.TOKEN_LITERAL.includes(valueChar)) {
      // Literal
      valueEnd = this.parseAnyLiteral(valueStart);
    } else {
      // Number
      valueEnd =
        this.parseUntilToken(valueStart + 1, [...JsonIssueMapper.TOKEN_SCOPE_EXIT, ' ', '\n']) - 1;
    }

    // Find the next key or end of object/array
    const separatorIndex = this.parseUntilToken(valueEnd + 1, JsonIssueMapper.TOKEN_SCOPE_EXIT);

    // Cursor somewhere within the value?
    const found = this.cursorWithin(index, valueEnd);
    return {
      found,
      endIndex: separatorIndex,
    };
  }

  private getStringCursorIndex(firstQuoteIndex: number, endQuoteIndex: number): number {
    const index = this.cursorPosition - firstQuoteIndex;

    // We make it such that if the cursor is on a quote, it is considered to be within the string
    if (index <= 0) {
      return 0;
    }

    let count = -1;
    let i = 0;
    while (i < index) {
      // Ignore escaped characters
      if (this.code[firstQuoteIndex + 1 + i] === '\\') {
        i += 2;
      } else {
        i += 1;
      }
      count++;
    }

    return Math.min(count, endQuoteIndex - firstQuoteIndex - 2);
  }

  /**
   * Parse a string value. Place the cursor at the end quote.
   */
  private parseString(firstQuoteIndex: number): ParseStepResult {
    const endQuoteIndex = this.parseUntilToken(firstQuoteIndex + 1, '"', true);

    // Cursor within string value
    if (this.cursorWithin(firstQuoteIndex, endQuoteIndex)) {
      if (endQuoteIndex - firstQuoteIndex > 1) {
        this.path.push({
          type: 'string',
          index: this.getStringCursorIndex(firstQuoteIndex, endQuoteIndex),
        });
      }

      return {
        found: true,
        endIndex: endQuoteIndex,
      };
    }

    return {
      found: this.cursorWithin(firstQuoteIndex, endQuoteIndex),
      endIndex: endQuoteIndex,
    };
  }

  /**
   * Parse any literal. Place the cursor at the end of the literal (last char).
   */
  private parseAnyLiteral(index: number): number {
    while (index < this.code.length - 1) {
      ++index;

      const char = this.code[index];
      if (!/[a-zA-Z]/.test(char)) {
        return index - 1;
      }
    }
    return index;
  }

  /**
   * Return the first index of the next/prev specified token.
   * If not found, return the index of the end of the code (code.length) or -1 depending on the direction.
   */
  private parseUntilToken(index: number, token: string | string[], ignoreEscaped = false): number {
    const tokens = Array.isArray(token) ? token : [token];

    while (index < this.code.length && index >= 0) {
      if (tokens.includes(this.code[index])) {
        if (!ignoreEscaped) {
          return index;
        }
        // Count number of `\` before the token. If there is an even number, the token is not escaped
        // eg \\\\" -> 4 slashes, not escaped
        // eg \\\" -> 3 slashes, escaped
        let escapeCount = 0;
        while (this.code[index - 1 - escapeCount] === '\\') {
          escapeCount += 1;
        }
        if (escapeCount % 2 === 0) {
          return index;
        }
      }

      index += 1;
    }

    return index;
  }

  /**
   * Whether the cursor position in within the specified bounds (includes these bounds)
   */
  private cursorWithin(startIndex: number, endIndex: number) {
    return startIndex <= this.cursorPosition && this.cursorPosition <= endIndex;
  }
}

export function pathToCursorInCell(path: PathToCursor): {
  cell: number;
  cursorOffset: number;
  line: number;
} | null {
  const [, cellEntry, , lineEntry, stringEntry] = path;
  if (
    cellEntry?.type !== 'array' ||
    lineEntry?.type !== 'array' ||
    stringEntry?.type !== 'string'
  ) {
    return null;
  }
  return {
    cell: cellEntry.index,
    line: lineEntry.index,
    cursorOffset: stringEntry.index,
  };
}

export function getOffsetsForIssue(issue: Issue, data: string) {
  if (!issue.textRange) {
    return { startOffset: null, endOffset: null };
  }

  const mapper = new JsonIssueMapper(data);

  const startOffset = pathToCursorInCell(
    mapper.get(
      mapper.lineOffsetToCursorPosition(issue.textRange.startLine, issue.textRange.startOffset),
    ),
  );
  const endOffset = pathToCursorInCell(
    mapper.get(
      mapper.lineOffsetToCursorPosition(issue.textRange.endLine, issue.textRange.endOffset),
    ),
  );

  return { startOffset, endOffset };
}
