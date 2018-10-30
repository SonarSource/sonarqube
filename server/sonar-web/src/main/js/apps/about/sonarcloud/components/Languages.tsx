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
  { name: 'Java', file: 'java.svg', width: 39 },
  { name: 'JavaScript', file: 'js.svg', width: 60 },
  { name: 'TypeScript', file: 'ts.svg', width: 100 },
  { name: 'C#', file: 'csharp.svg', width: 60 },
  { name: 'Python', file: 'python.svg', width: 42 },
  { name: 'C++', file: 'c-c-plus-plus.svg', width: 53 },
  { name: 'Go', file: 'go.svg', width: 91 },
  { name: 'Kotlin', file: 'kotlin.svg', width: 42 },
  { name: 'Ruby', file: 'ruby.svg', width: 43 },
  { name: 'ABAP', file: 'abap.svg', width: 52 },
  { name: 'Flex', file: 'flex.svg', width: 40 },
  { name: 'CSS', file: 'css.svg', width: 40 },
  { name: 'HTML', file: 'html5.svg', width: 40 },
  { name: 'Objective-C', file: 'obj-c.svg', width: 60 },
  { name: 'PHP', file: 'php.svg', width: 57 },
  { name: 'Scala', file: 'scala.svg', width: 29 },
  { name: 'Swift', file: 'swift.svg', width: 40 },
  { name: 'T-SQL', file: 't-sql.svg', width: 53 },
  { name: 'PL/SQL', file: 'pl-sql.svg', width: 65 },
  { name: 'VB', file: 'vb.svg', width: 55 },
  { name: 'XML', file: 'xml.svg', width: 67 }
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
                    style={{ width: language.width + 'px' }}
                  />
                </li>
              ))}
              {!open && (
                <li>
                  <a className="show-more" href="#" onClick={this.handleOpenClick}>
                    …
                  </a>
                </li>
              )}
            </ul>
          </div>
        </div>
      </div>
    );
  }
}
