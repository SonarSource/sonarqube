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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { useRenameMainBranchMutation } from '../../../queries/branch';
import { MainBranch } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branch: MainBranch;
  component: Component;
  onClose: () => void;
}

const FORM_ID = 'branch-rename-form';

export default function RenameBranchModal(props: Props) {
  const { branch, component, onClose } = props;
  const [name, setName] = useState<string>();

  const { mutate: renameMainBranch, isPending } = useRenameMainBranchMutation();

  const handleSubmit = React.useCallback(
    (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!name) {
        return;
      }

      renameMainBranch({ component, name }, { onSuccess: onClose });
    },
    [component, name, onClose, renameMainBranch],
  );

  const handleNameChange = React.useCallback((event: React.SyntheticEvent<HTMLInputElement>) => {
    setName(event.currentTarget.value);
  }, []);

  const header = translate('project_branch_pull_request.branch.rename');
  const submitDisabled = isPending || !name || name === branch.name;

  return (
    <Modal
      headerTitle={header}
      body={
        <form id={FORM_ID} onSubmit={handleSubmit}>
          <FormField className="sw-mb-1" label={<FormattedMessage id="new_name" />}>
            <InputField
              autoFocus
              id="rename-branch-name"
              maxLength={100}
              name="name"
              onChange={handleNameChange}
              required
              size="full"
              type="text"
              value={name ?? branch.name}
            />
          </FormField>
        </form>
      }
      loading={isPending}
      primaryButton={
        <Button
          isDisabled={submitDisabled}
          type="submit"
          form={FORM_ID}
          variety={ButtonVariety.Primary}
        >
          <FormattedMessage id="rename" />
        </Button>
      }
      secondaryButtonLabel={<FormattedMessage id="cancel" />}
      onClose={props.onClose}
    />
  );
}
