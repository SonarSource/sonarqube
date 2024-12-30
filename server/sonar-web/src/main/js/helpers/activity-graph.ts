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
import { MQR_CONDITIONS_MAP, STANDARD_CONDITIONS_MAP } from '../apps/quality-gates/utils';

export const mergeMeasureHistory = (
  historyData: TimeMachineResponse | undefined,
  parseDateFn: (date: string) => Date,
  isStandardMode = false,
) => {
  const standardMeasuresMap = new Map<
    string,
    { history: { date: string; value?: string }[]; index: number; splitDate?: Date }
  >();
  if (isStandardMode) {
    return (
      historyData?.measures.map((measure) => ({
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
      if (MQR_CONDITIONS_MAP[measure.metric]) {
        const splitPointIndex = measure.history.findIndex(
          (historyItem) => historyItem.value != null,
        );
        standardMeasuresMap.set(measure.metric, {
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
    const metric = STANDARD_CONDITIONS_MAP[measure.metric];
    const softwareQualityMetric = standardMeasuresMap.get(metric as string);
    if (softwareQualityMetric !== undefined && metric) {
      return {
        metric,
        splitPointDate: softwareQualityMetric.splitDate,
        history: measure.history
          .slice(0, softwareQualityMetric.index)
          .map(historyMapper)
          .concat(
            softwareQualityMetric.history.slice(softwareQualityMetric.index).map(historyMapper),
          ),
      };
    }

    return {
      metric: measure.metric,
      history: measure.history.map(historyMapper),
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
