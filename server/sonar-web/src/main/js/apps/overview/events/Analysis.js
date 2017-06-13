/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import Events from '../../projectActivity/components/Events';
import FormattedDate from '../../../components/ui/FormattedDate';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { translate } from '../../../helpers/l10n';
import type { Analysis as AnalysisType } from '../../projectActivity/types';

export default function Analysis(props: { analysis: AnalysisType }) {
  const { analysis } = props;

  return (
    <TooltipsContainer>
      <li className="overview-analysis">
        <div className="small little-spacer-bottom">
          <strong>
            <FormattedDate date={analysis.date} format="LL" />
          </strong>
        </div>

        {analysis.events.length > 0
          ? <Events events={analysis.events} canAdmin={false} />
          : <span className="note">{translate('project_activity.project_analyzed')}</span>}
      </li>
    </TooltipsContainer>
  );
}
