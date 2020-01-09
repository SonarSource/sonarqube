/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import ExpandSnippetIcon from 'sonar-ui-common/components/icons/ExpandSnippetIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from 'sonar-ui-common/helpers/path';
import { getPathUrlAsString } from 'sonar-ui-common/helpers/urls';
import { getBranchLikeQuery, isMainBranch } from '../../helpers/branch-like';
import { getBranchLikeUrl, getComponentIssuesUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier } from '../../types/component';
import Favorite from '../controls/Favorite';
import './SourceViewerHeaderSlim.css';

export interface Props {
  branchLike: BranchLike | undefined;
  expandable?: boolean;
  linkToProject?: boolean;
  loading?: boolean;
  onExpand?: () => void;
  sourceViewerFile: T.SourceViewerFile;
}

export default function SourceViewerHeaderSlim({
  branchLike,
  expandable,
  linkToProject = true,
  loading,
  onExpand,
  sourceViewerFile
}: Props) {
  const {
    key,
    measures,
    path,
    project,
    projectName,
    q,
    subProject,
    subProjectName
  } = sourceViewerFile;

  const projectNameLabel = (
    <>
      <QualifierIcon qualifier={ComponentQualifier.Project} /> <span>{projectName}</span>
    </>
  );

  return (
    <div className="source-viewer-header-slim display-flex-row display-flex-space-between">
      <div className="display-flex-center flex-1">
        <div>
          {linkToProject ? (
            <a
              className="link-with-icon"
              href={getPathUrlAsString(getBranchLikeUrl(project, branchLike))}>
              {projectNameLabel}
            </a>
          ) : (
            projectNameLabel
          )}
        </div>

        {subProject !== undefined && (
          <>
            <QualifierIcon qualifier={ComponentQualifier.SubProject} />{' '}
            <span>{subProjectName}</span>
          </>
        )}

        <div className="spacer-left">
          <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
          <span className="component-name-file">{fileFromPath(path)}</span>
        </div>

        <div className="spacer-left">
          <ClipboardIconButton className="button-link link-no-underline" copyValue={path} />
        </div>

        {sourceViewerFile.canMarkAsFavorite && (!branchLike || isMainBranch(branchLike)) && (
          <div className="nudged-up spacer-left">
            <Favorite
              className="component-name-favorite"
              component={key}
              favorite={sourceViewerFile.fav || false}
              qualifier={sourceViewerFile.q}
            />
          </div>
        )}
      </div>

      {measures.issues !== undefined && (
        <div
          className={classNames('flex-0 big-spacer-left', {
            'little-spacer-right': !expandable || loading
          })}>
          <Link
            to={getComponentIssuesUrl(project, {
              ...getBranchLikeQuery(branchLike),
              fileUuids: sourceViewerFile.uuid,
              resolved: 'false'
            })}>
            {translate('source_viewer.view_all_issues')}
          </Link>
        </div>
      )}

      {expandable && (
        <DeferredSpinner className="little-spacer-right" loading={loading}>
          <div className="flex-0 big-spacer-left">
            <ButtonIcon className="js-actions" onClick={onExpand}>
              <ExpandSnippetIcon />
            </ButtonIcon>
          </div>
        </DeferredSpinner>
      )}
    </div>
  );
}
