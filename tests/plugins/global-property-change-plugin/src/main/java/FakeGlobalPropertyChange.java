/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.GlobalPropertyChangeHandler;
import org.sonar.api.utils.log.Loggers;

@Properties(@Property(key = "globalPropertyChange.received", name = "Check that extension has correctly been notified by global property change", category = "fake"))
public final class FakeGlobalPropertyChange extends GlobalPropertyChangeHandler {

  @Override
  public void onChange(PropertyChange propertyChange) {
    Loggers.get(FakeGlobalPropertyChange.class).info("Received change: " + propertyChange);
  }
}
