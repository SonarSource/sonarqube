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
import { LinkStandalone } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import {
  ClipboardIconButton,
  DrilldownLink,
  Dropdown,
  InteractiveIcon,
  ItemButton,
  ItemLink,
  MenuIcon,
  Note,
  PopupPlacement,
  PopupZLevel,
  ProjectIcon,
  QualifierIcon,
  themeBorder,
  themeColor,
} from '~design-system';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { SOFTWARE_QUALITIES } from '../../helpers/constants';
import {
  ISSUETYPE_METRIC_KEYS_MAP,
  SOFTWARE_QUALITIES_METRIC_KEYS_MAP,
  getIssueTypeBySoftwareQuality,
} from '../../helpers/issues';
import { areCCTMeasuresComputed as areCCTMeasuresComputedFn } from '../../helpers/measures';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';
import { omitNil } from '../../helpers/request';
import { getBaseUrl } from '../../helpers/system';
import { isDefined } from '../../helpers/types';
import { getBranchLikeUrl, getCodeUrl } from '../../helpers/urls';
import { useStandardExperienceMode } from '../../queries/settings';
import type { BranchLike } from '../../types/branch-like';
import { IssueType } from '../../types/issues';
import type { Measure, SourceViewerFile } from '../../types/types';
import { DEFAULT_ISSUES_QUERY } from '../shared/utils';
import type { WorkspaceContextShape } from '../workspace/context';

interface Props {
  branchLike: BranchLike | undefined;
  componentMeasures?: Measure[];
  hidePinOption?: boolean;
  openComponent: WorkspaceContextShape['openComponent'];
  showMeasures?: boolean;
  sourceViewerFile: SourceViewerFile;
}

export default function SourceViewerHeader(props: Readonly<Props>) {
  const intl = useIntl();
  const { data: isStandardMode = false } = useStandardExperienceMode();

  const { showMeasures, branchLike, hidePinOption, openComponent, componentMeasures } = props;
  const { key, measures, path, project, projectName, q } = props.sourceViewerFile;
  const unitTestsOrLines = q === ComponentQualifier.TestFile ? MetricKey.tests : MetricKey.lines;

  const query = new URLSearchParams(omitNil({ key, ...getBranchLikeQuery(branchLike) })).toString();

  const rawSourcesLink = `${getBaseUrl()}/api/sources/raw?${query}`;

  const renderIssueMeasures = () => {
    const areCCTMeasuresComputed = !isStandardMode && areCCTMeasuresComputedFn(componentMeasures);

    return (
      componentMeasures &&
      componentMeasures.length > 0 && (
        <>
          <StyledVerticalSeparator className="sw-h-8 sw-mx-6" />

          <div className="sw-flex sw-gap-6">
            {SOFTWARE_QUALITIES.map((quality) => {
              const { deprecatedMetric, metric } = SOFTWARE_QUALITIES_METRIC_KEYS_MAP[quality];
              const measure = componentMeasures.find(
                (m) => m.metric === (areCCTMeasuresComputed ? metric : deprecatedMetric),
              );
              const measureValue = areCCTMeasuresComputed
                ? JSON.parse(measure?.value ?? 'null').total
                : (measure?.value ?? 0);

              const linkUrl = getComponentIssuesUrl(project, {
                ...getBranchLikeQuery(branchLike),
                files: path,
                ...DEFAULT_ISSUES_QUERY,
                ...(areCCTMeasuresComputed
                  ? { impactSoftwareQualities: quality }
                  : { types: getIssueTypeBySoftwareQuality(quality) }),
              });

              const qualityTitle = intl.formatMessage({
                id: `metric.${isStandardMode ? deprecatedMetric : metric}.short_name`,
              });

              return (
                <div className="sw-flex sw-flex-col sw-gap-1" key={quality}>
                  <Note className="it__source-viewer-header-measure-label">{qualityTitle}</Note>

                  <span>
                    <StyledDrilldownLink
                      className="sw-typo-lg"
                      aria-label={intl.formatMessage(
                        { id: 'source_viewer.issue_link_x' },
                        {
                          count: formatMeasure(measureValue, MetricType.Integer),
                          quality: qualityTitle,
                        },
                      )}
                      to={linkUrl}
                    >
                      {formatMeasure(measureValue, MetricType.Integer)}
                    </StyledDrilldownLink>
                  </span>
                </div>
              );
            })}

            <div className="sw-flex sw-flex-col sw-gap-1" key={IssueType.SecurityHotspot}>
              <Note className="it__source-viewer-header-measure-label">
                {intl.formatMessage({ id: `issue.type.${IssueType.SecurityHotspot}` })}
              </Note>

              <span>
                <StyledDrilldownLink
                  className="sw-typo-lg"
                  to={getComponentSecurityHotspotsUrl(project, branchLike, {
                    files: path,
                    ...DEFAULT_ISSUES_QUERY,
                    types: IssueType.SecurityHotspot,
                  })}
                >
                  {formatMeasure(
                    componentMeasures.find(
                      (m) =>
                        m.metric === ISSUETYPE_METRIC_KEYS_MAP[IssueType.SecurityHotspot].metric,
                    )?.value ?? 0,
                    MetricType.Integer,
                  )}
                </StyledDrilldownLink>
              </span>
            </div>
          </div>
        </>
      )
    );
  };

  return (
    <StyledHeaderContainer
      className={
        'it__source-viewer-header sw-typo-default sw-flex sw-items-center sw-px-4 sw-py-3 ' +
        'sw-relative'
      }
    >
      <div className="sw-flex sw-flex-1 sw-flex-col sw-gap-1 sw-mr-5 sw-my-1">
        <div className="sw-flex sw-gap-1 sw-items-center">
          <LinkStandalone
            iconLeft={<ProjectIcon className="sw-mr-2" />}
            to={getBranchLikeUrl(project, branchLike)}
          >
            {projectName}
          </LinkStandalone>
        </div>

        <div className="sw-flex sw-gap-2 sw-items-center">
          <QualifierIcon qualifier={q} />

          {collapsedDirFromPath(path)}

          {fileFromPath(path)}

          <span>
            <ClipboardIconButton
              aria-label={intl.formatMessage({ id: 'component_viewer.copy_path_to_clipboard' })}
              copyValue={path}
            />
          </span>
        </div>
      </div>

      {showMeasures && (
        <div className="sw-flex sw-gap-6 sw-items-center">
          {isDefined(measures[unitTestsOrLines]) && (
            <div className="sw-flex sw-flex-col sw-gap-1">
              <Note className="it__source-viewer-header-measure-label">
                {intl.formatMessage({ id: `metric.${unitTestsOrLines}.name` })}
              </Note>

              <span>{formatMeasure(measures[unitTestsOrLines], MetricType.ShortInteger)}</span>
            </div>
          )}

          {isDefined(measures.coverage) && (
            <div className="sw-flex sw-flex-col sw-gap-1">
              <Note className="it__source-viewer-header-measure-label">
                {intl.formatMessage({ id: 'metric.coverage.name' })}
              </Note>

              <span>{formatMeasure(measures.coverage, MetricType.Percent)}</span>
            </div>
          )}

          {isDefined(measures.duplicationDensity) && (
            <div className="sw-flex sw-flex-col sw-gap-1">
              <Note className="it__source-viewer-header-measure-label">
                {intl.formatMessage({ id: 'duplications' })}
              </Note>

              <span>{formatMeasure(measures.duplicationDensity, MetricType.Percent)}</span>
            </div>
          )}

          {renderIssueMeasures()}
        </div>
      )}

      <Dropdown
        id="source-viewer-header-actions"
        overlay={
          <>
            <ItemLink isExternal to={getCodeUrl(project, branchLike, key)}>
              {intl.formatMessage({ id: 'component_viewer.new_window' })}
            </ItemLink>

            {!hidePinOption && (
              <ItemButton
                className="it__js-workspace"
                onClick={() => {
                  openComponent({ branchLike, key });
                }}
              >
                {intl.formatMessage({ id: 'component_viewer.open_in_workspace' })}
              </ItemButton>
            )}

            <ItemLink isExternal to={rawSourcesLink}>
              {intl.formatMessage({ id: 'component_viewer.show_raw_source' })}
            </ItemLink>
          </>
        }
        placement={PopupPlacement.BottomRight}
        zLevel={PopupZLevel.Global}
      >
        <InteractiveIcon
          aria-label={intl.formatMessage({ id: 'component_viewer.action_menu' })}
          className="it__js-actions sw-flex-0 sw-ml-4 sw-px-3 sw-py-2"
          Icon={MenuIcon}
        />
      </Dropdown>
    </StyledHeaderContainer>
  );
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
