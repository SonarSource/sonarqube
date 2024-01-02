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
import {
  getCoverageRatingAverageValue,
  getCoverageRatingLabel,
  getDuplicationsRatingAverageValue,
  getDuplicationsRatingLabel,
  getSizeRatingAverageValue,
  getSizeRatingLabel,
} from '../ratings';

describe('getCoverageRatingLabel', () => {
  it('should fail', () => {
    expect(() => {
      getCoverageRatingLabel(-1);
    }).toThrow();
  });
  it.each([
    [1, '≥ 80%'],
    [2, '70% - 80%'],
    [3, '50% - 70%'],
    [4, '30% - 50%'],
    [5, '< 30%'],
  ])('should return the correct label for %s', (rating, label) => {
    expect(getCoverageRatingLabel(rating)).toBe(label);
  });
});

describe('getCoverageRatingAverageValue', () => {
  it.each([
    [1, 90],
    [2, 75],
    [3, 60],
    [4, 40],
    [5, 15],
  ])('should return the correct value', (rating, value) => {
    expect(getCoverageRatingAverageValue(rating)).toBe(value);
  });
});

describe('getDuplicationsRatingLabel', () => {
  it('should fail', () => {
    expect(() => {
      getCoverageRatingLabel(-1);
    }).toThrow();
  });
  it.each([
    [1, '< 3%'],
    [2, '3% - 5%'],
    [3, '5% - 10%'],
    [4, '10% - 20%'],
    [5, '> 20%'],
  ])('should return the correct label for %s', (rating, label) => {
    expect(getDuplicationsRatingLabel(rating)).toBe(label);
  });
});

describe('getDuplicationsRatingAverageValue', () => {
  it.each([
    [1, 1.5],
    [2, 4],
    [3, 7.5],
    [4, 15],
    [5, 30],
  ])('should return the correct value', (rating, value) => {
    expect(getDuplicationsRatingAverageValue(rating)).toBe(value);
  });
});

describe('getSizeRatingLabel', () => {
  it('should fail', () => {
    expect(() => {
      getCoverageRatingLabel(-1);
    }).toThrow();
  });
  it.each([
    [1, '< 1k'],
    [2, '1k - 10k'],
    [3, '10k - 100k'],
    [4, '100k - 500k'],
    [5, '> 500k'],
  ])('should return the correct label for %s', (rating, label) => {
    expect(getSizeRatingLabel(rating)).toBe(label);
  });
});

describe('getSizeRatingAverageValue', () => {
  it.each([
    [1, 500],
    [2, 5000],
    [3, 50000],
    [4, 250000],
    [5, 750000],
  ])('should return the correct value', (rating, value) => {
    expect(getSizeRatingAverageValue(rating)).toBe(value);
  });
});
