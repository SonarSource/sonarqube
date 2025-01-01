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

import { sortBy } from 'lodash';
import * as React from 'react';
import { A11ySkipLink } from '../../../types/types';
import { A11yContext } from './A11yContext';

interface State {
  links: A11ySkipLink[];
}

export default class A11yProvider extends React.Component<React.PropsWithChildren, State> {
  keys: string[] = [];
  state: State = { links: [] };

  addA11ySkipLink = (link: A11ySkipLink) => {
    this.setState((prevState) => {
      const links = [...prevState.links];
      links.push({ ...link, weight: link.weight || 0 });
      return { links };
    });
  };

  removeA11ySkipLink = (link: A11ySkipLink) => {
    this.setState((prevState) => {
      const links = prevState.links.filter((l) => l.key !== link.key);
      return { links };
    });
  };

  render() {
    const links = sortBy(this.state.links, 'weight');
    return (
      <A11yContext.Provider
        value={{
          addA11ySkipLink: this.addA11ySkipLink,
          links,
          removeA11ySkipLink: this.removeA11ySkipLink,
        }}
      >
        {this.props.children}
      </A11yContext.Provider>
    );
  }
}
