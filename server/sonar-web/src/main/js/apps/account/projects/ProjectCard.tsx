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
import * as React from 'react';
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import Level from 'sonar-ui-common/components/ui/Level';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import MetaLink from '../../overview/meta/MetaLink';
import { orderLinks } from '../../projectLinks/utils';

interface Props {
  project: T.MyProject;
}

export default function ProjectCard({ project }: Props) {
  const { links } = project;

  const orderedLinks: T.ProjectLink[] = orderLinks(
    links.map((link, i) => {
      const { href, name, type } = link;
      return {
        id: `link-${i}`,
        name,
        type,
        url: href
      };
    })
  );

  const { lastAnalysisDate } = project;

  return (
    <div className="account-project-card clearfix">
      <aside className="account-project-side">
        {lastAnalysisDate !== undefined ? (
          <div className="account-project-analysis">
            <DateFromNow date={lastAnalysisDate}>
              {fromNow => (
                <Tooltip overlay={<DateTimeFormatter date={lastAnalysisDate} />}>
                  <span>{translateWithParameters('my_account.projects.analyzed_x', fromNow)}</span>
                </Tooltip>
              )}
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
            <Level level={project.qualityGate} />
          </div>
        )}
      </aside>

      <h3 className="account-project-name">
        <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>{project.name}</Link>
      </h3>

      {orderedLinks.length > 0 && (
        <div className="account-project-links">
          <ul className="list-inline">
            {orderedLinks.map(link => (
              <MetaLink iconOnly={true} key={link.id} link={link} />
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
