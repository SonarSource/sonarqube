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

import classNames from 'classnames';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { List, ListRowProps } from 'react-virtualized/dist/commonjs/List';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { MeasuresForProjects } from '../../../types/measures';
import { Query } from '../query';
import { Project } from '../types';
import ProjectCard from './project-card/ProjectCard';

const PROJECT_CARD_HEIGHT = 181;
const PROJECT_CARD_MARGIN = 20;
const PROJECT_LIST_FOOTER_HEIGHT = 90;

interface Props {
  cardType?: string;
  isFavorite: boolean;
  isFiltered: boolean;
  loadMore: () => void;
  loading: boolean;
  measures: MeasuresForProjects[];
  projects: Omit<Project, 'measures'>[];
  query: Query;
  total?: number;
}

export default function ProjectsList(props: Readonly<Props>) {
  const { cardType, measures, loading, projects, total, loadMore } = props;

  const renderRow = ({ index, key, style }: ListRowProps) => {
    if (index === projects.length) {
      return (
        <div key="footer" style={{ ...style }}>
          <ListFooter
            loadMoreAriaLabel={translate('projects.show_more')}
            count={projects !== undefined ? projects.length : 0}
            loadMore={loadMore}
            loading={loading}
            ready={!loading}
            total={total ?? 0}
          />
        </div>
      );
    }

    const project = projects[index];
    const componentMeasures =
      measures
        ?.filter((measure) => measure.component === project.key)
        .reduce(
          (acc, measure) => {
            const value = isDiffMetric(measure.metric) ? measure.period?.value : measure.value;
            if (value !== undefined) {
              acc[measure.metric] = value;
            }
            return acc;
          },
          {} as Record<string, string>,
        ) ?? {};

    return (
      <div
        className={classNames({ 'sw-mt-4': index === 0 })}
        key={key}
        role="row"
        style={{ ...style, height: PROJECT_CARD_HEIGHT }}
      >
        <div className="sw-h-full" role="gridcell">
          <ProjectCard
            key={project.key}
            project={{ ...project, measures: componentMeasures }}
            type={cardType}
          />
        </div>
      </div>
    );
  };

  return (
    <AutoSizer>
      {({ height, width }) => (
        <List
          aria-label={translate('project_plural')}
          height={height}
          overscanRowCount={2}
          rowCount={projects.length + 1}
          rowHeight={({ index }) => {
            if (index === 0) {
              // first card, double top and bottom margin
              return PROJECT_CARD_HEIGHT + PROJECT_CARD_MARGIN * 2;
            }
            if (index === projects.length) {
              // Footer card, no margin
              return PROJECT_LIST_FOOTER_HEIGHT;
            }
            // all other cards, only bottom margin
            return PROJECT_CARD_HEIGHT + PROJECT_CARD_MARGIN;
          }}
          rowRenderer={renderRow}
          style={{ outline: 'none' }}
          tabIndex={-1}
          width={width}
        />
      )}
    </AutoSizer>
  );
}
