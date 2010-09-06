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

  def exportable_plugins_by_language
    hash = {}
    controller.java_facade.getLanguages().each do |language|
      hash[language.getKey()] = controller.java_facade.getPluginsWithConfigurationExportable(language)
    end
    hash
  end
  
  def projects_tooltip(profile)
    return nil if profile.projects.empty?
    html='<ul>'
    profile.projects.each do |project|
      html+="<li style='white-space: nowrap'>#{escape_javascript project.name}</li>"
    end
    html+='</ul>'
    html
  end
end