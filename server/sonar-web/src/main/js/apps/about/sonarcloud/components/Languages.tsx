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
import * as React from 'react';
import { getBaseUrl } from '../../../../helpers/urls';

interface State {
  height?: number;
  open: boolean;
}

const LANGUAGES = [
  { name: 'Java', file: 'java.svg' },
  { name: 'JavaScript', file: 'js.svg' },
  { name: 'TypeScript', file: 'ts.svg' },
  { name: 'C#', file: 'csharp.svg' },
  { name: 'Python', file: 'python.svg' },
  { name: 'C++', file: 'c-c-plus-plus.svg' },
  { name: 'Go', file: 'go.svg' },
  { name: 'Kotlin', file: 'kotlin.svg' },
  { name: 'Ruby', file: 'ruby.svg' },
  { name: 'ABAP', file: 'abap.svg' },
  { name: 'Flex', file: 'flex.svg' },
  { name: 'HTML', file: 'html5.svg' },
  { name: 'Objective-C', file: 'obj-c.svg' },
  { name: 'PHP', file: 'php.svg' },
  { name: 'Swift', file: 'swift.svg' },
  { name: 'T-SQL', file: 't-sql.svg' },
  { name: 'PL/SQL', file: 'pl-sql.svg' },
  { name: 'VB', file: 'vb.svg' },
  { name: 'XML', file: 'xml.svg' }
];

export class Languages extends React.PureComponent<{}, State> {
  container?: HTMLElement | null;
  state: State = { open: false };

  componentDidUpdate() {
    if (this.container && this.container.clientHeight !== this.container.scrollHeight) {
      this.setState({ height: this.container.scrollHeight });
    }
  }

  handleOpenClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.setState({ height: this.container ? this.container.clientHeight : undefined, open: true });
  };

  render() {
    const { open } = this.state;
    const languages = open ? LANGUAGES : LANGUAGES.slice(0, 9);

    return (
      <div className="position-relative">
        <div className="sc-languages-container clearfix">
          <div className="sc-section sc-columns">
            <div className="sc-column-min">
              <h3 className="big-spacer-bottom">
                SonarCloud
                <br />
                speaks your
                <br />
                language
              </h3>
              {!open && (
                <a href="#" onClick={this.handleOpenClick}>
                  See all supported languages
                </a>
              )}
            </div>
            <ul
              className="sc-languages-list"
              ref={node => (this.container = node)}
              style={{ height: this.state.height }}>
              {languages.map(language => (
                <li key={language.name}>
                  <img
                    alt={language.name}
                    src={`${getBaseUrl()}/images/languages/${language.file}`}
                  />
                </li>
              ))}
              {!open && (
                <li>
                  <h3>…</h3>
                </li>
              )}
            </ul>
          </div>
        </div>
      </div>
    );
  }
}
