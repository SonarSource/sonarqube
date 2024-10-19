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

import { ScaleTime } from 'd3-scale';
import { TimeMachineResponse } from '../api/time-machine';
import { SOFTWARE_QUALITY_RATING_METRICS_MAP } from './constants';

export const mergeRatingMeasureHistory = (
  historyData: TimeMachineResponse | undefined,
  parseDateFn: (date: string) => Date,
  isLegacy = false,
) => {
  const softwareQualityMeasures = Object.values(SOFTWARE_QUALITY_RATING_METRICS_MAP);
  const softwareQualityMeasuresMap = new Map<
    string,
    { history: { date: string; value?: string }[]; index: number; splitDate?: Date }
  >();
  if (isLegacy) {
    return (
      historyData?.measures
        ?.filter((m) => !softwareQualityMeasures.includes(m.metric))
        .map((measure) => ({
          metric: measure.metric,
          history: measure.history.map((historyItem) => ({
            date: parseDateFn(historyItem.date),
            value: historyItem.value,
          })),
        })) ?? []
    );
  }

  const historyDataFiltered =
    historyData?.measures?.filter((measure) => {
      if (softwareQualityMeasures.includes(measure.metric)) {
        const splitPointIndex = measure.history.findIndex(
          (historyItem) => historyItem.value != null,
        );
        softwareQualityMeasuresMap.set(measure.metric, {
          history: measure.history,
          index: measure.history.findIndex((historyItem) => historyItem.value != null),
          splitDate:
            // Don't show splitPoint if it's the first history item
            splitPointIndex !== -1 && splitPointIndex !== 0
              ? parseDateFn(measure.history[splitPointIndex].date)
              : undefined,
        });
        return false;
      }
      return true;
    }) ?? [];

  const historyMapper = (historyItem: { date: string; value?: string }) => ({
    date: parseDateFn(historyItem.date),
    value: historyItem.value,
  });

  return historyDataFiltered.map((measure) => {
    const softwareQualityMetric = softwareQualityMeasuresMap.get(
      SOFTWARE_QUALITY_RATING_METRICS_MAP[measure.metric],
    );
    return {
      metric: measure.metric,
      splitPointDate: softwareQualityMetric ? softwareQualityMetric.splitDate : undefined,
      history: softwareQualityMetric
        ? measure.history
            .slice(0, softwareQualityMetric.index)
            .map(historyMapper)
            .concat(
              softwareQualityMetric.history.slice(softwareQualityMetric.index).map(historyMapper),
            )
        : measure.history.map(historyMapper),
    };
  });
};

export const shouldShowSplitLine = (
  splitPointDate: Date | undefined,
  xScale: ScaleTime<number, number>,
): splitPointDate is Date =>
  splitPointDate !== undefined &&
  xScale(splitPointDate) >= xScale.range()[0] &&
  xScale(splitPointDate) <= xScale.range()[1];
