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
const checkNumberRating = coverageRating => {
  if (!(typeof coverageRating === 'number' && coverageRating > 0 && coverageRating < 6)) {
    throw new Error(`Unknown number rating: "${coverageRating}"`);
  }
};

export const getCoverageRatingLabel = rating => {
  checkNumberRating(rating);

  const mapping = ['≥ 80%', '< 80%', '< 70%', '< 50%', '< 30%'];
  return mapping[rating - 1];
};
export const getCoverageRatingAverageValue = rating => {
  checkNumberRating(rating);
  const mapping = [90, 75, 60, 40, 15];
  return mapping[rating - 1];
};
export const getDuplicationsRatingLabel = rating => {
  checkNumberRating(rating);
  const mapping = ['< 3%', '≥ 3%', '> 5%', '> 10%', '> 20%'];
  return mapping[rating - 1];
};
export const getDuplicationsRatingAverageValue = rating => {
  checkNumberRating(rating);
  const mapping = [1.5, 4, 7.5, 15, 30];
  return mapping[rating - 1];
};
export const getSizeRatingLabel = rating => {
  checkNumberRating(rating);
  const mapping = ['< 1k', '≥ 1k', '> 10k', '> 100k', '> 500k'];
  return mapping[rating - 1];
};
export const getSizeRatingAverageValue = rating => {
  checkNumberRating(rating);
  const mapping = [500, 5000, 50000, 250000, 750000];
  return mapping[rating - 1];
};
