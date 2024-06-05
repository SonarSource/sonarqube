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
  DiscreetLink,
  DuplicationHighlight,
  FlagMessage,
  StandoutLink as Link,
  QualifierIcon,
} from 'design-system';
import { groupBy, sortBy } from 'lodash';
import React, { Fragment, PureComponent } from 'react';
import { isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { translate } from '../../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../../helpers/path';
import { getProjectUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { Dict, DuplicatedFile, DuplicationBlock, SourceViewerFile } from '../../../types/types';
import { WorkspaceContextShape } from '../../workspace/context';

interface Props {
  blocks: DuplicationBlock[];
  branchLike: BranchLike | undefined;
  duplicatedFiles?: Dict<DuplicatedFile>;
  duplicationHeader: string;
  inRemovedComponent: boolean;
  openComponent: WorkspaceContextShape['openComponent'];
  sourceViewerFile: SourceViewerFile;
}

export default class DuplicationPopup extends PureComponent<Props> {
  shouldLink() {
    const { branchLike } = this.props;
    return !isPullRequest(branchLike);
  }

  isDifferentComponent = (a: { project: string }, b: { project: string }) => {
    return Boolean(a && b && a.project !== b.project);
  };

  handleFileClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const { key, line } = event.currentTarget.dataset;
    if (this.shouldLink() && key) {
      this.props.openComponent({
        branchLike: this.props.branchLike,
        key,
        line: line ? Number(line) : undefined,
      });
    }
  };

  renderDuplication(file: DuplicatedFile, children: React.ReactNode, line?: number) {
    return this.shouldLink() ? (
      <DiscreetLink
        data-key={file.key}
        data-line={line}
        onClick={this.handleFileClick}
        title={file.name}
        to={{}}
      >
        {children}
      </DiscreetLink>
    ) : (
      children
    );
  }

  render() {
    const {
      duplicatedFiles = {},
      sourceViewerFile,
      duplicationHeader,
      inRemovedComponent,
    } = this.props;

    const groupedBlocks = groupBy(this.props.blocks, '_ref');
    let duplications = Object.keys(groupedBlocks).map((fileRef) => {
      return {
        blocks: groupedBlocks[fileRef],
        file: duplicatedFiles[fileRef],
      };
    });

    // first duplications in the same file
    // then duplications in the same project
    // then duplications in other projects
    duplications = sortBy(
      duplications,
      (d) => d.file.projectName !== sourceViewerFile.projectName,
      (d) => d.file.key !== sourceViewerFile.key,
    );

    return (
      <div className="sw-w-abs-400">
        {inRemovedComponent && (
          <FlagMessage variant="warning">
            {translate('duplications.dups_found_on_deleted_resource')}
          </FlagMessage>
        )}
        {duplications.length > 0 && (
          <>
            <DuplicationHighlight>{duplicationHeader}</DuplicationHighlight>
            {duplications.map((duplication) => (
              <div className="sw-my-2" key={duplication.file.key}>
                <div className="sw-flex sw-flex-wrap sw-body-sm">
                  {this.isDifferentComponent(duplication.file, this.props.sourceViewerFile) && (
                    <div className="sw-mr-4">
                      <QualifierIcon className="sw-mr-1" qualifier={ComponentQualifier.Project} />
                      <Link
                        to={getProjectUrl(duplication.file.project)}
                        title={duplication.file.projectName}
                      >
                        {duplication.file.projectName}
                      </Link>
                    </div>
                  )}

                  {duplication.file.key !== this.props.sourceViewerFile.key && (
                    <div className="sw-mr-2">
                      {this.renderDuplication(
                        duplication.file,
                        <span
                          title={
                            (collapsedDirFromPath(duplication.file.name) ?? '') +
                            (fileFromPath(duplication.file.name) ?? '')
                          }
                        >
                          <span>{collapsedDirFromPath(duplication.file.name)}</span>
                          <span>{fileFromPath(duplication.file.name)}</span>
                        </span>,
                      )}
                    </div>
                  )}

                  <div>
                    {'Lines: '}
                    {duplication.blocks.map((block, index) => (
                      <Fragment key={index}>
                        {this.renderDuplication(
                          duplication.file,
                          <>
                            {block.from}
                            {' – '}
                            {block.from + block.size - 1}
                          </>,
                          block.from,
                        )}
                        {index < duplication.blocks.length - 1 && ', '}
                      </Fragment>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </>
        )}
      </div>
    );
  }
}
