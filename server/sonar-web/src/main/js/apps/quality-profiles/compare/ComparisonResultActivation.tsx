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
import { Button } from '@sonarsource/echoes-react';
import { Spinner } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { Profile } from '../../../api/quality-profiles';
import { getRuleDetails } from '../../../api/rules';
import Tooltip from '../../../components/controls/Tooltip';
import { RuleDetails } from '../../../types/types';
import ActivationFormModal from '../../coding-rules/components/ActivationFormModal';

interface Props {
  onDone: () => Promise<void>;
  organization?: string;
  profile: Profile;
  ruleKey: string;
}

export default function ComparisonResultActivation(props: React.PropsWithChildren<Props>) {
  const { profile, ruleKey } = props;
  const [state, setState] = React.useState<'closed' | 'opening' | 'open'>('closed');
  const [rule, setRule] = React.useState<RuleDetails>();
  const intl = useIntl();

  const isOpen = state === 'open' && !!rule;

  const activateRuleMsg = intl.formatMessage(
    { id: 'quality_profiles.comparison.activate_rule' },
    { profile: profile.name },
  );

  const handleButtonClick = () => {
    setState('opening');
    getRuleDetails({ key: ruleKey, organization: props.organization }).then(
      ({ rule }) => {
        setState('open');
        setRule(rule);
      },
      () => {
        setState('closed');
      },
    );
  };

  return (
    <Spinner loading={state === 'opening'}>
      <Tooltip side="bottom" content={activateRuleMsg}>
        <Button
          isDisabled={state !== 'closed'}
          aria-label={activateRuleMsg}
          onClick={handleButtonClick}
        >
          {intl.formatMessage({ id: 'activate' })}
        </Button>
      </Tooltip>

      {rule && (
        <ActivationFormModal
          isOpen={isOpen}
          onOpenChange={(open) => setState(open ? 'open' : 'closed')}
          modalHeader={intl.formatMessage({ id: 'coding_rules.activate_in_quality_profile' })}
          onClose={() => {
            setState('closed');
          }}
          onDone={props.onDone}
          profiles={[profile]}
          rule={rule}
        />
      )}
    </Spinner>
  );
}
