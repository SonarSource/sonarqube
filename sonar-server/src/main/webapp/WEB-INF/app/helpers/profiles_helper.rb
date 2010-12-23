#
# Sonar, open source software quality management tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
module ProfilesHelper
  def languages
    controller.java_facade.getLanguages()
  end

  def label_for_rules_count(profile)
    label="#{profile.active_rules.count} rules"

    count_overriding=profile.count_overriding_rules
    if count_overriding>0
      label += ", incl. #{count_overriding} overriding"
      label += image_tag('overrides.png')
    end
    label
  end
end