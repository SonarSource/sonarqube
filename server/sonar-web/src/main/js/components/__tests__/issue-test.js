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
import Issue from '../issue/models/issue';

describe('Model', () => {
  it('should have correct urlRoot', () => {
    const issue = new Issue();
    expect(issue.urlRoot()).toBe('/api/issues');
  });

  it('should parse response without root issue object', () => {
    const issue = new Issue();
    const example = { a: 1 };
    expect(issue.parse(example)).toEqual(example);
  });

  it('should parse response with the root issue object', () => {
    const issue = new Issue();
    const example = { a: 1 };
    expect(issue.parse({ issue: example })).toEqual(example);
  });

  it('should reset attributes (no attributes initially)', () => {
    const issue = new Issue();
    const example = { a: 1 };
    issue.reset(example);
    expect(issue.toJSON()).toEqual(example);
  });

  it('should reset attributes (override attribute)', () => {
    const issue = new Issue({ a: 2 });
    const example = { a: 1 };
    issue.reset(example);
    expect(issue.toJSON()).toEqual(example);
  });

  it('should reset attributes (different attributes)', () => {
    const issue = new Issue({ a: 2 });
    const example = { b: 1 };
    issue.reset(example);
    expect(issue.toJSON()).toEqual(example);
  });

  it('should unset `textRange` of a closed issue', () => {
    const issue = new Issue();
    const result = issue.parse({ issue: { status: 'CLOSED', textRange: { startLine: 5 } } });
    expect(result.textRange).toBeFalsy();
  });

  it('should unset `flows` of a closed issue', () => {
    const issue = new Issue();
    const result = issue.parse({ issue: { status: 'CLOSED', flows: [1, 2, 3] } });
    expect(result.flows).toEqual([]);
  });

  describe('Actions', () => {
    it('should assign', () => {
      const issue = new Issue({ key: 'issue-key' });
      const spy = jest.fn();
      issue._action = spy;
      issue.assign('admin');
      expect(spy).toBeCalledWith({
        data: { assignee: 'admin', issue: 'issue-key' },
        url: '/api/issues/assign'
      });
    });

    it('should unassign', () => {
      const issue = new Issue({ key: 'issue-key' });
      const spy = jest.fn();
      issue._action = spy;
      issue.assign();
      expect(spy).toBeCalledWith({
        data: { assignee: undefined, issue: 'issue-key' },
        url: '/api/issues/assign'
      });
    });

    it('should plan', () => {
      const issue = new Issue({ key: 'issue-key' });
      const spy = jest.fn();
      issue._action = spy;
      issue.plan('plan');
      expect(spy).toBeCalledWith({
        data: { plan: 'plan', issue: 'issue-key' },
        url: '/api/issues/plan'
      });
    });

    it('should unplan', () => {
      const issue = new Issue({ key: 'issue-key' });
      const spy = jest.fn();
      issue._action = spy;
      issue.plan();
      expect(spy).toBeCalledWith({
        data: { plan: undefined, issue: 'issue-key' },
        url: '/api/issues/plan'
      });
    });

    it('should set severity', () => {
      const issue = new Issue({ key: 'issue-key' });
      const spy = jest.fn();
      issue._action = spy;
      issue.setSeverity('BLOCKER');
      expect(spy).toBeCalledWith({
        data: { severity: 'BLOCKER', issue: 'issue-key' },
        url: '/api/issues/set_severity'
      });
    });
  });

  describe('#getLinearLocations', () => {
    it('should return single line location', () => {
      const issue = new Issue({
        textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 10 }
      });
      const locations = issue.getLinearLocations();
      expect(locations.length).toBe(1);

      expect(locations[0].line).toBe(1);
      expect(locations[0].from).toBe(0);
      expect(locations[0].to).toBe(10);
    });

    it('should return location not from 0', () => {
      const issue = new Issue({
        textRange: { startLine: 1, endLine: 1, startOffset: 5, endOffset: 10 }
      });
      const locations = issue.getLinearLocations();
      expect(locations.length).toBe(1);

      expect(locations[0].line).toBe(1);
      expect(locations[0].from).toBe(5);
      expect(locations[0].to).toBe(10);
    });

    it('should return 2-lines location', () => {
      const issue = new Issue({
        textRange: { startLine: 2, endLine: 3, startOffset: 5, endOffset: 10 }
      });
      const locations = issue.getLinearLocations();
      expect(locations.length).toBe(2);

      expect(locations[0].line).toBe(2);
      expect(locations[0].from).toBe(5);
      expect(locations[0].to).toBe(999999);

      expect(locations[1].line).toBe(3);
      expect(locations[1].from).toBe(0);
      expect(locations[1].to).toBe(10);
    });

    it('should return 3-lines location', () => {
      const issue = new Issue({
        textRange: { startLine: 4, endLine: 6, startOffset: 5, endOffset: 10 }
      });
      const locations = issue.getLinearLocations();
      expect(locations.length).toBe(3);

      expect(locations[0].line).toBe(4);
      expect(locations[0].from).toBe(5);
      expect(locations[0].to).toBe(999999);

      expect(locations[1].line).toBe(5);
      expect(locations[1].from).toBe(0);
      expect(locations[1].to).toBe(999999);

      expect(locations[2].line).toBe(6);
      expect(locations[2].from).toBe(0);
      expect(locations[2].to).toBe(10);
    });

    it('should return [] when no location', () => {
      const issue = new Issue();
      const locations = issue.getLinearLocations();
      expect(locations.length).toBe(0);
    });
  });
});
