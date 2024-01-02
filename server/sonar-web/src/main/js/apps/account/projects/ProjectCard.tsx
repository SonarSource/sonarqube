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
import Link from '../../../components/common/Link';
import MetaLink from '../../../components/common/MetaLink';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import DateFromNow from '../../../components/intl/DateFromNow';
import Level from '../../../components/ui/Level';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { orderLinks } from '../../../helpers/projectLinks';
import { getProjectUrl } from '../../../helpers/urls';
import { MyProject, ProjectLink } from '../../../types/types';

interface Props {
  project: MyProject;
}

export default function ProjectCard({ project }: Props) {
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

  return (
    <div className="account-project-card clearfix">
      <aside className="account-project-side">
        {lastAnalysisDate !== undefined ? (
          <div className="account-project-analysis">
            <DateFromNow date={lastAnalysisDate}>
              {(fromNow) => translateWithParameters('my_account.projects.analyzed_x', fromNow)}
            </DateFromNow>
          </div>
        ) : (
          <div className="account-project-analysis">
            {translate('my_account.projects.never_analyzed')}
          </div>
        )}

        {project.qualityGate !== undefined && (
          <div className="account-project-quality-gate">
            {project.qualityGate === 'WARN' && (
              <HelpTooltip
                className="little-spacer-right"
                overlay={translate('quality_gates.conditions.warning.tooltip')}
              />
            )}
            <Level aria-label={translate('quality_gates.status')} level={project.qualityGate} />
          </div>
        )}
      </aside>

      <h3 className="account-project-name">
        <Link to={getProjectUrl(project.key)}>{project.name}</Link>
      </h3>

      {orderedLinks.length > 0 && (
        <div className="account-project-links">
          <ul className="list-inline">
            {orderedLinks.map((link) => (
              <MetaLink iconOnly key={link.id} link={link} />
            ))}
          </ul>
        </div>
      )}

      <div className="account-project-key">{project.key}</div>

      {!!project.description && (
        <div className="account-project-description">{project.description}</div>
      )}
    </div>
  );
}
