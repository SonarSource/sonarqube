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
import * as PropTypes from 'prop-types';
import { MarkdownHeading } from '../@types/graphql-types';

interface Props {
  children: (props: { headers: MarkdownHeading[] }) => React.ReactNode;
}

interface State {
  headers: MarkdownHeading[];
}

export default class HeaderListProvider extends React.Component<Props, State> {
  state = { headers: [] };

  static childContextTypes = {
    headers: PropTypes.object
  };

  getChildContext = () => {
    return {
      headers: {
        setHeaders: this.setHeaders
      }
    };
  };

  setHeaders = (headers: MarkdownHeading[]) => {
    this.setState({ headers });
  };

  render() {
    const { headers } = this.state;
    return this.props.children({ headers });
  }
}
