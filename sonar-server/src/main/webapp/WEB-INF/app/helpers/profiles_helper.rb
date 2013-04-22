#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
module ProfilesHelper
  def languages
    controller.java_facade.getLanguages()
  end

  def label_for_rules_count(profile)
    label="#{profile.count_active_rules} #{message('rules').downcase}"

    count_overriding=profile.count_overriding_rules
    if count_overriding>0
      label += message('quality_profiles.including_x_overriding.suffix', :params => count_overriding)
      label += image_tag('overrides.png')
    end
    label
  end

  def options_for_profiles(profiles, selected_id=nil)
    html=""
    profiles.group_by(&:language).each do |language, profiles|
      html += "<optgroup label=\"#{html_escape(language)}\">"
      profiles.each do |profile|
        html += "<option value='#{profile.id}' #{'selected' if profile.id==selected_id}>#{html_escape(profile.name)}</option>"
      end
      html += "</optgroup>"
    end
    html
  end
end