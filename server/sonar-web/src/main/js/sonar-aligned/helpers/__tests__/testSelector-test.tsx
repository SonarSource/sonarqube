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

import { render } from '@testing-library/react';
import {
  byDisplayValue,
  byLabelText,
  byPlaceholderText,
  byRole,
  byTestId,
  byText,
  byTitle,
} from '../testSelector';

describe('byText', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byText('test').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byText('repeated').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byText('test').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byText('repeated').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byText('test').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byText('repeated').queryAll()).toHaveLength(2);
  });
});

describe('byRole', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byRole('button').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byRole('alert').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byRole('button').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byRole('alert').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byRole('button').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byRole('alert').queryAll()).toHaveLength(2);
  });
});

describe('byPlaceholderText', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byPlaceholderText('placeholder').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byPlaceholderText('repeated').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byPlaceholderText('placeholder').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byPlaceholderText('repeated').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byPlaceholderText('placeholder').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byPlaceholderText('repeated').queryAll()).toHaveLength(2);
  });
});

describe('byLabelText', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byLabelText('test').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byLabelText('alert').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byLabelText('test').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byLabelText('alert').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byLabelText('test').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byLabelText('alert').queryAll()).toHaveLength(2);
  });
});

describe('byTestId', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byTestId('test').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byTestId('alert').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byTestId('test').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byTestId('alert').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byTestId('test').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byTestId('alert').queryAll()).toHaveLength(2);
  });
});

describe('byDisplayValue', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byDisplayValue('one').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byDisplayValue('two').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byDisplayValue('one').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byDisplayValue('two').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byDisplayValue('one').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byDisplayValue('two').queryAll()).toHaveLength(2);
  });
});

describe('byTitle', () => {
  it('should find', async () => {
    renderByRTL();
    expect(await byTitle('test').find()).toBeVisible();
  });

  it('should find all', async () => {
    renderByRTL();
    expect(await byTitle('alert').findAll()).toHaveLength(2);
  });

  it('should get', () => {
    renderByRTL();
    expect(byTitle('test').get()).toBeVisible();
  });

  it('should get all', () => {
    renderByRTL();
    expect(byTitle('alert').getAll()).toHaveLength(2);
  });

  it('should query', () => {
    renderByRTL();
    expect(byTitle('test').query()).toBeVisible();
  });

  it('should query all', () => {
    renderByRTL();
    expect(byTitle('alert').queryAll()).toHaveLength(2);
  });
});

function renderByRTL() {
  return render(
    <div>
      <p aria-label="test" data-testid="test" title="test">
        test
      </p>
      <div aria-label="alert" data-testid="alert" role="alert" title="alert">
        repeated
      </div>
      <div aria-label="alert" data-testid="alert" role="alert" title="alert">
        repeated
      </div>
      <button type="submit">click me</button>
      <input name="name" placeholder="placeholder" />
      <input name="repeated" placeholder="repeated" />
      <input name="repeated2" placeholder="repeated" />
      <select>
        <option value="1">one</option>
        <option value="2">two</option>
      </select>
      <textarea defaultValue="two" />
      <textarea defaultValue="two" />
    </div>,
  );
}
