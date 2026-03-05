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
import { createCodefixPr } from '../../api/ai-codefix';
import { CreatePRButton, CreatePRIcon, CreatePRText } from './FixDiffStyles';

interface CreatePullRequestButtonProps {
  jobId?: string;
}

export function CreatePullRequestButton({ jobId }: Readonly<CreatePullRequestButtonProps>) {
  const [isActive, setIsActive] = React.useState(false);
  const [isDisabled, setIsDisabled] = React.useState(false);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const handleCreatePR = React.useCallback(() => {
    if (!jobId || isDisabled) return;
    setIsSubmitting(true);
    createCodefixPr(jobId)
      .then(() => {
        setIsActive(true);
        setIsDisabled(true);
      })
      .catch(() => {
        // Error already handled by request layer; keep button clickable for retry
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  }, [jobId, isDisabled]);

  const disabled = !jobId || isDisabled || isSubmitting;

  return (
    <CreatePRButton
      type="button"
      onClick={handleCreatePR}
      disabled={disabled}
      $active={isActive}
    >
      <CreatePRIcon>
           <img src="/images/pull-request-icon.svg" className="" alt="pull-request-icon" />
      </CreatePRIcon>
      <CreatePRText>Create Pull Request</CreatePRText>
    </CreatePRButton>
  );
}

