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
import { ButtonPrimary, FlagMessage, Modal } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { translate } from '../../../helpers/l10n';
import { useSetMainBranchMutation } from '../../../queries/branch';
import { Branch } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface SetAsMainBranchModalProps {
  branch: Branch;
  component: Component;
  onClose: () => void;
  onSetAsMain: () => void;
}

const FORM_ID = 'branch-setasmain-form';

export default function SetAsMainBranchModal(props: SetAsMainBranchModalProps) {
  const { branch, component, onClose, onSetAsMain } = props;
  const { mutate: setMainBranch, isLoading } = useSetMainBranchMutation();

  const handleSubmit = React.useCallback(
    (e: React.SyntheticEvent<HTMLFormElement>) => {
      e.preventDefault();
      setMainBranch({ component, branchName: branch.name }, { onSuccess: onSetAsMain });
    },
    [branch, component, onSetAsMain, setMainBranch],
  );

  return (
    <Modal
      headerTitle={
        <FormattedMessage
          id="project_branch_pull_request.branch.set_x_as_main"
          values={{ branch: <span className="sw-break-all">{branch.name}</span> }}
        />
      }
      loading={isLoading}
      onClose={onClose}
      body={
        <form id={FORM_ID} onSubmit={handleSubmit}>
          <p className="sw-mb-4">
            <FormattedMessage
              id="project_branch_pull_request.branch.main_branch.are_you_sure"
              values={{ branch: <span className="sw-break-all">{branch.name}</span> }}
            />
          </p>
          <p className="sw-mb-4">
            <FormattedMessage
              id="project_branch_pull_request.branch.main_branch.learn_more"
              values={{
                documentation: (
                  <DocumentationLink to="/analyzing-source-code/branches/branch-analysis/#main-branch">
                    {translate('documentation')}
                  </DocumentationLink>
                ),
              }}
            />
          </p>
          <FlagMessage variant="warning">
            {translate('project_branch_pull_request.branch.main_branch.requires_reindex')}
          </FlagMessage>
        </form>
      }
      primaryButton={
        <ButtonPrimary disabled={isLoading} type="submit" form={FORM_ID}>
          <FormattedMessage id="project_branch_pull_request.branch.set_main" />
        </ButtonPrimary>
      }
      secondaryButtonLabel={<FormattedMessage id="cancel" />}
    />
  );
}
