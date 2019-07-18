/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { LANGUAGES } from '../utils';
import SCChevronDownIcon from './SCChevronDownIcon';

interface State {
  height?: number;
  open: boolean;
}

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
    const languages = open ? LANGUAGES : LANGUAGES.slice(0, 10);

    return (
      <div className="position-relative">
        <div className="sc-languages-container clearfix">
          <div className="sc-section sc-columns">
            <div className="sc-column-full">
              <h3 className="sc-big-spacer-bottom">SonarCloud speaks your language</h3>
              <ul
                className="sc-languages-list"
                ref={node => (this.container = node)}
                style={{ height: this.state.height }}>
                {languages.map(language => (
                  <li key={language.name}>
                    <img
                      alt={language.name}
                      src={`${getBaseUrl()}/images/languages/${language.file}`}
                      width={language.width}
                    />
                  </li>
                ))}
              </ul>
              {!open && (
                <a
                  className="bt bt-large bt-nav bt-orange2 display-inline-flex-center"
                  href="#"
                  onClick={this.handleOpenClick}>
                  See All Languages
                  <SCChevronDownIcon className="little-spacer-left" />
                </a>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }
}
