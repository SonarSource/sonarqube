/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import { ButtonIcon } from '../../components/controls/buttons';
import { ClipboardIconButton } from '../../components/controls/clipboard';
import ExpandSnippetIcon from '../../components/icons/ExpandSnippetIcon';
import QualifierIcon from '../../components/icons/QualifierIcon';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { getBranchLikeQuery } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { getBranchLikeUrl, getComponentIssuesUrl, getPathUrlAsString } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier } from '../../types/component';
import { SourceViewerFile } from '../../types/types';
import './SourceViewerHeaderSlim.css';

export interface Props {
  branchLike: BranchLike | undefined;
  expandable?: boolean;
  displayProjectName?: boolean;
  linkToProject?: boolean;
  loading?: boolean;
  onExpand?: () => void;
  sourceViewerFile: SourceViewerFile;
}

export default function SourceViewerHeaderSlim(props: Props) {
  const {
    branchLike,
    expandable,
    displayProjectName = true,
    linkToProject = true,
    loading,
    onExpand,
    sourceViewerFile
  } = props;
  const { measures, path, project, projectName, q } = sourceViewerFile;

  const projectNameLabel = (
    <>
      <QualifierIcon qualifier={ComponentQualifier.Project} /> <span>{projectName}</span>
    </>
  );

  const isProjectRoot = q === ComponentQualifier.Project;

  return (
    <div className="source-viewer-header-slim display-flex-row display-flex-space-between">
      <div className="display-flex-center flex-1">
        {displayProjectName && (
          <div className="spacer-right">
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
        )}

        {!isProjectRoot && (
          <>
            <div className="spacer-right">
              <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
              <span className="component-name-file">{fileFromPath(path)}</span>
            </div>

            <div className="spacer-right">
              <ClipboardIconButton className="button-link link-no-underline" copyValue={path} />
            </div>
          </>
        )}
      </div>

      {!isProjectRoot && measures.issues !== undefined && (
        <div
          className={classNames('flex-0 big-spacer-left', {
            'little-spacer-right': !expandable || loading
          })}>
          <Link
            to={getComponentIssuesUrl(project, {
              ...getBranchLikeQuery(branchLike),
              files: path,
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
