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
import helper from '../source-viewer/helpers/code-with-issue-locations-helper';

describe('Code With Issue Locations Helper', () => {
  it('should be a function', () => {
    expect(helper).toBeTruthy();
  });

  it('should mark one location', () => {
    const code = '<span class="k">if</span> (<span class="sym-2 sym">a</span> + <span class="c">1</span>) {';
    const locations = [{ from: 1, to: 5 }];
    const result = helper(code, locations, 'x');
    expect(result).toBe([
      '<span class="k">i</span>',
      '<span class="k x">f</span>',
      '<span class=" x"> (</span>',
      '<span class="sym-2 sym x">a</span>',
      '<span class=""> + </span>',
      '<span class="c">1</span>',
      '<span class="">) {</span>'
    ].join(''));
  });

  it('should mark two locations', () => {
    const code = 'abcdefghijklmnopqrst';
    const locations = [
      { from: 1, to: 6 },
      { from: 11, to: 16 }
    ];
    const result = helper(code, locations, 'x');
    expect(result).toBe([
      '<span class="">a</span>',
      '<span class=" x">bcdef</span>',
      '<span class="">ghijk</span>',
      '<span class=" x">lmnop</span>',
      '<span class="">qrst</span>'
    ].join(''));
  });

  it('should mark one locations', () => {
    const code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>';
    const locations = [{ from: 15, to: 20 }];
    const result = helper(code, locations, 'x');
    expect(result).toBe([
      '<span class="cppd"> * Copyright (C</span>',
      '<span class="cppd x">) 200</span>',
      '<span class="cppd">8-2014 SonarSource</span>'
    ].join(''));
  });

  it('should mark two locations', () => {
    const code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>';
    const locations = [
      { from: 24, to: 29 },
      { from: 15, to: 20 }
    ];
    const result = helper(code, locations, 'x');
    expect(result).toBe([
      '<span class="cppd"> * Copyright (C</span>',
      '<span class="cppd x">) 200</span>',
      '<span class="cppd">8-20</span>',
      '<span class="cppd x">14 So</span>',
      '<span class="cppd">narSource</span>'
    ].join(''));
  });

  it('should parse line with < and >', () => {
    const code = '<span class="j">#include &lt;stdio.h&gt;</span>';
    const result = helper(code, []);
    expect(result).toBe('<span class="j">#include &lt;stdio.h&gt;</span>');
  });

  it('should parse syntax and usage highlighting', () => {
    const code = '<span class="k"><span class="sym-3 sym">this</span></span>';
    const expected = '<span class="k sym-3 sym">this</span>';
    const result = helper(code, []);
    expect(result).toBe(expected);
  });

  it('should parse nested tags', () => {
    const code = '<span class="k"><span class="sym-3 sym">this</span> is</span>';
    const expected = '<span class="k sym-3 sym">this</span><span class="k"> is</span>';
    const result = helper(code, []);
    expect(result).toBe(expected);
  });
});

