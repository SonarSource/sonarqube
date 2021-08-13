/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.ce;

import org.sonar.api.config.Configuration;
import org.sonar.ce.configuration.WorkerCountProvider;

public class CodeScanWorkerCountProvider implements WorkerCountProvider {

    /**
     * This property defines the number of compute engine workers.
     */
    public static final String CE_WORKER_COUNT = "sonar.ce.workerCount";

    private final Configuration configuration;

    public CodeScanWorkerCountProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public int get() {
        return Integer.parseInt(configuration.get(CE_WORKER_COUNT).orElse("3"));
    }

}
