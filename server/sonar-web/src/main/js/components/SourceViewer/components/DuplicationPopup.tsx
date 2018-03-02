/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import { groupBy, sortBy } from 'lodash';
import { SourceViewerFile, DuplicationBlock, DuplicatedFile } from '../../../app/types';
import BubblePopup from '../../common/BubblePopup';
import QualifierIcon from '../../shared/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../../helpers/path';
import { getProjectUrl } from '../../../helpers/urls';

interface Props {
  blocks: DuplicationBlock[];
  // TODO use branchLike
  branch: string | undefined;
  duplicatedFiles?: { [ref: string]: DuplicatedFile };
  inRemovedComponent: boolean;
  onClose: () => void;
  popupPosition?: any;
  sourceViewerFile: SourceViewerFile;
}

export default class DuplicationPopup extends React.PureComponent<Props> {
  isDifferentComponent = (
    a: { project: string; subProject?: string },
    b: { project: string; subProject?: string }
  ) => {
    return Boolean(a && b && (a.project !== b.project || a.subProject !== b.subProject));
  };

  handleFileClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const Workspace = require('../../workspace/main').default;
    const { key, line } = event.currentTarget.dataset;
    Workspace.openComponent({ key, line, branch: this.props.branch });
    this.props.onClose();
  };

  render() {
    const { duplicatedFiles = {}, sourceViewerFile } = this.props;

    const groupedBlocks = groupBy(this.props.blocks, '_ref');
    let duplications = Object.keys(groupedBlocks).map(fileRef => {
      return {
        blocks: groupedBlocks[fileRef],
        file: duplicatedFiles[fileRef]
      };
    });

    // first duplications in the same file
    // then duplications in the same sub-project
    // then duplications in the same project
    // then duplications in other projects
    duplications = sortBy(
      duplications,
      d => d.file.projectName !== sourceViewerFile.projectName,
      d => d.file.subProjectName !== sourceViewerFile.subProjectName,
      d => d.file.key !== sourceViewerFile.key
    );

    return (
      <BubblePopup customClass="source-viewer-bubble-popup" position={this.props.popupPosition}>
        <div className="bubble-popup-container">
          {this.props.inRemovedComponent && (
            <div className="alert alert-warning">
              {translate('duplications.dups_found_on_deleted_resource')}
            </div>
          )}
          {duplications.length > 0 && (
            <>
              <div className="bubble-popup-title">
                {translate('component_viewer.transition.duplication')}
              </div>
              {duplications.map(duplication => (
                <div className="bubble-popup-section" key={duplication.file.key}>
                  <div className="component-name">
                    {this.isDifferentComponent(duplication.file, this.props.sourceViewerFile) && (
                      <>
                        <div className="component-name-parent">
                          <QualifierIcon className="little-spacer-right" qualifier="TRK" />
                          <Link to={getProjectUrl(duplication.file.project)}>
                            {duplication.file.projectName}
                          </Link>
                        </div>
                        {duplication.file.subProject &&
                          duplication.file.subProjectName && (
                            <div className="component-name-parent">
                              <QualifierIcon className="little-spacer-right" qualifier="BRC" />
                              <Link to={getProjectUrl(duplication.file.subProject)}>
                                {duplication.file.subProjectName}
                              </Link>
                            </div>
                          )}
                      </>
                    )}

                    {duplication.file.key !== this.props.sourceViewerFile.key && (
                      <div className="component-name-path">
                        <a
                          className="link-action"
                          data-key={duplication.file.key}
                          href="#"
                          onClick={this.handleFileClick}
                          title={duplication.file.name}>
                          <span>{collapsedDirFromPath(duplication.file.name)}</span>
                          <span className="component-name-file">
                            {fileFromPath(duplication.file.name)}
                          </span>
                        </a>
                      </div>
                    )}

                    <div className="component-name-path">
                      {'Lines: '}
                      {duplication.blocks.map((block, index) => (
                        <React.Fragment key={index}>
                          <a
                            data-key={duplication.file.key}
                            data-line={block.from}
                            href="#"
                            onClick={this.handleFileClick}>
                            {block.from}
                            {' – '}
                            {block.from + block.size - 1}
                          </a>
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
      </BubblePopup>
    );
  }
}
