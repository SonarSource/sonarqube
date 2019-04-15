/*
 * Copyright (C) 2017-2019 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
import { GlobalWithFetchMock } from 'jest-fetch-mock';

const customGlobal: GlobalWithFetchMock = global as GlobalWithFetchMock;

customGlobal.fetch = require('jest-fetch-mock');

customGlobal.fetchMock = customGlobal.fetch;
