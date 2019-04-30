/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DeferredSpinner from '../common/DeferredSpinner';
import Favorite from '../controls/Favorite';
import ExpandSnippetIcon from '../icons-components/ExpandSnippetIcon';
import QualifierIcon from '../icons-components/QualifierIcon';
import { ButtonIcon } from '../ui/buttons';
import { getPathUrlAsString, getBranchLikeUrl } from '../../helpers/urls';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { isMainBranch } from '../../helpers/branches';
import './SourceViewerHeaderSlim.css';

export interface Props {
  branchLike: T.BranchLike | undefined;
  expandable?: boolean;
  loading?: boolean;
  onExpand?: () => void;
  sourceViewerFile: T.SourceViewerFile;
}

export default function SourceViewerHeaderSlim({
  branchLike,
  expandable,
  loading,
  onExpand,
  sourceViewerFile
}: Props) {
  const { key, path, project, projectName, q, subProject, subProjectName } = sourceViewerFile;

  return (
    <div className="source-viewer-header-slim display-flex-row display-flex-space-between">
      <div className="display-flex-row flex-1">
        <div>
          <a
            className="link-with-icon"
            href={getPathUrlAsString(getBranchLikeUrl(project, branchLike))}>
            <QualifierIcon qualifier="TRK" /> <span>{projectName}</span>
          </a>
        </div>

        {subProject !== undefined && (
          <>
            <QualifierIcon qualifier="BRC" /> <span>{subProjectName}</span>
          </>
        )}

        <div className="spacer-left">
          <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
          <span className="component-name-file">{fileFromPath(path)}</span>
        </div>
        {sourceViewerFile.canMarkAsFavorite && (!branchLike || isMainBranch(branchLike)) && (
          <div className="nudged-up">
            <Favorite
              className="component-name-favorite"
              component={key}
              favorite={sourceViewerFile.fav || false}
              qualifier={sourceViewerFile.q}
            />
          </div>
        )}
      </div>

      {expandable && (
        <DeferredSpinner className="little-spacer-right" loading={loading}>
          <div className="source-viewer-header-slim-actions flex-0">
            <ButtonIcon className="js-actions" onClick={onExpand}>
              <ExpandSnippetIcon />
            </ButtonIcon>
          </div>
        </DeferredSpinner>
      )}
    </div>
  );
}
