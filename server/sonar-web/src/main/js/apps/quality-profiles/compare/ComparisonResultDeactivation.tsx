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

import { noop } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { DangerButtonSecondary } from '~design-system';
import { Profile, deactivateRule } from '../../../api/quality-profiles';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';

interface Props {
  canDeactivateInheritedRules: boolean;
  onDone: () => Promise<void>;
  profile: Profile;
  ruleKey: string;
}

export default function ComparisonResultDeactivation(props: React.PropsWithChildren<Props>) {
  const { profile, ruleKey, canDeactivateInheritedRules } = props;
  const intl = useIntl();

  const handleDeactivate = () => {
    const data = {
      key: profile.key,
      rule: ruleKey,
    };
    deactivateRule(data).then(props.onDone, noop);
  };

  return (
    <ConfirmButton
      confirmButtonText={intl.formatMessage({ id: 'yes' })}
      modalBody={intl.formatMessage({ id: 'coding_rules.deactivate.confirm' })}
      modalHeader={intl.formatMessage({ id: 'coding_rules.deactivate' })}
      onConfirm={handleDeactivate}
    >
      {({ onClick }) => (
        <Tooltip
          content={
            canDeactivateInheritedRules
              ? intl.formatMessage(
                  { id: 'quality_profiles.comparison.deactivate_rule' },
                  { profile: profile.name },
                )
              : intl.formatMessage({ id: 'coding_rules.can_not_deactivate' })
          }
        >
          <DangerButtonSecondary
            disabled={!canDeactivateInheritedRules}
            onClick={onClick}
            aria-label={intl.formatMessage(
              { id: 'quality_profiles.comparison.deactivate_rule' },
              { profile: profile.name },
            )}
          >
            {intl.formatMessage({ id: 'coding_rules.deactivate' })}
          </DangerButtonSecondary>
        </Tooltip>
      )}
    </ConfirmButton>
  );
}
