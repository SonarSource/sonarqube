 #
 # Sonar, entreprise quality control tool.
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
class CompleteProjectLinkKeys < ActiveRecord::Migration

  def self.up
    ProjectLink.find(:all, :conditions => {:link_type => nil}).each do |link|
      link.link_type = ProjectLink.name_to_key(link.name)
      link.save
    end

    ProjectLink.update_all("name='Home'", "link_type='homepage'")
    ProjectLink.update_all("name='Sources'", "link_type='scm'")
    ProjectLink.update_all("name='Continuous integration'", "link_type='ci'")
    ProjectLink.update_all("name='Issues'", "link_type='issue'")
    ProjectLink.update_all("name='Developer connection'", "link_type='scm_dev'")
  end

  def self.down
  end
end