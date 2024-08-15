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
import { Button, ButtonVariety, RadioButtonGroup } from '@sonarsource/echoes-react';
import { FlagMessage, Modal } from 'design-system';
import React, { useState } from 'react';
import { Visibility } from '~sonar-aligned/types/component';
import { translate } from '../../helpers/l10n';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';
import { useGilabProvisioningEnabledQuery } from '../../queries/identity-provider/gitlab';

export interface Props {
  defaultVisibility: Visibility;
  onClose: () => void;
  onConfirm: (visiblity: Visibility) => void;
}

const FORM_ID = 'change-default-visibility-form';

export default function ChangeDefaultVisibilityForm(props: Readonly<Props>) {
  const [visibility, setVisibility] = useState(props.defaultVisibility);
  const { data: githubProbivisioningEnabled } = useGithubProvisioningEnabledQuery();
  const { data: gitlabProbivisioningEnabled } = useGilabProvisioningEnabledQuery();

  let changeVisibilityTranslationKey = 'settings.projects.change_visibility_form.warning';
  if (githubProbivisioningEnabled) {
    changeVisibilityTranslationKey += '.github';
  } else if (gitlabProbivisioningEnabled) {
    changeVisibilityTranslationKey += '.gitlab';
  }

  const handleConfirmClick = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    props.onConfirm(visibility);
    props.onClose();
  };

  const handleVisibilityChange = (visibility: Visibility) => {
    setVisibility(visibility);
  };

  const header = translate('settings.projects.change_visibility_form.header');

  const body = (
    <form id={FORM_ID} onSubmit={handleConfirmClick}>
      <RadioButtonGroup
        ariaLabel={header}
        id="settings-projects-visibility-radio"
        options={Object.values(Visibility).map((visibilityValue) => ({
          label: translate('visibility', visibilityValue),
          helpText: translate('visibility', visibilityValue, 'description.short'),
          value: visibilityValue,
        }))}
        value={visibility}
        onChange={handleVisibilityChange}
      />

      <FlagMessage variant="warning">{translate(changeVisibilityTranslationKey)}</FlagMessage>
    </form>
  );

  return (
    <Modal
      isScrollable={false}
      isOverflowVisible
      headerTitle={header}
      onClose={props.onClose}
      body={body}
      primaryButton={
        <Button form={FORM_ID} hasAutoFocus type="submit" variety={ButtonVariety.Primary}>
          {translate('settings.projects.change_visibility_form.submit')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
