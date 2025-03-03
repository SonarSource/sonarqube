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
package org.sonar.process.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;

public class LogMaskingUtil {

    private static final int SKIP_FIRST_N_CHARS = 2;
    private static final Pattern MASK_EMAIL_PATTERN = Pattern.compile("([\\w.\\-+_]+)@[\\w.\\-+_]+\\.\\w+",
            Pattern.MULTILINE);

    public static String maskMessage(String message, Pattern pattern) {
        if (StringUtils.isNotEmpty(message)) {
            StringBuilder sb = new StringBuilder(message);
            Matcher matcher = pattern.matcher(sb);
            while (matcher.find()) {
                IntStream.rangeClosed(1, matcher.groupCount()).forEach(group -> {
                    if (matcher.group(group) != null) {
                        IntStream.range(matcher.start(group),
                                matcher.end(group)).skip(SKIP_FIRST_N_CHARS).forEach(i -> sb.setCharAt(i, '*'));
                    }
                });
            }
            return sb.toString();
        }
        return message;
    }

    public static String maskEmail(String message) {
        return maskMessage(message, MASK_EMAIL_PATTERN);
    }
}