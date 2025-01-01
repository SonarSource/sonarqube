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

const RATING_GRID_SIZE = 4;
export const PERCENT_MULTIPLIER = 100;
export const DIFF_METRIC_PREFIX_LENGTH = 4;
export const GRID_INDEX_OFFSET = 2; // Rating of 2 should get index 0 (threshold between 1 and 2)

function checkNumberRating(coverageRating: number): void {
  if (!(typeof coverageRating === 'number' && coverageRating > 0 && coverageRating < 6)) {
    throw new Error(`Unknown number rating: "${coverageRating}"`);
  }
}

export function getCoverageRatingLabel(rating: number): string {
  checkNumberRating(rating);
  const mapping = ['≥ 80%', '70% - 80%', '50% - 70%', '30% - 50%', '< 30%'];
  return mapping[rating - 1];
}

export function getCoverageRatingAverageValue(rating: number): number {
  checkNumberRating(rating);
  const mapping = [90, 75, 60, 40, 15];
  return mapping[rating - 1];
}

export function getDuplicationsRatingLabel(rating: number): string {
  checkNumberRating(rating);
  const mapping = ['< 3%', '3% - 5%', '5% - 10%', '10% - 20%', '> 20%'];
  return mapping[rating - 1];
}

export function getSizeRatingLabel(rating: number): string {
  checkNumberRating(rating);
  const mapping = ['< 1k', '1k - 10k', '10k - 100k', '100k - 500k', '> 500k'];
  return mapping[rating - 1];
}

export function getSizeRatingAverageValue(rating: number): number {
  checkNumberRating(rating);
  const mapping = [500, 5000, 50000, 250000, 750000];
  return mapping[rating - 1];
}

export const getMaintainabilityGrid = (ratingGridSetting: string) => {
  const numbers = ratingGridSetting
    .split(',')
    .map((s) => parseFloat(s))
    .filter((n) => !isNaN(n));

  return numbers.length === RATING_GRID_SIZE ? numbers : [0, 0, 0, 0];
};

const DUPLICATION_RATINGS: ['A', 'B', 'C', 'D', 'E', 'F'] = ['A', 'B', 'C', 'D', 'E', 'F'];
export function duplicationValueToRating(val: number) {
  return DUPLICATION_RATINGS[val - 1];
}
