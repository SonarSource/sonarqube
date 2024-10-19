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
import { formatDuration } from '../utils';

describe('Helpers', () => {
  describe('#formatDuration()', () => {
    it('should format 173ms', () => {
      expect(formatDuration(173)).toBe('173ms');
    });

    it('should format 999ms', () => {
      expect(formatDuration(999)).toBe('999ms');
    });

    it('should format 1s 0ms', () => {
      expect(formatDuration(1000)).toBe('1.0s');
    });

    it('should format 1s 1ms', () => {
      expect(formatDuration(1001)).toBe('1.1s');
    });

    it('should format 1s 501ms', () => {
      expect(formatDuration(1501)).toBe('1.501s');
    });

    it('should format 59s', () => {
      expect(formatDuration(59000)).toBe('59s');
    });

    it('should format 1min 0s', () => {
      expect(formatDuration(60000)).toBe('1min 0s');
    });

    it('should format 1min 2s', () => {
      expect(formatDuration(62757)).toBe('1min 2s');
    });

    it('should format 3min 44s', () => {
      expect(formatDuration(224567)).toBe('3min 44s');
    });

    it('should format 1h 20m', () => {
      expect(formatDuration(80 * 60 * 1000)).toBe('1h 20min');
    });
  });
});
