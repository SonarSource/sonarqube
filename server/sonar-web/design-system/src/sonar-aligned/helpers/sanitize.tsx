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

import dompurify from 'dompurify';

import React from 'react';

const { sanitize } = dompurify;

export enum SanitizeLevel {
  FORBID_STYLE, // minimum sanitation level to prevent CSS injections
  FORBID_SVG_MATHML, // adds SVG and MathML exclusion
  USER_INPUT, // adds restrictions on tags and attributes
  RESTRICTED, // adds even more restrictions on tags and attributes
}

export const sanitizeFunctionByLevel = (sanitizeLevel: SanitizeLevel) =>
  ({
    [SanitizeLevel.FORBID_STYLE]: sanitizeHTMLToPreventCSSInjection,
    [SanitizeLevel.FORBID_SVG_MATHML]: sanitizeHTMLNoSVGNoMathML,
    [SanitizeLevel.USER_INPUT]: sanitizeHTMLUserInput,
    [SanitizeLevel.RESTRICTED]: sanitizeHTMLRestricted,
  })[sanitizeLevel];

export const sanitizeHTMLToPreventCSSInjection = (htmlAsString: string) =>
  sanitize(htmlAsString, {
    FORBID_ATTR: ['style'],
    FORBID_TAGS: ['style'],
  });

export function sanitizeHTMLNoSVGNoMathML(htmlAsString: string) {
  return sanitize(htmlAsString, {
    FORBID_ATTR: ['style'],
    FORBID_TAGS: ['style'],
    USE_PROFILES: { html: true },
  });
}

export function sanitizeHTMLUserInput(htmlAsString: string) {
  return sanitize(htmlAsString, {
    ALLOWED_ATTR: ['href', 'rel'],
    ALLOWED_TAGS: [
      'a',
      'b',
      'blockquote',
      'br',
      'code',
      'h1',
      'h2',
      'h3',
      'h4',
      'h5',
      'h6',
      'i',
      'li',
      'ol',
      'p',
      'pre',
      'strong',
      'ul',
    ],
  });
}

export function sanitizeHTMLRestricted(htmlAsString: string) {
  return sanitize(htmlAsString, {
    ALLOWED_ATTR: ['href'],
    ALLOWED_TAGS: ['a', 'b', 'br', 'code', 'i', 'li', 'p', 'strong', 'ul'],
  });
}

/**
 * Safely injects HTML into an element with no risk of XSS attacks.
 *
 * @param children The React element to clone with the sanitized HTML (defaults to a `span`)
 * @param htmlAsString The HTML string to sanitize and inject (required)
 * @param sanitizeLevel The level of sanitation to apply (defaults to `SanitizeLevel.FORBID_STYLE`)
 *
 * @returns A React element with the sanitized HTML injected, and all other props preserved
 *
 * @example
 * Here's a simple example with no children:
 * ```
 * <SafeHTMLInjection htmlAsString={taintedString} />
 * ```
 *
 * @example
 * Here's an example with a custom `sanitizeLevel` and a child `div`:
 * ```
 * <SafeHTMLInjection htmlAsString={taintedString} sanitizeLevel={SanitizeLevel.RESTRICTED}>
 *   // the HTML will be safely injected in the div below, with the className preserved:
 *   <div className="someClassThatWillBePreserved" />
 * </SafeHTMLInjection>
 * ```
 */
export const SafeHTMLInjection = ({
  children,
  htmlAsString,
  sanitizeLevel = SanitizeLevel.FORBID_STYLE,
}: Readonly<{
  children?: React.ReactElement;
  htmlAsString: string;
  sanitizeLevel?: SanitizeLevel;
}>) =>
  React.cloneElement(children ?? <span />, {
    dangerouslySetInnerHTML: { __html: sanitizeFunctionByLevel(sanitizeLevel)(htmlAsString) },
  });
