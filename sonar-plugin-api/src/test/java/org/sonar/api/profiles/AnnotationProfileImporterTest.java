/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.profiles;

import org.junit.Test;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Check;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AnnotationProfileImporterTest {

  @Test
  public void importProfile() {
    AnnotationProfileImporter importer = AnnotationProfileImporter.create("checkstyle", Arrays.<Class>asList(FakeRule.class));
    ValidationMessages validation = ValidationMessages.create();
    ProfilePrototype profile = importer.importProfile(null, validation);
    assertThat(profile.getRule("checkstyle", "fake").getPriority(), is(RulePriority.BLOCKER));
  }
}

@BelongsToProfile(title = "not used !", priority = Priority.BLOCKER)
@Check(key = "fake", isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class FakeRule {

}
