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
class Navigation

  attr_reader :key, :show_sidebar

  def initialize(key, show_sidebar)
    @key = key
    @show_sidebar = show_sidebar
  end

  SECTION_HOME = Navigation.new('home', true)
  SECTION_RESOURCE = Navigation.new('resource', true)
  SECTION_CONFIGURATION = Navigation.new('configuration', true)
  SECTION_RESOURCE_CONFIGURATION = Navigation.new('resource_configuration', true)
  SECTION_SESSION = Navigation.new('session', true)
  SECTION_ISSUES = Navigation.new('issues', false)
  SECTION_MEASURES = Navigation.new('measures', false)
  SECTION_QUALITY_PROFILES = Navigation.new('quality_profiles', false)
  SECTION_QUALITY_GATES = Navigation.new('quality_gates', false)
  SECTION_CODING_RULES = Navigation.new('coding_rules', false)
end
