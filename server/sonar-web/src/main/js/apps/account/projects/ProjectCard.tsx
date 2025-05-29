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
  Card,
  DiscreetLink,
  LightPrimary,
  Note,
  QualityGateIndicator,
  SubHeading,
  UnorderedList,
} from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { Status } from '~sonar-aligned/types/common';
import { MetricType } from '~sonar-aligned/types/metrics';
import MetaLink from '../../../components/common/MetaLink';
import Tooltip from '../../../components/controls/Tooltip';
import DateFromNow from '../../../components/intl/DateFromNow';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { orderLinks } from '../../../helpers/projectLinks';
import { getProjectUrl } from '../../../helpers/urls';
import { MyProject, ProjectLink } from '../../../types/types';
import { useCurrentUser } from 'src/main/js/app/components/current-user/CurrentUserContext';

interface Props {
  project: MyProject;
}

export default function ProjectCard({ project }: Readonly<Props>) {
  const { links } = project;

  const orderedLinks: ProjectLink[] = orderLinks(
    links.map((link, i) => {
      const { href, name, type } = link;
      return {
        id: `link-${i}`,
        name,
        type,
        url: href,
      };
    }),
  );

  const { lastAnalysisDate } = project;

  const formatted = formatMeasure(project.qualityGate, MetricType.Level);
  const qualityGateLabel = translateWithParameters('overview.quality_gate_x', formatted);
  const {currentUser} = useCurrentUser();

  return (
    <Card>
      <aside className="sw-float-right sw-flex sw-flex-col sw-items-end sw-gap-2">
        {lastAnalysisDate !== undefined ? (
          <Note>
            <DateFromNow date={lastAnalysisDate}>
              {(fromNow) => translateWithParameters('my_account.projects.analyzed_x', fromNow)}
            </DateFromNow>
          </Note>
        ) : (
          <Note>{translate('my_account.projects.never_analyzed')}</Note>
        )}

        {project.qualityGate !== undefined && (
          <div>
            <Tooltip content={qualityGateLabel}>
              <span className="sw-flex sw-items-center">
                <QualityGateIndicator status={(project.qualityGate as Status) ?? 'NONE'} />
                <LightPrimary className="sw-ml-2 sw-typo-semibold">{formatted}</LightPrimary>
              </span>
            </Tooltip>
          </div>
        )}
      </aside>

      <SubHeading as="h3">
  {currentUser.isNotStandardOrg
    ? <>Key: {project.name}</>
    : <DiscreetLink to={getProjectUrl(project.key)}>Key: {project.name}</DiscreetLink>
  }
</SubHeading>

      <Note>{project.key}</Note>

      {!!project.description && <div className="sw-mt-2">{project.description}</div>}
      
      {!currentUser.isNotStandardOrg && orderedLinks.length > 0 && (
        <div className="sw-mt-2">
          <UnorderedList className="sw-flex sw-gap-4">
            {orderedLinks.map((link) => (
              <MetaLink key={link.id} link={link} />
            ))}
          </UnorderedList>
        </div>
      )}
    </Card>
  );
}
