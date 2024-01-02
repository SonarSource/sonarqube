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
import * as React from 'react';
import { ButtonIcon } from '../../components/controls/buttons';
import { ClipboardIconButton } from '../../components/controls/clipboard';
import Dropdown from '../../components/controls/Dropdown';
import ListIcon from '../../components/icons/ListIcon';
import QualifierIcon from '../../components/icons/QualifierIcon';
import { PopupPlacement } from '../../components/ui/popups';
import { getBranchLikeQuery } from '../../helpers/branch-like';
import { ISSUE_TYPES } from '../../helpers/constants';
import { ISSUETYPE_METRIC_KEYS_MAP } from '../../helpers/issues';
import { translate } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { omitNil } from '../../helpers/request';
import { getBaseUrl } from '../../helpers/system';
import {
  getBranchLikeUrl,
  getCodeUrl,
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
  getPathUrlAsString,
} from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier } from '../../types/component';
import { IssueType } from '../../types/issues';
import { Measure, SourceViewerFile } from '../../types/types';
import Link from '../common/Link';
import { WorkspaceContextShape } from '../workspace/context';
import MeasuresOverlay from './components/MeasuresOverlay';

interface Props {
  branchLike: BranchLike | undefined;
  componentMeasures?: Measure[];
  openComponent: WorkspaceContextShape['openComponent'];
  showMeasures?: boolean;
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
    this.props.openComponent({ branchLike: this.props.branchLike, key });
  };

  renderIssueMeasures = () => {
    const { branchLike, componentMeasures, sourceViewerFile } = this.props;
    return (
      componentMeasures &&
      componentMeasures.length > 0 && (
        <>
          <div className="source-viewer-header-measure-separator" />

          {ISSUE_TYPES.map((type: IssueType) => {
            const params = {
              ...getBranchLikeQuery(branchLike),
              files: sourceViewerFile.path,
              resolved: 'false',
              types: type,
            };

            const measure = componentMeasures.find(
              (m) => m.metric === ISSUETYPE_METRIC_KEYS_MAP[type].metric
            );

            const linkUrl =
              type === IssueType.SecurityHotspot
                ? getComponentSecurityHotspotsUrl(sourceViewerFile.project, params)
                : getComponentIssuesUrl(sourceViewerFile.project, params);

            return (
              <div className="source-viewer-header-measure" key={type}>
                <span className="source-viewer-header-measure-label">
                  {translate('issue.type', type)}
                </span>
                <span className="source-viewer-header-measure-value">
                  <Link to={linkUrl}>{formatMeasure((measure && measure.value) || 0, 'INT')}</Link>
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
    const { key, measures, path, project, projectName, q } = this.props.sourceViewerFile;
    const unitTestsOrLines = q === ComponentQualifier.TestFile ? 'tests' : 'lines';
    const workspace = false;
    const query = new URLSearchParams(
      omitNil({ key, ...getBranchLikeQuery(this.props.branchLike) })
    ).toString();
    const rawSourcesLink = `${getBaseUrl()}/api/sources/raw?${query}`;

    // TODO favorite
    return (
      <div className="source-viewer-header display-flex-center">
        <div className="flex-1 little-spacer-top">
          <div className="component-name">
            <div className="component-name-parent">
              <a
                className="link-no-underline"
                href={getPathUrlAsString(getBranchLikeUrl(project, this.props.branchLike))}
              >
                <QualifierIcon qualifier={ComponentQualifier.Project} /> <span>{projectName}</span>
              </a>
            </div>

            <div className="component-name-path">
              <QualifierIcon qualifier={q} /> <span>{collapsedDirFromPath(path)}</span>
              <span className="component-name-file">{fileFromPath(path)}</span>
              <span className="nudged-up spacer-left">
                <ClipboardIconButton
                  aria-label={translate('component_viewer.copy_path_to_clipboard')}
                  className="button-link link-no-underline"
                  copyValue={path}
                />
              </span>
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
                  to={getCodeUrl(this.props.sourceViewerFile.project, this.props.branchLike, key)}
                >
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
                  target="_blank"
                >
                  {translate('component_viewer.show_raw_source')}
                </a>
              </li>
            </ul>
          }
          overlayPlacement={PopupPlacement.BottomRight}
        >
          <ButtonIcon className="js-actions" aria-label={translate('component_viewer.action_menu')}>
            <ListIcon />
          </ButtonIcon>
        </Dropdown>
      </div>
    );
  }
}
