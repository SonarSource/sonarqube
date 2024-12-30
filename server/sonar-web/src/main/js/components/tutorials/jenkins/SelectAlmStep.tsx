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

import { ToggleButton, TutorialStep } from '~design-system';
import { hasMessage, translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';

export interface SelectAlmStepProps {
  alm?: AlmKeys;
  onChange: (value: AlmKeys) => void;
}

function getAlmLongName(alm: AlmKeys) {
  return hasMessage('alm', alm, 'long') ? translate('alm', alm, 'long') : translate('alm', alm);
}

export default function SelectAlmStep(props: SelectAlmStepProps) {
  const { alm } = props;
  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.alm_selection.title')}>
      <ToggleButton
        label={translate('onboarding.tutorial.with.jenkins.alm_selection.title')}
        onChange={props.onChange}
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
    </TutorialStep>
  );
}
