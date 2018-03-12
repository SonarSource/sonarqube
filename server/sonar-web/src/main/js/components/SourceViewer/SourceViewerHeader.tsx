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
import { stringify } from 'querystring';
import * as React from 'react';
import { Link } from 'react-router';
import MeasuresOverlay from './components/MeasuresOverlay';
import { SourceViewerFile, BranchLike } from '../../app/types';
import QualifierIcon from '../shared/QualifierIcon';
import FavoriteContainer from '../controls/FavoriteContainer';
import {
  getPathUrlAsString,
  getBranchLikeUrl,
  getComponentIssuesUrl,
  getBaseUrl
} from '../../helpers/urls';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { translate } from '../../helpers/l10n';
import { getBranchLikeQuery } from '../../helpers/branches';
import { formatMeasure } from '../../helpers/measures';
import { omitNil } from '../../helpers/request';

interface Props {
  branchLike: BranchLike | undefined;
  sourceViewerFile: SourceViewerFile;
}

interface State {
  measuresOverlay: boolean;
}

export default class SourceViewerHeader extends React.PureComponent<Props, State> {
  state: State = { measuresOverlay: false };

  handleShowMeasuresClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ measuresOverlay: true });
  };

  handleMeasuresOverlayClose = () => {
    this.setState({ measuresOverlay: false });
  };

  openInWorkspace = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    const { key } = this.props.sourceViewerFile;
    const Workspace = require('../workspace/main').default;
    Workspace.openComponent({ key, branchLike: this.props.branchLike });
  };

  render() {
    const {
      key,
      measures,
      path,
      project,
      projectName,
      q,
      subProject,
      subProjectName,
      uuid
    } = this.props.sourceViewerFile;
    const isUnitTest = q === 'UTS';
    const workspace = false;
    const rawSourcesLink =
      getBaseUrl() +
      '/api/sources/raw?' +
      stringify(omitNil({ key, ...getBranchLikeQuery(this.props.branchLike) }));

    // TODO favorite
    return (
      <div className="source-viewer-header">
        <div className="source-viewer-header-component">
          <div className="component-name">
            <div className="component-name-parent">
              <a
                className="link-with-icon"
                href={getPathUrlAsString(getBranchLikeUrl(project, this.props.branchLike))}>
                <QualifierIcon qualifier="TRK" /> <span>{projectName}</span>
              </a>
            </div>

            {subProject != null && (
              <div className="component-name-parent">
                <a
                  className="link-with-icon"
                  href={getPathUrlAsString(getBranchLikeUrl(subProject, this.props.branchLike))}>
                  <QualifierIcon qualifier="BRC" /> <span>{subProjectName}</span>
                </a>
              </div>
            )}

            <div className="component-name-path">
              <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
              <span className="component-name-file">{fileFromPath(path)}</span>
              {this.props.sourceViewerFile.canMarkAsFavorite && (
                <FavoriteContainer className="component-name-favorite" componentKey={key} />
              )}
            </div>
          </div>
        </div>

        <div className="dropdown source-viewer-header-actions">
          <a
            className="js-actions icon-list dropdown-toggle"
            data-toggle="dropdown"
            title={translate('component_viewer.more_actions')}
          />
          <ul className="dropdown-menu dropdown-menu-right">
            <li>
              <a className="js-measures" href="#" onClick={this.handleShowMeasuresClick}>
                {translate('component_viewer.show_details')}
              </a>
              {this.state.measuresOverlay && (
                <MeasuresOverlay
                  branchLike={this.props.branchLike}
                  onClose={this.handleMeasuresOverlayClose}
                  sourceViewerFile={this.props.sourceViewerFile}
                />
              )}
            </li>
            <li>
              <a
                className="js-new-window"
                href={getPathUrlAsString({
                  pathname: '/component',
                  query: { id: key, ...getBranchLikeQuery(this.props.branchLike) }
                })}
                target="_blank">
                {translate('component_viewer.new_window')}
              </a>
            </li>
            {!workspace && (
              <li>
                <a className="js-workspace" href="#" onClick={this.openInWorkspace}>
                  {translate('component_viewer.open_in_workspace')}
                </a>
              </li>
            )}
            <li>
              <a className="js-raw-source" href={rawSourcesLink} target="_blank">
                {translate('component_viewer.show_raw_source')}
              </a>
            </li>
          </ul>
        </div>

        <div className="source-viewer-header-measures">
          {isUnitTest && (
            <div className="source-viewer-header-measure">
              <span className="source-viewer-header-measure-value">
                {formatMeasure(measures.tests, 'SHORT_INT')}
              </span>
              <span className="source-viewer-header-measure-label">
                {translate('metric.tests.name')}
              </span>
            </div>
          )}

          {!isUnitTest && (
            <div className="source-viewer-header-measure">
              <span className="source-viewer-header-measure-value">
                {formatMeasure(measures.lines, 'SHORT_INT')}
              </span>
              <span className="source-viewer-header-measure-label">
                {translate('metric.lines.name')}
              </span>
            </div>
          )}

          <div className="source-viewer-header-measure">
            <span className="source-viewer-header-measure-value">
              <Link
                to={getComponentIssuesUrl(project, {
                  resolved: 'false',
                  fileUuids: uuid,
                  ...getBranchLikeQuery(this.props.branchLike)
                })}>
                {measures.issues != null ? formatMeasure(measures.issues, 'SHORT_INT') : 0}
              </Link>
            </span>
            <span className="source-viewer-header-measure-label">
              {translate('metric.violations.name')}
            </span>
          </div>

          {measures.coverage != null && (
            <div className="source-viewer-header-measure">
              <span className="source-viewer-header-measure-value">
                {formatMeasure(measures.coverage, 'PERCENT')}
              </span>
              <span className="source-viewer-header-measure-label">
                {translate('metric.coverage.name')}
              </span>
            </div>
          )}

          {measures.duplicationDensity != null && (
            <div className="source-viewer-header-measure">
              <span className="source-viewer-header-measure-value">
                {formatMeasure(measures.duplicationDensity, 'PERCENT')}
              </span>
              <span className="source-viewer-header-measure-label">
                {translate('duplications')}
              </span>
            </div>
          )}
        </div>
      </div>
    );
  }
}
