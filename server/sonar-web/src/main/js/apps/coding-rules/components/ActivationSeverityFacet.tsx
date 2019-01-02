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
import Facet, { BasicProps } from './Facet';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { SEVERITIES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';

interface Props extends BasicProps {
  disabled: boolean;
}

export default class ActivationSeverityFacet extends React.PureComponent<Props> {
  renderName = (severity: string) => <SeverityHelper severity={severity} />;

  renderTextName = (severity: string) => translate('severity', severity);

  render() {
    return (
      <Facet
        {...this.props}
        disabled={this.props.disabled}
        disabledHelper={translate('coding_rules.filters.active_severity.inactive')}
        halfWidth={true}
        options={SEVERITIES}
        property="activationSeverities"
        renderName={this.renderName}
        renderTextName={this.renderTextName}
      />
    );
  }
}
