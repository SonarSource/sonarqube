/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { render } from '../../helpers/testUtils';
import { CodeSyntaxHighlighter } from '../CodeSyntaxHighlighter';

it('renders correctly with no code', () => {
  const { container } = render(
    <CodeSyntaxHighlighter
      htmlAsString={`
        <p>Hello there!</p>

        <p>There's no code here.</p>
      `}
    />,
  );

  // eslint-disable-next-line testing-library/no-node-access
  expect(container.getElementsByClassName('hljs-string').length).toBe(0);
});

it('renders correctly with code', () => {
  const { container } = render(
    <CodeSyntaxHighlighter
      htmlAsString={`
        <p>Hello there!</p>

        <p>There's some <code>"code"</code> here.</p>
     `}
      language="typescript"
    />,
  );

  // eslint-disable-next-line testing-library/no-node-access
  expect(container.getElementsByClassName('hljs-string').length).toBe(1);
});

/*
 * This test reproduces a breaking case for https://sonarsource.atlassian.net/browse/SONAR-20161
 */
it('handles html code snippets', () => {
  const { container } = render(
    <CodeSyntaxHighlighter
      htmlAsString={
        '\u003ch4\u003eNoncompliant code example\u003c/h4\u003e\n\u003cpre data-diff-id\u003d"1" data-diff-type\u003d"noncompliant"\u003e\npublic void Method(MyObject myObject)\n{\n    if (myObject is null)\n    {\n        new MyObject(); // Noncompliant\n    }\n\n    if (myObject.IsCorrupted)\n    {\n        new ArgumentException($"{nameof(myObject)} is corrupted"); // Noncompliant\n    }\n\n    // ...\n}\n\u003c/pre\u003e\n\u003ch4\u003eCompliant solution\u003c/h4\u003e\n\u003cpre data-diff-id\u003d"1" data-diff-type\u003d"compliant"\u003e\npublic void Method(MyObject myObject)\n{\n    if (myObject is null)\n    {\n        myObject \u003d new MyObject(); // Compliant\n    }\n\n    if (myObject.IsCorrupted)\n    {\n        throw new ArgumentException($"{nameof(myObject)} is corrupted"); // Compliant\n    }\n\n    // ...\n}\n\u003c/pre\u003e'
      }
    />,
  );

  expect(container.querySelectorAll('pre')).toHaveLength(2);
});
