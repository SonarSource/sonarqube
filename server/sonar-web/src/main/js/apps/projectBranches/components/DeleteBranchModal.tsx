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
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { getBranchLikeDisplayName, isPullRequest } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useDeletBranchMutation } from '../../../queries/branch';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branchLike: BranchLike;
  component: Component;
  onClose: () => void;
}

export default function DeleteBranchModal(props: Props) {
  const { branchLike, component } = props;
  const { mutate: deleteBranch, isLoading } = useDeletBranchMutation();

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    deleteBranch(
      { component, branchLike },
      {
        onSuccess: props.onClose,
      }
    );
  };

  const header = translate(
    isPullRequest(branchLike)
      ? 'project_branch_pull_request.pull_request.delete'
      : 'project_branch_pull_request.branch.delete'
  );

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <form onSubmit={handleSubmit}>
        <div className="modal-body">
          {translateWithParameters(
            isPullRequest(branchLike)
              ? 'project_branch_pull_request.pull_request.delete.are_you_sure'
              : 'project_branch_pull_request.branch.delete.are_you_sure',
            getBranchLikeDisplayName(branchLike)
          )}
        </div>
        <footer className="modal-foot">
          {isLoading && <i className="spinner spacer-right" />}
          <SubmitButton className="button-red" disabled={isLoading}>
            {translate('delete')}
          </SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </form>
    </Modal>
  );
}
