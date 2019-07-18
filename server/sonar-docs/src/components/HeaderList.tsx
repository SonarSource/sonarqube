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
import * as PropTypes from 'prop-types';
import * as React from 'react';
import { MarkdownHeading } from '../@types/graphql-types';

interface Props {
  headers: MarkdownHeading[];
}

export default class HeaderList extends React.PureComponent<Props> {
  static contextTypes = {
    headers: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.context.headers.setHeaders(this.props.headers);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.headers.length !== this.props.headers.length) {
      this.context.headers.setHeaders(prevProps.headers);
    }
  }

  render() {
    return null;
  }
}
