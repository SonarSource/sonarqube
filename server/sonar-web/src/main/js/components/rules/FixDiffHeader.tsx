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

import {
  ChevronDownIcon,
  ChevronRightIcon,
  ClipboardIconButton,
  HoverLink,
  InteractiveIcon,
  LightLabel,
  PencilIcon,
} from '~design-system';
import { translate } from '../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { getBranchLikeUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { CreatePullRequestButton } from './CreatePullRequestButton';
import {
  BranchIcon,
  BranchSelect,
  BranchSelectButton,
  BranchText,
  FixDiffHeader as StyledFixDiffHeader,
} from './FixDiffStyles';

interface FixDiffHeaderProps {
  filePath: string;
  branchDisplayName: string;
  projectKey: string;
  projectName: string;
  branchLike?: BranchLike;
  jobId?: string;
  issueKey?: string;
}

export function FixDiffHeader({
  filePath,
  branchDisplayName,
  projectKey,
  projectName,
  branchLike,
  jobId,
  issueKey,
}: Readonly<FixDiffHeaderProps>) {
  return (
    <StyledFixDiffHeader>
      <div className="sw-flex-1 sw-flex sw-items-center sw-gap-2">
        <LightLabel>
          <HoverLink to={getBranchLikeUrl(projectKey, branchLike)} className="sw-mr-2">
            {projectName}
          </HoverLink>
        </LightLabel>
        <ChevronRightIcon className="sw-mr-2" />
        <LightLabel>
          {collapsedDirFromPath(filePath)}
          {fileFromPath(filePath)}
        </LightLabel>
        <ClipboardIconButton
          className="sw-h-6 sw-mx-2"
          copyValue={filePath}
          copyLabel={translate('source_viewer.click_to_copy_filepath')}
        />
      </div>
    <CreatePullRequestButton issueKey={issueKey} jobId={jobId} />
    </StyledFixDiffHeader>
  );
}

