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
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';

interface Props {
  activeDeprecatedRules: number;
  organization: string | null;
  profile: string;
}

export default function ProfileRulesDeprecatedWarning(props: Props) {
  return (
    <div className="quality-profile-rules-deprecated clearfix">
      <span className="pull-left">
        <span className="text-middle">{translate('quality_profiles.deprecated_rules')}</span>
        <HelpTooltip
          className="spacer-left"
          overlay={translate('quality_profiles.deprecated_rules_description')}
        />
      </span>
      <Link
        className="pull-right"
        to={getDeprecatedActiveRulesUrl({ qprofile: props.profile }, props.organization)}>
        {props.activeDeprecatedRules}
      </Link>
    </div>
  );
}
