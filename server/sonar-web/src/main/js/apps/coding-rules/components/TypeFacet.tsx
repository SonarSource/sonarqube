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

import * as React from 'react';
import Facet, { BasicProps } from '../../../components/facets/Facet';
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import { RULE_TYPES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';

export default class TypeFacet extends React.PureComponent<BasicProps> {
  renderName = (type: string) => (
    <>
      <IssueTypeIcon className="sw-mr-1" type={type} />
      {translate('issue.type', type)}
    </>
  );

  renderTextName = (type: string) => translate('issue.type', type);

  render() {
    return (
      <Facet
        {...this.props}
        options={RULE_TYPES}
        property="types"
        renderName={this.renderName}
        renderTextName={this.renderTextName}
      />
    );
  }
}
