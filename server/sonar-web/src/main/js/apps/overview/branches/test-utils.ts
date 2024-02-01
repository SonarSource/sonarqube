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
import userEvent from '@testing-library/user-event';
import { byRole, byTestId, byText } from '../../../helpers/testSelector';
import {
  SoftwareImpactMeasureData,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';

export const getPageObjects = () => {
  const user = userEvent.setup();
  const selectors = {
    overallCodeButton: byRole('tab', { name: /overview.overall_code/ }),
    softwareImpactMeasureCard: (softwareQuality: SoftwareQuality) =>
      byTestId(`overview__software-impact-card-${softwareQuality}`),
  };
  const ui = {
    ...selectors,
    expectSoftwareImpactMeasureCard: (
      softwareQuality: SoftwareQuality,
      rating?: string,
      data?: SoftwareImpactMeasureData,
      severitiesActiveState?: boolean[],
    ) => {
      if (typeof rating === 'string') {
        expect(
          byText(rating, { exact: true }).get(ui.softwareImpactMeasureCard(softwareQuality).get()),
        ).toBeInTheDocument();
      }
      if (data) {
        expect(
          byRole('link', {
            name: `overview.measures.software_impact.see_list_of_x_open_issues.${data.total}.software_quality.${softwareQuality}`,
          }).get(),
        ).toBeInTheDocument();
        expect(
          byRole('link', {
            name: `overview.measures.software_impact.severity.see_x_open_issues.${
              data[SoftwareImpactSeverity.High]
            }.software_quality.${softwareQuality}.overview.measures.software_impact.severity.HIGH.tooltip`,
          }).get(),
        ).toBeInTheDocument();
        expect(
          byRole('link', {
            name: `overview.measures.software_impact.severity.see_x_open_issues.${
              data[SoftwareImpactSeverity.Medium]
            }.software_quality.${softwareQuality}.overview.measures.software_impact.severity.MEDIUM.tooltip`,
          }).get(),
        ).toBeInTheDocument();
        expect(
          byRole('link', {
            name: `overview.measures.software_impact.severity.see_x_open_issues.${
              data[SoftwareImpactSeverity.Low]
            }.software_quality.${softwareQuality}.overview.measures.software_impact.severity.LOW.tooltip`,
          }).get(),
        ).toBeInTheDocument();
      }
      if (severitiesActiveState) {
        ui.expectSoftwareImpactMeasureBreakdownCard(
          softwareQuality,
          SoftwareImpactSeverity.High,
          severitiesActiveState[0],
        );
        ui.expectSoftwareImpactMeasureBreakdownCard(
          softwareQuality,
          SoftwareImpactSeverity.Medium,
          severitiesActiveState[1],
        );
        ui.expectSoftwareImpactMeasureBreakdownCard(
          softwareQuality,
          SoftwareImpactSeverity.Low,
          severitiesActiveState[2],
        );
      }
    },
    expectSoftwareImpactMeasureBreakdownCard: (
      softwareQuality: SoftwareQuality,
      severity: SoftwareImpactSeverity,
      active: boolean,
    ) => {
      const link = byTestId(
        `overview__software-impact-${softwareQuality}-severity-${severity}`,
      ).get(ui.softwareImpactMeasureCard(softwareQuality).get());
      if (active) {
        expect(link).toHaveClass('active');
      } else {
        expect(link).not.toHaveClass('active');
      }
    },
  };
  return { user, ui };
};
