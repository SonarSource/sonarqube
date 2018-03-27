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
import Tooltip from '@atlaskit/tooltip';
import DateTimeFormatter from '@sqcore/components/intl/DateTimeFormatter';
import Level from '@sqcore/components/ui/Level';
import SonarCloudIcon from './SonarCloudIcon';
import { ProjectData } from '../types';

interface Props {
  project: ProjectData;
}

export default function ProjectCardHeader({ project }: Props) {
  return (
    <div className="project-card-header">
      <div className="project-card-header-inner">
        <SonarCloudIcon size={24} />
        <Tooltip content={project.name}>
          <h4 className="spacer-left">
            <a href={'/dashboard?id=' + project.key} target="_blank">
              {project.name}
            </a>
          </h4>
        </Tooltip>
        {project.analysisDate && (
          <Level className="spacer-left" level={project.measures['alert_status']} small={true} />
        )}
      </div>
      {project.analysisDate && (
        <DateTimeFormatter date={project.analysisDate}>
          {formattedDate => (
            <small className="project-card-analysis-date">
              Last analysis:<br className="hidden-big" />
              <span className="little-spacer-left">{formattedDate}</span>
            </small>
          )}
        </DateTimeFormatter>
      )}
    </div>
  );
}
