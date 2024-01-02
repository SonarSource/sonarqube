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
import { groupBy, sortBy } from 'lodash';
import * as React from 'react';
import Link from '../../../components/common/Link';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { Alert } from '../../../components/ui/Alert';
import { isPullRequest } from '../../../helpers/branch-like';
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
  inRemovedComponent: boolean;
  openComponent: WorkspaceContextShape['openComponent'];
  sourceViewerFile: SourceViewerFile;
}

export default class DuplicationPopup extends React.PureComponent<Props> {
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
      <a
        data-key={file.key}
        data-line={line}
        href="#"
        onClick={this.handleFileClick}
        title={file.name}
      >
        {children}
      </a>
    ) : (
      children
    );
  }

  render() {
    const { duplicatedFiles = {}, sourceViewerFile } = this.props;

    const groupedBlocks = groupBy(this.props.blocks, '_ref');
    let duplications = Object.keys(groupedBlocks).map((fileRef) => {
      return {
        blocks: groupedBlocks[fileRef],
        file: duplicatedFiles[fileRef],
      };
    });

    // first duplications in the same file
    // then duplications in the same sub-project
    // then duplications in the same project
    // then duplications in other projects
    duplications = sortBy(
      duplications,
      (d) => d.file.projectName !== sourceViewerFile.projectName,
      (d) => d.file.key !== sourceViewerFile.key
    );

    return (
      <div className="source-viewer-bubble-popup abs-width-400">
        {this.props.inRemovedComponent && (
          <Alert variant="warning">
            {translate('duplications.dups_found_on_deleted_resource')}
          </Alert>
        )}
        {duplications.length > 0 && (
          <>
            <h6 className="spacer-bottom">
              {translate('component_viewer.transition.duplication')}
            </h6>
            {duplications.map((duplication) => (
              <div className="spacer-top text-ellipsis" key={duplication.file.key}>
                <div className="component-name">
                  {this.isDifferentComponent(duplication.file, this.props.sourceViewerFile) && (
                    <div className="component-name-parent">
                      <QualifierIcon className="little-spacer-right" qualifier="TRK" />
                      <Link to={getProjectUrl(duplication.file.project)}>
                        {duplication.file.projectName}
                      </Link>
                    </div>
                  )}

                  {duplication.file.key !== this.props.sourceViewerFile.key && (
                    <div className="component-name-path">
                      {this.renderDuplication(
                        duplication.file,
                        <>
                          <span>{collapsedDirFromPath(duplication.file.name)}</span>
                          <span className="component-name-file">
                            {fileFromPath(duplication.file.name)}
                          </span>
                        </>
                      )}
                    </div>
                  )}

                  <div className="component-name-path">
                    {'Lines: '}
                    {duplication.blocks.map((block, index) => (
                      <React.Fragment key={index}>
                        {this.renderDuplication(
                          duplication.file,
                          <>
                            {block.from}
                            {' – '}
                            {block.from + block.size - 1}
                          </>,
                          block.from
                        )}
                        {index < duplication.blocks.length - 1 && ', '}
                      </React.Fragment>
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
