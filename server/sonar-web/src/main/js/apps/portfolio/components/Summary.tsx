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
import { Link } from 'react-router';
import LanguageDistributionContainer from '../../../components/charts/LanguageDistributionContainer';
import Measure from '../../../components/measure/Measure';
import { translate } from '../../../helpers/l10n';
import { getComponentDrilldownUrl } from '../../../helpers/urls';

interface Props {
  component: { description?: string; key: string };
  measures: { [key: string]: string | undefined };
}

export default function Summary({ component, measures }: Props) {
  const { projects, ncloc } = measures;
  const nclocDistribution = measures['ncloc_language_distribution'];

  return (
    <section id="portfolio-summary" className="big-spacer-bottom">
      {component.description && <div className="big-spacer-bottom">{component.description}</div>}

      <ul className="portfolio-grid">
        <li>
          <div className="portfolio-measure-secondary-value">
            <Link to={getComponentDrilldownUrl(component.key, 'projects')}>
              <Measure metricKey="projects" metricType="SHORT_INT" value={projects} />
            </Link>
          </div>
          <div className="spacer-top text-muted">{translate('projects')}</div>
        </li>
        <li>
          <div className="portfolio-measure-secondary-value">
            <Link to={getComponentDrilldownUrl(component.key, 'ncloc')}>
              <Measure metricKey="ncloc" metricType="SHORT_INT" value={ncloc} />
            </Link>
          </div>
          <div className="spacer-top text-muted">{translate('metric.ncloc.name')}</div>
        </li>
      </ul>

      {nclocDistribution && (
        <div className="big-spacer-top">
          <LanguageDistributionContainer distribution={nclocDistribution} width={260} />
        </div>
      )}
    </section>
  );
}
