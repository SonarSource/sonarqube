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
import { deleteBranch, deletePullRequest } from '../../../api/branches';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import { getBranchLikeDisplayName, isPullRequest } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branchLike: BranchLike;
  component: Component;
  onClose: () => void;
  onDelete: () => void;
}

interface State {
  loading: boolean;
}

export default class DeleteBranchModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    const request = isPullRequest(this.props.branchLike)
      ? deletePullRequest({
          project: this.props.component.key,
          pullRequest: this.props.branchLike.key,
        })
      : deleteBranch({
          branch: this.props.branchLike.name,
          project: this.props.component.key,
        });
    request.then(
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
          this.props.onDelete();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { branchLike } = this.props;
    const header = translate(
      isPullRequest(branchLike)
        ? (branchLike.isComparisonBranch
              ? 'project_branch_pull_request.comparison_branch.delete'
              : 'project_branch_pull_request.pull_request.delete')
        : 'project_branch_pull_request.branch.delete'
    );

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            {translateWithParameters(
              isPullRequest(branchLike)
                ? (branchLike.isComparisonBranch
                      ? 'project_branch_pull_request.comparison_branch.delete.are_you_sure'
                      : 'project_branch_pull_request.pull_request.delete.are_you_sure')
                : 'project_branch_pull_request.branch.delete.are_you_sure',
              getBranchLikeDisplayName(branchLike)
            )}
          </div>
          <footer className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <SubmitButton className="button-red" disabled={this.state.loading}>
              {translate('delete')}
            </SubmitButton>
            <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
