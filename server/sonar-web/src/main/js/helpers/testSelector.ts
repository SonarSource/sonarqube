/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  BoundFunction,
  GetByBoundAttribute,
  GetByRole,
  GetByText,
  screen,
  waitForOptions,
  within,
} from '@testing-library/react';

function maybeScreen(container?: HTMLElement) {
  return container ? within(container) : screen;
}

interface ReactTestingQuery {
  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ): Promise<T>;
  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ): Promise<T[]>;
  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T;
  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T[];
  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T | null;
  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T[] | null;
}

export class QuerySelector {
  dispatchQuery: ReactTestingQuery;

  constructor(dispatchQuery: ReactTestingQuery) {
    this.dispatchQuery = dispatchQuery;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ): Promise<T> {
    return this.dispatchQuery.find<T>(container, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ): Promise<T[]> {
    return this.dispatchQuery.findAll<T>(container, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T {
    return this.dispatchQuery.get<T>(container);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T[] {
    return this.dispatchQuery.getAll<T>(container);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T | null {
    return this.dispatchQuery.query<T>(container);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement): T[] | null {
    return this.dispatchQuery.queryAll<T>(container);
  }

  getAt<T extends HTMLElement = HTMLElement>(index: number, container?: HTMLElement): T {
    return this.getAll<T>(container)[index];
  }

  async findAt<T extends HTMLElement = HTMLElement>(
    index: number,
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ): Promise<T> {
    return (await this.findAll<T>(container, waitForOptions))[index];
  }

  queryAt<T extends HTMLElement = HTMLElement>(index: number, container?: HTMLElement): T | null {
    const all = this.queryAll<T>(container);
    if (all) {
      return all[index];
    }
    return null;
  }

  by(selector: ReactTestingQuery): QuerySelector {
    return new ChainDispatch(this, selector);
  }

  byText(...args: Parameters<BoundFunction<GetByText>>): QuerySelector {
    return this.by(new DispatchByText(args));
  }

  byRole(...args: Parameters<BoundFunction<GetByRole>>): QuerySelector {
    return this.by(new DispatchByRole(args));
  }

  byPlaceholderText(...args: Parameters<BoundFunction<GetByBoundAttribute>>): QuerySelector {
    return this.by(new DispatchByPlaceholderText(args));
  }

  byLabelText(...args: Parameters<BoundFunction<GetByText>>): QuerySelector {
    return this.by(new DispatchByLabelText(args));
  }

  byTestId(...args: Parameters<BoundFunction<GetByBoundAttribute>>): QuerySelector {
    return this.by(new DispatchByTestId(args));
  }

  byDisplayValue(...args: Parameters<BoundFunction<GetByBoundAttribute>>): QuerySelector {
    return this.by(new DispatchByDisplayValue(args));
  }

  byTitle(...args: Parameters<BoundFunction<GetByBoundAttribute>>): ReactTestingQuery {
    return this.by(new DispatchByTitle(args));
  }
}

class ChainDispatch extends QuerySelector {
  innerQuery: QuerySelector;

  constructor(insideQuery: QuerySelector, elementQuery: ReactTestingQuery) {
    super(elementQuery);
    this.innerQuery = insideQuery;
  }

  async find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return this.dispatchQuery.find<T>(await this.innerQuery.find(container, waitForOptions));
  }

  async findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return this.dispatchQuery.findAll<T>(await this.innerQuery.find(container, waitForOptions));
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return this.dispatchQuery.get<T>(this.innerQuery.get(container));
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    const containers = this.innerQuery.getAll(container);
    return containers.reduce(
      (acc, item) => [...acc, ...(this.dispatchQuery.queryAll<T>(item) ?? [])],
      [],
    );
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    const innerContainer = this.innerQuery.query(container);
    if (innerContainer) {
      return this.dispatchQuery.query<T>(innerContainer);
    }
    return null;
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    const innerContainer = this.innerQuery.query(container);
    if (innerContainer) {
      return this.dispatchQuery.queryAll<T>(innerContainer);
    }
    return null;
  }
}

class DispatchByText implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByText>>;

  constructor(args: Parameters<BoundFunction<GetByText>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByText<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByText<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByText<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByText<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByText<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByText<T>(...this.args);
  }
}

class DispatchByLabelText implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByText>>;

  constructor(args: Parameters<BoundFunction<GetByText>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByLabelText<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByLabelText<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByLabelText<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByLabelText<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByLabelText<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByLabelText<T>(...this.args);
  }
}

class DispatchByRole implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByRole>>;

  constructor(args: Parameters<BoundFunction<GetByRole>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByRole<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByRole<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByRole<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByRole<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByRole<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByRole<T>(...this.args);
  }
}

class DispatchByTestId implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByBoundAttribute>>;

  constructor(args: Parameters<BoundFunction<GetByBoundAttribute>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByTestId<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByTestId<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByTestId<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByTestId<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByTestId<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByTestId<T>(...this.args);
  }
}

class DispatchByTitle implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByBoundAttribute>>;

  constructor(args: Parameters<BoundFunction<GetByBoundAttribute>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByTitle<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByTitle<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByTitle<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByTitle<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByTitle<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByTitle<T>(...this.args);
  }
}

class DispatchByDisplayValue implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByBoundAttribute>>;

  constructor(args: Parameters<BoundFunction<GetByBoundAttribute>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByDisplayValue<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByDisplayValue<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByDisplayValue<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByDisplayValue<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByDisplayValue<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByDisplayValue<T>(...this.args);
  }
}

class DispatchByPlaceholderText implements ReactTestingQuery {
  readonly args: Parameters<BoundFunction<GetByBoundAttribute>>;

  constructor(args: Parameters<BoundFunction<GetByBoundAttribute>>) {
    this.args = args;
  }

  find<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findByPlaceholderText<T>(...this.args, waitForOptions);
  }

  findAll<T extends HTMLElement = HTMLElement>(
    container?: HTMLElement,
    waitForOptions?: waitForOptions,
  ) {
    return maybeScreen(container).findAllByPlaceholderText<T>(...this.args, waitForOptions);
  }

  get<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getByPlaceholderText<T>(...this.args);
  }

  getAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).getAllByPlaceholderText<T>(...this.args);
  }

  query<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryByPlaceholderText<T>(...this.args);
  }

  queryAll<T extends HTMLElement = HTMLElement>(container?: HTMLElement) {
    return maybeScreen(container).queryAllByPlaceholderText<T>(...this.args);
  }
}

export function byText(...args: Parameters<BoundFunction<GetByText>>) {
  return new QuerySelector(new DispatchByText(args));
}

export function byRole(...args: Parameters<BoundFunction<GetByRole>>) {
  return new QuerySelector(new DispatchByRole(args));
}

export function byPlaceholderText(...args: Parameters<BoundFunction<GetByBoundAttribute>>) {
  return new QuerySelector(new DispatchByPlaceholderText(args));
}

export function byLabelText(...args: Parameters<BoundFunction<GetByText>>) {
  return new QuerySelector(new DispatchByLabelText(args));
}

export function byTestId(...args: Parameters<BoundFunction<GetByBoundAttribute>>) {
  return new QuerySelector(new DispatchByTestId(args));
}

export function byTitle(...args: Parameters<BoundFunction<GetByBoundAttribute>>) {
  return new QuerySelector(new DispatchByTitle(args));
}

export function byDisplayValue(...args: Parameters<BoundFunction<GetByBoundAttribute>>) {
  return new QuerySelector(new DispatchByDisplayValue(args));
}
