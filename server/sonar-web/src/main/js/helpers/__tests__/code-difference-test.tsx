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

import { render } from '@testing-library/react';
import React from 'react';
import applyCodeDifferences from '../code-difference';
import { SafeHTMLInjection } from '../sanitize';

it('should apply diff view correctly', () => {
  const { container } = renderDom(properCodeSnippet);
  applyCodeDifferences(container);
  // eslint-disable-next-line testing-library/no-container, testing-library/no-node-access
  expect(container.getElementsByClassName('code-difference-scrollable')).toMatchSnapshot(
    'differenciatedCode'
  );
});

it('should not apply diff view if 3 examples are present', () => {
  const { container } = renderDom(codeSnippetWith3Examples);
  applyCodeDifferences(container);
  // eslint-disable-next-line testing-library/no-container, testing-library/no-node-access
  expect(container.getElementsByClassName('code-difference-scrollable').length).toEqual(0);
});

it('should not apply diff view if compliant code is absent', () => {
  const { container } = renderDom(codeSnippetWithoutCompliantCode);
  applyCodeDifferences(container);
  // eslint-disable-next-line testing-library/no-container, testing-library/no-node-access
  expect(container.getElementsByClassName('code-difference-scrollable').length).toEqual(0);
});

const properCodeSnippet = `<!DOCTYPE html><body>
<h1>Some title</h1>
<p>Some paragraph...</p>

<h2>Example 1</h2>

<pre data-diff-id="1" data-diff-type="noncompliant">
public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(data); // Noncompliant
}
</pre>

<p>Some other paragraph</p>

<pre data-diff-id="1" data-diff-type="compliant">
import org.owasp.encoder.Encode;

public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(Encode.forHtml(data));
}
</pre>

<p>Final paragraph</p>
</body></html>`;

const codeSnippetWith3Examples = `<!DOCTYPE html><body>
<h1>Some title</h1>
<p>Some paragraph...</p>

<h2>Example 1</h2>

<pre data-diff-id="1" data-diff-type="noncompliant">
public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(data); // Noncompliant
}
</pre>

<p>Some other paragraph</p>

<pre data-diff-id="1" data-diff-type="compliant">
import org.owasp.encoder.Encode;

public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(Encode.forHtml(data));
}
</pre>

<p>Some other paragraph</p>

<pre data-diff-id="1" data-diff-type="compliant">
import org.owasp.encoder.Encode;

public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(Encode.forHtml(data));
}
</pre>

<p>Final paragraph</p>
</body></html>`;

const codeSnippetWithoutCompliantCode = `<!DOCTYPE html><body>
<h1>Some title</h1>
<p>Some paragraph...</p>

<h2>Example 1</h2>

<pre data-diff-id="1" data-diff-type="noncompliant">
public void endpoint(HttpServletRequest request, HttpServletResponse response) throws IOException
{
    String data        = request.getParameter("input");
    PrintWriter writer = response.getWriter();

    writer.print(data); // Noncompliant
}
</pre>

<p>Some other paragraph</p>
<p>Final paragraph</p>
</body></html>`;

function renderDom(codeSnippet: string) {
  return render(
    <SafeHTMLInjection htmlAsString={codeSnippet}>
      <div className="markdown" />
    </SafeHTMLInjection>
  );
}
