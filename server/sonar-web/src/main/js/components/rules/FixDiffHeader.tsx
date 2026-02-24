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
}

export function FixDiffHeader({
  filePath,
  branchDisplayName,
  projectKey,
  projectName,
  branchLike,
  jobId,
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
        <InteractiveIcon
          Icon={PencilIcon}
          aria-label={translate('edit')}
          onClick={() => {
            // TODO: Implement edit functionality
            console.log('Edit clicked');
          }}
          className="sw-mx-2"
        />
      </div>
      <BranchSelect>
        <BranchSelectButton>
          <BranchIcon>
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M8.62238 3.34801C8.39438 3.34801 8.20838 3.42001 8.06438 3.56401C7.92038 3.70801 7.84838 3.89101 7.84838 4.11301C7.84838 4.33501 7.92038 4.51801 8.06438 4.66201C8.20838 4.80601 8.39138 4.87801 8.61338 4.87801C8.83538 4.87801 9.02138 4.80301 9.17138 4.65301C9.32138 4.50301 9.39638 4.32301 9.39638 4.11301C9.39638 3.90301 9.32138 3.72301 9.17138 3.57301C9.02138 3.42301 8.83838 3.34801 8.62238 3.34801ZM6.30038 4.10401C6.30038 3.69601 6.41438 3.30301 6.64238 2.92501C6.87038 2.54701 7.17638 2.25601 7.56038 2.05201C7.92038 1.86001 8.32538 1.77901 8.77538 1.80901C9.22538 1.83901 9.61838 1.97401 9.95438 2.21401C10.2904 2.45401 10.5544 2.77801 10.7464 3.18601C10.9144 3.58201 10.9714 3.99601 10.9174 4.42801C10.8634 4.86001 10.6954 5.24401 10.4134 5.58001C10.1314 5.91601 9.79238 6.15601 9.39638 6.30001V11.7C9.91238 11.88 10.3204 12.216 10.6204 12.708C10.7524 12.948 10.8424 13.203 10.8904 13.473C10.9384 13.743 10.9384 14.01 10.8904 14.274C10.7944 14.826 10.5364 15.288 10.1164 15.66C9.66038 16.02 9.15638 16.2 8.60438 16.2C8.05238 16.2 7.56038 16.014 7.12838 15.642C6.69638 15.27 6.43238 14.814 6.33638 14.274C6.24038 13.698 6.33338 13.17 6.61538 12.69C6.89738 12.21 7.30838 11.88 7.84838 11.7V6.30001C7.39238 6.13201 7.02038 5.85001 6.73238 5.45401C6.44438 5.05801 6.30038 4.60801 6.30038 4.10401ZM8.62238 13.104C8.40638 13.104 8.22338 13.179 8.07338 13.329C7.92338 13.479 7.84838 13.665 7.84838 13.887C7.84838 14.109 7.92038 14.292 8.06438 14.436C8.20838 14.58 8.39138 14.652 8.61338 14.652C8.83538 14.652 9.02138 14.577 9.17138 14.427C9.32138 14.277 9.39638 14.097 9.39638 13.887C9.39638 13.677 9.31838 13.494 9.16238 13.338C9.00638 13.182 8.82638 13.104 8.62238 13.104Z"
                fill="#637192"
              />
            </svg>
          </BranchIcon>
          <BranchText>{branchDisplayName}</BranchText>
          <ChevronDownIcon className="sw-ml-1" />
        </BranchSelectButton>
      </BranchSelect>
      <CreatePullRequestButton jobId={jobId} />
    </StyledFixDiffHeader>
  );
}

