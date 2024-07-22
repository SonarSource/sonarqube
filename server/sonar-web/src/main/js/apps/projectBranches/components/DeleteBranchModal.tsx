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
import { Modal } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { getBranchLikeDisplayName } from '../../../helpers/branch-like';
import { useDeletBranchMutation } from '../../../queries/branch';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branchLike: BranchLike;
  component: Component;
  onClose: () => void;
}

const FORM_ID = 'confirm-branch-delete-form';

export default function DeleteBranchModal(props: Props) {
  const { branchLike, component, onClose } = props;
  const { mutate: deleteBranch, isPending } = useDeletBranchMutation();

  const handleSubmit = React.useCallback(
    (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();
      deleteBranch(
        { component, branchLike },
        {
          onSuccess: onClose,
        },
      );
    },
    [deleteBranch, component, branchLike, onClose],
  );

  return (
    <Modal
      headerTitle={
        <FormattedMessage
          id={
            isPullRequest(branchLike)
              ? 'project_branch_pull_request.pull_request.delete'
              : 'project_branch_pull_request.branch.delete'
          }
        />
      }
      body={
        <form id={FORM_ID} onSubmit={handleSubmit}>
          <FormattedMessage
            id={
              isPullRequest(branchLike)
                ? 'project_branch_pull_request.pull_request.delete.are_you_sure'
                : 'project_branch_pull_request.branch.delete.are_you_sure'
            }
            values={{ name: getBranchLikeDisplayName(branchLike) }}
          />
        </form>
      }
      loading={isPending}
      primaryButton={
        <Button type="submit" form={FORM_ID} variety={ButtonVariety.Danger}>
          <FormattedMessage id="delete" />
        </Button>
      }
      secondaryButtonLabel={<FormattedMessage id="cancel" />}
      onClose={props.onClose}
    />
  );
}
