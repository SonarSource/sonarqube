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
class IncludeBranchInProjectName < ActiveRecord::Migration

  def self.up
    Project.find(:all, :conditions => {:scope => Project::SCOPE_SET, :qualifier => Project::QUALIFIER_PROJECT}).each do |project|
      branch=branch(project)
      if branch
        project.name+= ' ' + branch
        project.save!
      end
    end
  end

  def self.down

  end

  private
  def self.branch(project)
    s=project.kee.split(':')
    if s.size>=3
      return s[2]
    end
    nil
  end
end