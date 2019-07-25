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
import { stringify } from 'querystring';
import * as React from 'react';
import { Link } from 'react-router';
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import ListIcon from 'sonar-ui-common/components/icons/ListIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { collapsedDirFromPath, fileFromPath } from 'sonar-ui-common/helpers/path';
import { omitNil } from 'sonar-ui-common/helpers/request';
import { getBaseUrl, getPathUrlAsString } from 'sonar-ui-common/helpers/urls';
import { getBranchLikeQuery, isMainBranch } from '../../helpers/branches';
import { getBranchLikeUrl, getCodeUrl, getComponentIssuesUrl } from '../../helpers/urls';
import Favorite from '../controls/Favorite';
import { WorkspaceContextShape } from '../workspace/context';
import MeasuresOverlay from './components/MeasuresOverlay';

interface Props {
  branchLike: T.BranchLike | undefined;
  issues?: T.Issue[];
  openComponent: WorkspaceContextShape['openComponent'];
  showMeasures?: boolean;
  sourceViewerFile: T.SourceViewerFile;
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
    this.props.openComponent({ branchLike: this.props.branchLike, key });
  };

  renderIssueMeasures = () => {
    const { branchLike, issues, sourceViewerFile } = this.props;
    return (
      issues &&
      issues.length > 0 && (
        <>
          <div className="source-viewer-header-measure-separator" />

          {['BUG', 'VULNERABILITY', 'CODE_SMELL', 'SECURITY_HOTSPOT'].map((type: T.IssueType) => {
            const params = {
              ...getBranchLikeQuery(branchLike),
              fileUuids: sourceViewerFile.uuid,
              resolved: 'false',
              types: type
            };

            const total = issues.filter(issue => issue.type === type).length;
            return (
              <div className="source-viewer-header-measure" key={type}>
                <span className="source-viewer-header-measure-label">
                  {translate('issue.type', type)}
                </span>
                <span className="source-viewer-header-measure-value">
                  <Link to={getComponentIssuesUrl(sourceViewerFile.project, params)}>
                    {formatMeasure(total, 'INT')}
                  </Link>
                </span>
              </div>
            );
          })}
        </>
      )
    );
  };

  render() {
    const { showMeasures } = this.props;
    const {
      key,
      measures,
      path,
      project,
      projectName,
      q,
      subProject,
      subProjectName
    } = this.props.sourceViewerFile;
    const unitTestsOrLines = q === 'UTS' ? 'tests' : 'lines';
    const workspace = false;
    const rawSourcesLink =
      getBaseUrl() +
      '/api/sources/raw?' +
      stringify(omitNil({ key, ...getBranchLikeQuery(this.props.branchLike) }));

    // TODO favorite
    return (
      <div className="source-viewer-header display-flex-center">
        <div className="source-viewer-header-component flex-1">
          <div className="component-name">
            <div className="component-name-parent">
              <a
                className="link-with-icon"
                href={getPathUrlAsString(getBranchLikeUrl(project, this.props.branchLike))}>
                <QualifierIcon qualifier="TRK" /> <span>{projectName}</span>
              </a>
            </div>

            {subProject !== undefined && (
              <div className="component-name-parent">
                <QualifierIcon qualifier="BRC" /> <span>{subProjectName}</span>
              </div>
            )}

            <div className="component-name-path">
              <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
              <span className="component-name-file">{fileFromPath(path)}</span>
              <span className="nudged-up spacer-left">
                <ClipboardIconButton className="button-link link-no-underline" copyValue={path} />
              </span>
              {this.props.sourceViewerFile.canMarkAsFavorite &&
                (!this.props.branchLike || isMainBranch(this.props.branchLike)) && (
                  <Favorite
                    className="component-name-favorite spacer-left"
                    component={key}
                    favorite={this.props.sourceViewerFile.fav || false}
                    qualifier={this.props.sourceViewerFile.q}
                  />
                )}
            </div>
          </div>
        </div>

        {this.state.measuresOverlay && (
          <MeasuresOverlay
            branchLike={this.props.branchLike}
            onClose={this.handleMeasuresOverlayClose}
            sourceViewerFile={this.props.sourceViewerFile}
          />
        )}

        {showMeasures && (
          <div className="display-flex-center">
            {measures[unitTestsOrLines] && (
              <div className="source-viewer-header-measure">
                <span className="source-viewer-header-measure-label">
                  {translate(`metric.${unitTestsOrLines}.name`)}
                </span>
                <span className="source-viewer-header-measure-value">
                  {formatMeasure(measures[unitTestsOrLines], 'SHORT_INT')}
                </span>
              </div>
            )}

            {measures.coverage !== undefined && (
              <div className="source-viewer-header-measure">
                <span className="source-viewer-header-measure-label">
                  {translate('metric.coverage.name')}
                </span>
                <span className="source-viewer-header-measure-value">
                  {formatMeasure(measures.coverage, 'PERCENT')}
                </span>
              </div>
            )}

            {measures.duplicationDensity !== undefined && (
              <div className="source-viewer-header-measure">
                <span className="source-viewer-header-measure-label">
                  {translate('duplications')}
                </span>
                <span className="source-viewer-header-measure-value">
                  {formatMeasure(measures.duplicationDensity, 'PERCENT')}
                </span>
              </div>
            )}

            {this.renderIssueMeasures()}
          </div>
        )}

        <Dropdown
          className="source-viewer-header-actions flex-0"
          overlay={
            <ul className="menu">
              <li>
                <a className="js-measures" href="#" onClick={this.handleShowMeasuresClick}>
                  {translate('component_viewer.show_details')}
                </a>
              </li>
              <li>
                <Link
                  className="js-new-window"
                  rel="noopener noreferrer"
                  target="_blank"
                  to={getCodeUrl(this.props.sourceViewerFile.project, this.props.branchLike, key)}>
                  {translate('component_viewer.new_window')}
                </Link>
              </li>
              {!workspace && (
                <li>
                  <a className="js-workspace" href="#" onClick={this.openInWorkspace}>
                    {translate('component_viewer.open_in_workspace')}
                  </a>
                </li>
              )}
              <li>
                <a
                  className="js-raw-source"
                  href={rawSourcesLink}
                  rel="noopener noreferrer"
                  target="_blank">
                  {translate('component_viewer.show_raw_source')}
                </a>
              </li>
            </ul>
          }
          overlayPlacement={PopupPlacement.BottomRight}>
          <ButtonIcon className="js-actions">
            <ListIcon />
          </ButtonIcon>
        </Dropdown>
      </div>
    );
  }
}
