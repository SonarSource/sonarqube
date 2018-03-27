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
import { Language } from '@sqcore/api/languages';
import MeasureBugs from './measures/MeasureBugs';
import MeasureCodeSmells from './measures/MeasureCodeSmells';
import MeasureCoverage from './measures/MeasureCoverage';
import MeasureDuplication from './measures/MeasureDuplication';
import MeasureLanguages from './measures/MeasureLanguages';
import MeasureVulnerabilities from './measures/MeasureVulnerabilities';
import { ProjectData } from '../types';

interface Props extends Pick<ProjectData, 'measures'> {
  languages: Language[];
}

export default function ProjectCardMeasures({ languages, measures }: Props) {
  return (
    <div className="project-card-measures">
      <div className="project-card-measures-left">
        <MeasureBugs measures={measures} />
        <i className="project-card-measure-separator" />
        <MeasureVulnerabilities measures={measures} />
        <i className="project-card-measure-separator" />
        <MeasureCodeSmells measures={measures} />
        <i className="project-card-measure-separator hidden-small" />
      </div>
      <div className="project-card-measures-right">
        <MeasureCoverage measures={measures} />
        <i className="project-card-measure-separator" />
        <MeasureDuplication measures={measures} />
        <i className="project-card-measure-separator hidden-big" />
        <MeasureLanguages languages={languages} measures={measures} />
      </div>
    </div>
  );
}
