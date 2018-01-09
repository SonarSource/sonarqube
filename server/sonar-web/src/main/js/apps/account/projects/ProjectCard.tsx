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
import { sortBy } from 'lodash';
import { Link } from 'react-router';
import { Project } from './types';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Level from '../../../components/ui/Level';
import Tooltip from '../../../components/controls/Tooltip';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  project: Project;
}

export default function ProjectCard({ project }: Props) {
  const isAnalyzed = project.lastAnalysisDate != null;
  const links = sortBy(project.links, 'type');

  return (
    <div className="account-project-card clearfix">
      <aside className="account-project-side">
        {isAnalyzed ? (
          <Tooltip
            overlay={<DateTimeFormatter date={project.lastAnalysisDate} />}
            placement="right">
            <div className="account-project-analysis">
              <DateFromNow date={project.lastAnalysisDate}>
                {(fromNow: string) => (
                  <span>{translateWithParameters('my_account.projects.analyzed_x', fromNow)}</span>
                )}
              </DateFromNow>
            </div>
          </Tooltip>
        ) : (
          <div className="account-project-analysis">
            {translate('my_account.projects.never_analyzed')}
          </div>
        )}

        {project.qualityGate != null && (
          <div className="account-project-quality-gate">
            <Level level={project.qualityGate} />
          </div>
        )}
      </aside>

      <h3 className="account-project-name">
        <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>{project.name}</Link>
      </h3>

      {links.length > 0 && (
        <div className="account-project-links">
          <ul className="list-inline">
            {links.map(link => (
              <li key={link.type}>
                <a
                  className="link-with-icon"
                  href={link.href}
                  title={link.name}
                  target="_blank"
                  rel="nofollow">
                  <i className={`icon-color-link icon-${link.type}`} />
                </a>
              </li>
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
