#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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

  def label_for_rules_count(qProfile, all_profile_stats)
    profile_stat = all_profile_stats[qProfile.key()] if all_profile_stats
    profile_rules_count = profile_rules_count(profile_stat)
    label = "#{profile_rules_count} #{message('rules').downcase}"

    count_overriding = overriding_rules_count(profile_stat)
    if count_overriding && count_overriding>0
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

  def projects_count(qProfile)
    Internal.quality_profiles.countProjects(qProfile).to_i
  end

  def profile_rules_count(profile_stat)
    count = 0
    count = profile_stat.get('countActiveRules').get(0).getValue() if profile_stat && profile_stat.get('countActiveRules')
    count
  end

  def overriding_rules_count(profile_stat)
    inheritance_stats = Hash[ *profile_stat.get('inheritance').collect { |v| [ v.getKey(), v ] }.flatten ] if profile_stat
    inheritance_stats['OVERRIDES'].getValue().to_i if inheritance_stats && inheritance_stats['OVERRIDES']
  end
end
