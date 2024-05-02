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
import { RadioButtonGroup } from '@sonarsource/echoes-react';
import { ButtonPrimary, FlagMessage, Modal } from 'design-system';
import React, { useState } from 'react';
import { Visibility } from '~sonar-aligned/types/component';
import { translate } from '../../helpers/l10n';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';

export interface Props {
  defaultVisibility: Visibility;
  onClose: () => void;
  onConfirm: (visiblity: Visibility) => void;
}

const FORM_ID = 'change-default-visibility-form';

export default function ChangeDefaultVisibilityForm(props: Readonly<Props>) {
  const [visibility, setVisibility] = useState(props.defaultVisibility);
  const { data: githubProbivisioningEnabled } = useGithubProvisioningEnabledQuery();

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
        id="settings-projects-visibility-radio"
        options={Object.values(Visibility).map((visibilityValue) => ({
          label: translate('visibility', visibilityValue),
          helpText: translate('visibility', visibilityValue, 'description.short'),
          value: visibilityValue,
        }))}
        value={visibility}
        onChange={handleVisibilityChange}
      />

      <FlagMessage variant="warning">
        {translate(
          `settings.projects.change_visibility_form.warning${
            githubProbivisioningEnabled ? '.github' : ''
          }`,
        )}
      </FlagMessage>
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
        <ButtonPrimary form={FORM_ID} autoFocus type="submit">
          {translate('settings.projects.change_visibility_form.submit')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
