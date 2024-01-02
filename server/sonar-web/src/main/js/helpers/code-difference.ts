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
import { diffLines } from 'diff';
import { groupBy, keyBy } from 'lodash';
import { sanitizeString } from './sanitize';

const NUMBER_OF_EXAMPLES = 2;

type DiffBlock = { noncompliant: Element; compliant: Element };

export default function applyCodeDifferences(element: Element | null) {
  if (element === null) {
    return;
  }
  const codeExamples = getExamplesFromDom(element);

  codeExamples.forEach(({ noncompliant, compliant }) => {
    if (noncompliant === undefined || compliant === undefined) {
      return;
    }
    const [markedNonCompliant, markedCompliantCode] = differentiateCode(
      noncompliant.innerHTML,
      compliant.innerHTML,
    );

    replaceInDom(noncompliant, markedNonCompliant);
    replaceInDom(compliant, markedCompliantCode);
  });
}

function getExamplesFromDom(element: Element) {
  const pres = Array.from(element.querySelectorAll(`pre[data-diff-id]`));

  return (
    Object.values(
      groupBy(
        pres.filter((e) => e.getAttribute('data-diff-id') !== undefined),
        (e) => e.getAttribute('data-diff-id'),
      ),
    )
      // If we have 1 or 3+ example we can't display any differences
      .filter((diffsBlock) => diffsBlock.length === NUMBER_OF_EXAMPLES)
      .map(
        (diffBlock) =>
          keyBy(diffBlock, (block) => block.getAttribute('data-diff-type')) as DiffBlock,
      )
  );
}

function differentiateCode(compliant: string, nonCompliant: string) {
  const hunks = diffLines(compliant, nonCompliant);

  let nonCompliantCode = '';
  let compliantCode = '';

  hunks.forEach((hunk) => {
    const { value } = hunk;
    if (!hunk.added && !hunk.removed) {
      nonCompliantCode += value;
      compliantCode += value;
    }

    if (hunk.added) {
      compliantCode += `<div class='code-added'>${value}</div>`;
    }

    if (hunk.removed) {
      nonCompliantCode += `<div class='code-removed'>${value}</div>`;
    }
  });
  return [sanitizeString(nonCompliantCode), sanitizeString(compliantCode)];
}

function replaceInDom(current: Element, code: string) {
  const markedCode = document.createElement('pre');
  markedCode.classList.add('code-difference-scrollable');
  const div = document.createElement('div');
  div.classList.add('code-difference-container');
  div.innerHTML = code;
  markedCode.appendChild(div);
  current.parentNode?.replaceChild(markedCode, current);
}
