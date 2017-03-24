/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

type Props = {
  loading: boolean,
  total?: number,
  children?: {}
};

export default class PageHeader extends React.PureComponent {
  props: Props;

  render() {
    return (
      <header className="page-header">
        {this.props.loading && <i className="spinner" />}
        {this.props.children}
        {this.props.total != null &&
          <span className="page-totalcount">
            <strong>{formatMeasure(this.props.total, 'INT')}</strong>
            {' '}
            {translate('organization.members.member(s)')}
          </span>}
      </header>
    );
  }
}
