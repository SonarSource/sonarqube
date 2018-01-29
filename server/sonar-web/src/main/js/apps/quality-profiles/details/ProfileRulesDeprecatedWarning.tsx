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
import { Link } from 'react-router';
import Tooltip from '../../../components/controls/Tooltip';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';

interface Props {
  activeDeprecatedRules: number;
  organization: string | null;
  profile: string;
}

export default function ProfileRulesDeprecatedWarning(props: Props) {
  return (
    <div className="quality-profile-rules-deprecated clearfix">
      <span className="pull-left">
        {translate('quality_profiles.deprecated_rules')}
        <Tooltip overlay={translate('quality_profiles.deprecated_rules_description')}>
          <i className="icon-help spacer-left" />
        </Tooltip>
      </span>
      <Link
        className="pull-right"
        to={getDeprecatedActiveRulesUrl({ qprofile: props.profile }, props.organization)}>
        {props.activeDeprecatedRules}
      </Link>
    </div>
  );
}
