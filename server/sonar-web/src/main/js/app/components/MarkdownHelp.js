/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';

export default function MarkdownHelp() {
  return (
    <div className="page page-limited">
      <h2 className="spacer-bottom">Markdown Syntax</h2>
      <table className="width-100 data zebra">
        <thead>
          <tr>
            <th>Write:</th>
            <th>To display:</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>*this text is bold*</td>
            <td className="markdown">
              <strong>this text is bold</strong>
            </td>
          </tr>
          <tr>
            <td>{'http://sonarqube.org'}</td>
            <td className="markdown">
              <a href="http://sonarqube.org">{'http://sonarqube.org'}</a>
            </td>
          </tr>
          <tr>
            <td className="text-top">[SonarQube™ Home Page](http://www.sonarqube.org)</td>
            <td className="markdown text-top">
              <a href="http://www.sonarqube.org" target="_blank">
                SonarQube™ Home Page
              </a>
            </td>
          </tr>
          <tr>
            <td className="text-top">
              * first item<br />
              * second item
            </td>
            <td className="markdown">
              <ul>
                <li>first item</li>
                <li>second item</li>
              </ul>
            </td>
          </tr>
          <tr>
            <td className="text-top">
              1. first item<br />
              1. second item
            </td>
            <td className="markdown text-top">
              <ol>
                <li>first item</li>
                <li>second item</li>
              </ol>
            </td>
          </tr>
          <tr>
            <td className="text-top">
              = Heading Level 1<br />
              == Heading Level 2<br />
              === Heading Level 3<br />
              ==== Heading Level 4<br />
              ===== Heading Level 5<br />
              ====== Heading Level 6<br />
            </td>
            <td className="markdown text-top">
              <h1>Heading Level 1</h1>
              <h2>Heading Level 2</h2>
              <h3>Heading Level 3</h3>
              <h4>Heading Level 4</h4>
              <h5>Heading Level 5</h5>
              <h6>Heading Level 6</h6>
            </td>
          </tr>
          <tr>
            <td className="text-top">``Lists#newArrayList()``</td>
            <td className="markdown text-top">
              <code>Lists#newArrayList()</code>
            </td>
          </tr>
          <tr>
            <td className="text-top">
              ``<br />
              // code on multiple lines<br />
              {'public void foo() {'}
              <br />
              &nbsp;&nbsp;{'// do some logic here'}
              <br />
              {'}'}
              <br />
              ``
            </td>
            <td className="markdown text-top">
              <pre>
                {'// code on multiple lines\npublic void foo() {\n  // do some logic here\n}'}
              </pre>
            </td>
          </tr>
          <tr>
            <td className="text-top">
              Standard text<br />
              &gt; Blockquoted text<br />
              &gt; that spans multiple lines<br />
            </td>
            <td className="markdown text-top">
              <p>Standard text</p>
              <blockquote>
                Blockquoted text<br />
                that spans multiple lines<br />
              </blockquote>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
