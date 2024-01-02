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
import styled from '@emotion/styled';
import {
  ClipboardIconButton,
  DrilldownLink,
  Dropdown,
  InteractiveIcon,
  ItemButton,
  ItemLink,
  Link,
  MenuIcon,
  Note,
  PopupPlacement,
  PopupZLevel,
  ProjectIcon,
  QualifierIcon,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
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
} from '../../helpers/urls';
import { DEFAULT_ISSUES_QUERY } from '../shared/utils';

import { ComponentQualifier } from '../../types/component';
import { IssueType } from '../../types/issues';
import { MetricKey, MetricType } from '../../types/metrics';

import type { BranchLike } from '../../types/branch-like';
import type { Measure, SourceViewerFile } from '../../types/types';
import type { WorkspaceContextShape } from '../workspace/context';

interface Props {
  branchLike: BranchLike | undefined;
  componentMeasures?: Measure[];
  openComponent: WorkspaceContextShape['openComponent'];
  showMeasures?: boolean;
  sourceViewerFile: SourceViewerFile;
}

export default class SourceViewerHeader extends React.PureComponent<Props> {
  openInWorkspace = () => {
    const { key } = this.props.sourceViewerFile;
    this.props.openComponent({ branchLike: this.props.branchLike, key });
  };

  renderIssueMeasures = () => {
    const { branchLike, componentMeasures, sourceViewerFile } = this.props;

    return (
      componentMeasures &&
      componentMeasures.length > 0 && (
        <>
          <StyledVerticalSeparator className="sw-h-8 sw-mx-6" />

          <div className="sw-flex sw-gap-6">
            {ISSUE_TYPES.map((type: IssueType) => {
              const params = {
                ...getBranchLikeQuery(branchLike),
                files: sourceViewerFile.path,
                ...DEFAULT_ISSUES_QUERY,
                types: type,
              };

              const measure = componentMeasures.find(
                (m) => m.metric === ISSUETYPE_METRIC_KEYS_MAP[type].metric,
              );

              const linkUrl =
                type === IssueType.SecurityHotspot
                  ? getComponentSecurityHotspotsUrl(sourceViewerFile.project, params)
                  : getComponentIssuesUrl(sourceViewerFile.project, params);

              return (
                <div className="sw-flex sw-flex-col sw-gap-1" key={type}>
                  <Note className="it__source-viewer-header-measure-label sw-body-lg">
                    {translate('issue.type', type)}
                  </Note>

                  <span>
                    <StyledDrilldownLink className="sw-body-md" to={linkUrl}>
                      {formatMeasure(measure?.value ?? 0, MetricType.Integer)}
                    </StyledDrilldownLink>
                  </span>
                </div>
              );
            })}
          </div>
        </>
      )
    );
  };

  render() {
    const { showMeasures } = this.props;
    const { key, measures, path, project, projectName, q } = this.props.sourceViewerFile;
    const unitTestsOrLines = q === ComponentQualifier.TestFile ? MetricKey.tests : MetricKey.lines;

    const query = new URLSearchParams(
      omitNil({ key, ...getBranchLikeQuery(this.props.branchLike) }),
    ).toString();

    const rawSourcesLink = `${getBaseUrl()}/api/sources/raw?${query}`;

    return (
      <StyledHeaderContainer
        className={
          'it__source-viewer-header sw-body-sm sw-flex sw-items-center sw-px-4 sw-py-3 ' +
          'sw-relative'
        }
      >
        <div className="sw-flex sw-flex-1 sw-flex-col sw-gap-1 sw-mr-5 sw-my-1">
          <div className="sw-flex sw-gap-1 sw-items-center">
            <Link icon={<ProjectIcon />} to={getBranchLikeUrl(project, this.props.branchLike)}>
              {projectName}
            </Link>
          </div>

          <div className="sw-flex sw-gap-1 sw-items-center">
            <QualifierIcon qualifier={q} />

            {collapsedDirFromPath(path)}

            {fileFromPath(path)}

            <span className="sw-ml-1">
              <ClipboardIconButton
                aria-label={translate('component_viewer.copy_path_to_clipboard')}
                copyValue={path}
              />
            </span>
          </div>
        </div>

        {showMeasures && (
          <div className="sw-flex sw-gap-6 sw-items-center">
            {measures[unitTestsOrLines] && (
              <div className="sw-flex sw-flex-col sw-gap-1">
                <Note className="it__source-viewer-header-measure-label sw-body-lg">
                  {translate(`metric.${unitTestsOrLines}.name`)}
                </Note>

                <span className="sw-body-lg">
                  {formatMeasure(measures[unitTestsOrLines], MetricType.ShortInteger)}
                </span>
              </div>
            )}

            {measures.coverage !== undefined && (
              <div className="sw-flex sw-flex-col sw-gap-1">
                <Note className="it__source-viewer-header-measure-label sw-body-lg">
                  {translate('metric.coverage.name')}
                </Note>

                <span className="sw-body-lg">
                  {formatMeasure(measures.coverage, MetricType.Percent)}
                </span>
              </div>
            )}

            {measures.duplicationDensity !== undefined && (
              <div className="sw-flex sw-flex-col sw-gap-1">
                <Note className="it__source-viewer-header-measure-label sw-body-lg">
                  {translate('duplications')}
                </Note>

                <span className="sw-body-lg">
                  {formatMeasure(measures.duplicationDensity, MetricType.Percent)}
                </span>
              </div>
            )}

            {this.renderIssueMeasures()}
          </div>
        )}

        <Dropdown
          id="source-viewer-header-actions"
          overlay={
            <>
              <ItemLink
                isExternal
                to={getCodeUrl(this.props.sourceViewerFile.project, this.props.branchLike, key)}
              >
                {translate('component_viewer.new_window')}
              </ItemLink>

              <ItemButton className="it__js-workspace" onClick={this.openInWorkspace}>
                {translate('component_viewer.open_in_workspace')}
              </ItemButton>

              <ItemLink isExternal to={rawSourcesLink}>
                {translate('component_viewer.show_raw_source')}
              </ItemLink>
            </>
          }
          placement={PopupPlacement.BottomRight}
          zLevel={PopupZLevel.Global}
        >
          <InteractiveIcon
            aria-label={translate('component_viewer.action_menu')}
            className="it__js-actions sw-flex-0 sw-ml-4 sw-px-3 sw-py-2"
            Icon={MenuIcon}
          />
        </Dropdown>
      </StyledHeaderContainer>
    );
  }
}

const StyledDrilldownLink = styled(DrilldownLink)`
  color: ${themeColor('linkDefault')};

  &:visited {
    color: ${themeColor('linkDefault')};
  }

  &:active,
  &:focus,
  &:hover {
    color: ${themeColor('linkActive')};
  }
`;

const StyledHeaderContainer = styled.div`
  background-color: ${themeColor('backgroundSecondary')};
  border-bottom: ${themeBorder('default', 'codeLineBorder')};
`;

const StyledVerticalSeparator = styled.div`
  border-right: ${themeBorder('default', 'codeLineBorder')};
`;
