/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { useState } from 'react';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { useRenameMainBranchMutation } from '../../../queries/branch';
import { MainBranch } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branch: MainBranch;
  component: Component;
  onClose: () => void;
}

export default function RenameBranchModal(props: Props) {
  const { branch, component } = props;
  const [name, setName] = useState<string>();

  const { mutate: renameMainBranch, isLoading } = useRenameMainBranchMutation();

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!name) {
      return;
    }

    renameMainBranch({ component, name }, { onSuccess: props.onClose });
  };

  const handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    setName(event.currentTarget.value);
  };

  const header = translate('project_branch_pull_request.branch.rename');
  const submitDisabled = isLoading || !name || name === branch.name;

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose} size="small">
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <form onSubmit={handleSubmit}>
        <div className="modal-body">
          <MandatoryFieldsExplanation className="modal-field" />
          <div className="modal-field">
            <label htmlFor="rename-branch-name">
              {translate('new_name')}
              <MandatoryFieldMarker />
            </label>
            <input
              autoFocus
              id="rename-branch-name"
              maxLength={100}
              name="name"
              onChange={handleNameChange}
              required
              size={50}
              type="text"
              value={name ?? branch.name}
            />
          </div>
        </div>
        <footer className="modal-foot">
          {isLoading && <i className="spinner spacer-right" />}
          <SubmitButton disabled={submitDisabled}>{translate('rename')}</SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </form>
    </Modal>
  );
}
