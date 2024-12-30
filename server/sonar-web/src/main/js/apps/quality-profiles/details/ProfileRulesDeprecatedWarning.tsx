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

import { FormattedMessage } from 'react-intl';
import { FlagMessage, HelperHintIcon, Link } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';

interface Props {
  organization: string;
  activeDeprecatedRules: number;
  profile: string;
}

export default function ProfileRulesDeprecatedWarning(props: Props) {
  return (
    <FlagMessage variant="warning">
      <div className="sw-flex sw-gap-1">
        <FormattedMessage
          defaultMessage={translate('quality_profiles.x_deprecated_rules')}
          id="quality_profiles.x_deprecated_rules"
          values={{
            count: props.activeDeprecatedRules,
            linkCount: (
              <Link to={getDeprecatedActiveRulesUrl({ qprofile: props.profile }, props.organization)}>
                {props.activeDeprecatedRules}
              </Link>
            ),
          }}
        />
        <HelpTooltip overlay={translate('quality_profiles.deprecated_rules_description')}>
          <HelperHintIcon aria-label="help-tooltip" />
        </HelpTooltip>
      </div>
    </FlagMessage>
  );
}
