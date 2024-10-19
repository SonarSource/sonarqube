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
package io.codescan.sonarqube.codescanhosted.scanner;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;

/**
 * This class checks that the branch name is valid... according to git branch name is a good test
 */
public class CodeScanBranchParamsValidator implements BranchParamsValidator {

    private final GlobalConfiguration config;

    public CodeScanBranchParamsValidator(final GlobalConfiguration config) {
        this.config = config;
    }

    @Override
    public void validate(final List<String> validationMessages) {
        final String branchName = this.config.get("sonar.branch.name").orElse(null);
        if (StringUtils.isNotEmpty(branchName) && !isValid(branchName)) {
            validationMessages.add(String.format("Invalid branch name: \"%s\". Needs to be a valid git branch name",
                    branchName));
        }
    }

    private static boolean isValid(String refName) {
        final int len = refName.length();
        if (len == 0) {
            return false;
        }
        if (refName.endsWith(".lock")) {
            return false;
        }

        int components = 1;
        char p = '\0';
        for (int i = 0; i < len; i++) {
            final char c = refName.charAt(i);
            if (c <= ' ') {
                return false;
            }
            switch (c) {
                case '.':
                    switch (p) {
                        case '\0':
                        case '/':
                        case '.':
                            return false;
                    }
                    if (i == len - 1) {
                        return false;
                    }
                    break;
                case '/':
                    if (i == 0 || i == len - 1) {
                        return false;
                    }
                    if (p == '/') {
                        return false;
                    }
                    components++;
                    break;
                case '{':
                    if (p == '@') {
                        return false;
                    }
                    break;
                case '~':
                case '^':
                case ':':
                case '?':
                case '[':
                case '*':
                case '\\':
                case '\u007F':
                    return false;
            }
            p = c;
        }
        return components > 0;
    }
}
