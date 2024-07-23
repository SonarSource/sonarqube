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

interface UnderlineRangePosition {
  cursorOffset: number;
  line: number;
}

interface UnderlineRange {
  end: UnderlineRangePosition;
  start: UnderlineRangePosition;
}

export class HljsUnderlinePlugin {
  static readonly SPAN_REGEX = '<\\/?span[^>]*>';

  static readonly TOKEN_PREFIX = 'SNR_TGXRJVF'; // Random string to avoid conflicts with real code

  static readonly TOKEN_SUFFIX_START = '_START';

  static readonly TOKEN_SUFFIX_END = '_END';

  static readonly TOKEN_START =
    HljsUnderlinePlugin.TOKEN_PREFIX + HljsUnderlinePlugin.TOKEN_SUFFIX_START;

  static readonly TOKEN_END =
    HljsUnderlinePlugin.TOKEN_PREFIX + HljsUnderlinePlugin.TOKEN_SUFFIX_END;

  static readonly OPEN_TAG = '<span data-testid="hljs-sonar-underline" class="sonar-underline">';

  static readonly CLOSE_TAG = '</span>';

  /**
   * Add a pair of tokens to the source code to mark the start and end of the content to be underlined.
   */
  tokenize(source: string[], ranges: UnderlineRange[]): string[] {
    // Order ranges by start position, ascending
    ranges.sort((a, b) => {
      if (a.start.line === b.start.line) {
        return a.start.cursorOffset - b.start.cursorOffset;
      }
      return a.start.line - b.start.line;
    });

    // We want to merge overlapping ranges to ensure the underline markup doesn't intesect with itself in the after hook
    const simplifiedRanges: UnderlineRange[] = [];
    let currentRange = ranges[0];
    for (let i = 1; i < ranges.length; i++) {
      const nextRange = ranges[i];

      if (
        currentRange.start.line <= nextRange.start.line &&
        currentRange.start.cursorOffset <= nextRange.start.cursorOffset &&
        currentRange.end.line >= nextRange.end.line &&
        currentRange.end.cursorOffset >= nextRange.end.cursorOffset
      ) {
        // Range is contained in the current range. Do nothing
      } else if (
        currentRange.end.line >= nextRange.start.line &&
        currentRange.end.cursorOffset >= nextRange.start.cursorOffset
      ) {
        // Ranges overlap
        currentRange.end = nextRange.end;
      } else {
        simplifiedRanges.push(currentRange);
        currentRange = nextRange;
      }
    }
    simplifiedRanges.push(currentRange);

    // Add tokens to the source code, from the end to the start to avoid messing up the indices
    for (let i = simplifiedRanges.length - 1; i >= 0; i--) {
      const range = simplifiedRanges[i];

      source[range.end.line] = [
        source[range.end.line].slice(0, range.end.cursorOffset),
        HljsUnderlinePlugin.TOKEN_END,
        source[range.end.line].slice(range.end.cursorOffset),
      ].join('');

      // If there are lines between the start and end, we re-tokenize each line
      if (range.end.line !== range.start.line) {
        source[range.end.line] = HljsUnderlinePlugin.TOKEN_START + source[range.end.line];
        for (let j = range.end.line - 1; j > range.start.line; j--) {
          source[j] = [
            HljsUnderlinePlugin.TOKEN_START,
            source[j],
            HljsUnderlinePlugin.TOKEN_END,
          ].join('');
        }
        source[range.start.line] += HljsUnderlinePlugin.TOKEN_END;
      }

      source[range.start.line] = [
        source[range.start.line].slice(0, range.start.cursorOffset),
        HljsUnderlinePlugin.TOKEN_START,
        source[range.start.line].slice(range.start.cursorOffset),
      ].join('');
    }

    return source;
  }

  'after:highlight'(result: HighlightResult) {
    const re = new RegExp(HljsUnderlinePlugin.TOKEN_START, 'g');
    re.lastIndex = 0;
    let match = re.exec(result.value);
    while (match) {
      result.value = this.replaceTokens(result.value, match.index);
      match = re.exec(result.value);
    }
  }

  /**
   * Whether the content is intersecting with HTML <span> tags added by HLJS or this plugin.
   */
  isIntersectingHtmlMarkup(content: string) {
    const re = new RegExp(HljsUnderlinePlugin.SPAN_REGEX, 'g');
    let depth = 0;
    let intersecting = false;
    let tag = re.exec(content);
    while (tag) {
      if (tag[0].startsWith('</')) {
        depth--;
      } else {
        depth++;
      }

      // If at any point we're closing one-too-many tag, we're intersecting
      if (depth < 0) {
        intersecting = true;
        break;
      }

      tag = re.exec(content);
    }

    // If at the end we're not at 0, we're intersecting
    intersecting = intersecting || depth !== 0;

    return intersecting;
  }

  /**
   * Replace a pair of tokens and everything between with the appropriate HTML markup to underline the content.
   */
  private replaceTokens(htmlMarkup: string, startTokenIndex: number) {
    const endTagIndex = htmlMarkup.indexOf(HljsUnderlinePlugin.TOKEN_END);

    // Just in case the end tag is before the start tag (or the end tag isn't found)
    if (endTagIndex <= startTokenIndex) {
      return htmlMarkup;
    }

    let content = htmlMarkup.slice(
      startTokenIndex + HljsUnderlinePlugin.TOKEN_START.length,
      endTagIndex,
    );

    // If intersecting, we highlight in a safe way
    // We could always use this method, but this creates visual artifacts in the underline wave
    if (this.isIntersectingHtmlMarkup(content)) {
      content = content.replace(
        new RegExp(HljsUnderlinePlugin.SPAN_REGEX, 'g'),
        (tag) => `${HljsUnderlinePlugin.CLOSE_TAG}${tag}${HljsUnderlinePlugin.OPEN_TAG}`,
      );
    }

    // If no intersection, it's safe to add the tags
    const stringRegex = [
      HljsUnderlinePlugin.TOKEN_START,
      '(.+?)',
      HljsUnderlinePlugin.TOKEN_END,
    ].join('');
    htmlMarkup = htmlMarkup.replace(
      new RegExp(stringRegex, 's'),
      `${HljsUnderlinePlugin.OPEN_TAG}${content}${HljsUnderlinePlugin.CLOSE_TAG}`,
    );

    return htmlMarkup;
  }
}

const hljsUnderlinePlugin = new HljsUnderlinePlugin();
export { hljsUnderlinePlugin };
