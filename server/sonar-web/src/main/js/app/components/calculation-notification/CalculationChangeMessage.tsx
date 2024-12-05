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

import { LinkHighlight } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DismissableAlert } from '../../../components/ui/DismissableAlert';
import { DocLink } from '../../../helpers/doc-links';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { Dict } from '../../../types/types';

const SHOW_MESSAGE_PATHS: Dict<ComponentQualifier> = {
  '/projects': ComponentQualifier.Project,
  '/portfolios': ComponentQualifier.Portfolio,
};

const ALERT_KEY = 'sonarqube.dismissed_calculation_change_alert';

export default function CalculationChangeMessage() {
  const location = useLocation();
  const { data: isStandardMode } = useStandardExperienceModeQuery();

  if (isStandardMode || !Object.keys(SHOW_MESSAGE_PATHS).includes(location.pathname)) {
    return null;
  }

  return (
    <DismissableAlert variant="info" alertKey={ALERT_KEY + SHOW_MESSAGE_PATHS[location.pathname]}>
      <FormattedMessage
        id="notification.calculation_change.message"
        values={{
          link: (text) => (
            <DocumentationLink
              shouldOpenInNewTab
              className="sw-ml-1"
              highlight={LinkHighlight.Default}
              to={DocLink.MetricDefinitions}
            >
              {text}
            </DocumentationLink>
          ),
        }}
      />
    </DismissableAlert>
  );
}
