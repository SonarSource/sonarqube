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
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
import { hasMessage, translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import ButtonToggle from '../../controls/ButtonToggle';
import Step from '../components/Step';

export interface SelectAlmStepProps {
  alm?: AlmKeys;
  open: boolean;
  onCheck: (value: AlmKeys) => void;
  onOpen: () => void;
}

function getAlmLongName(alm: AlmKeys) {
  return hasMessage('alm', alm, 'long') ? translate('alm', alm, 'long') : translate('alm', alm);
}

export default function SelectAlmStep(props: SelectAlmStepProps) {
  const { alm, open } = props;
  return (
    <Step
      finished={true}
      open={open}
      onOpen={props.onOpen}
      renderForm={() => (
        <div className="boxed-group-inner">
          <ButtonToggle
            label={translate('onboarding.tutorial.with.jenkins.alm_selection.title')}
            onCheck={props.onCheck}
            options={[
              AlmKeys.BitbucketCloud,
              AlmKeys.BitbucketServer,
              AlmKeys.GitHub,
              AlmKeys.GitLab,
            ].map((almKey) => ({
              label: getAlmLongName(almKey),
              value: almKey,
            }))}
            value={alm}
          />
        </div>
      )}
      renderResult={() =>
        alm && (
          <div className="boxed-group-actions display-flex-center">
            <AlertSuccessIcon className="spacer-right" />
            {getAlmLongName(alm)}
          </div>
        )
      }
      stepTitle={translate('onboarding.tutorial.with.jenkins.alm_selection.title')}
    />
  );
}
