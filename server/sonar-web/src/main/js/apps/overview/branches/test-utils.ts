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
import { byLabelText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../types/clean-code-taxonomy';

export const getPageObjects = () => {
  const user = userEvent.setup();
  const selectors = {
    unsolvedOverallMessage: byText(/overview.ai_assurance.unsolved_overall.title/),
    dismissUnsolvedButton: byRole('button', {
      name: 'overview.ai_assurance.unsolved_overall.dismiss',
    }),
    overallCodeButton: byRole('tab', { name: /overview.overall_code/ }),
    softwareImpactMeasureCard: (softwareQuality: SoftwareQuality) =>
      byTestId(`overview__software-impact-card-${softwareQuality}`),
    softwareImpactMeasureCardRating: (softwareQuality: SoftwareQuality, rating: string) =>
      byLabelText(
        `overview.project.software_impact.has_rating.software_quality.${softwareQuality}.${rating}`,
      ),
  };
  const ui = {
    ...selectors,
    expectSoftwareImpactMeasureCard: (
      softwareQuality: SoftwareQuality,
      rating?: string,
      total?: number,
      branch = 'master',
      failed = false,
    ) => {
      if (failed) {
        expect(
          byTestId(`overview__software-impact-card-${softwareQuality}`)
            .byText('overview.measures.failed_badge')
            .get(),
        ).toBeInTheDocument();
      }

      if (typeof rating === 'string') {
        expect(
          byText(rating, { exact: true }).get(ui.softwareImpactMeasureCard(softwareQuality).get()),
        ).toBeInTheDocument();
      }
      if (total !== undefined) {
        const branchQuery = branch ? `&branch=${branch}` : '';

        expect(
          byRole('link', {
            name: `overview.measures.software_impact.see_list_of_x_open_issues.${total}.software_quality.${softwareQuality}`,
          }).get(),
        ).toHaveAttribute(
          'href',
          `/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=${softwareQuality}${branchQuery}&id=foo`,
        );
      }
    },
    expectSoftwareImpactMeasureCardToHaveOldMeasures: (
      softwareQuality: SoftwareQuality,
      rating: string,
      total: number,
      oldMetric: string,
      branch = 'master',
    ) => {
      const branchQuery = branch ? `&branch=${branch}` : '';
      expect(
        byText(rating, { exact: true }).get(ui.softwareImpactMeasureCard(softwareQuality).get()),
      ).toBeInTheDocument();
      expect(
        byRole('link', {
          name: `overview.measures.software_impact.see_list_of_x_open_issues.${total}.software_quality.${softwareQuality}`,
        }).get(),
      ).toHaveAttribute(
        'href',
        `/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=${oldMetric}${branchQuery}&id=foo`,
      );
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
    expectSoftwareImpactMeasureCardRatingTooltip: async (
      softwareQuality: SoftwareQuality,
      rating: string,
      text: string,
    ) => {
      await expect(
        ui.softwareImpactMeasureCardRating(softwareQuality, rating).get(),
      ).toHaveATooltipWithContent(text);
    },
  };
  return { user, ui };
};
