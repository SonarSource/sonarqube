#
# Sonar, entreprise quality control tool.
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

require "json"

class Api::DependenciesController < Api::ResourceRestController
  
  def rest_call
    conditions=[]
    values={}
  
    if params[:parent]
      conditions<<'dependencies.parent_dependency_id=:parent'
      values[:parent]=params[:parent].to_i
    elsif params[:id]
      conditions<<'dependencies.id=:id'
      values[:id]=params[:id].to_i
    else
      snapshot=@resource.last_snapshot
      direction=params[:dir]
      if direction=='in'
        conditions<<'dependencies.to_snapshot_id=:tosid'
        values[:tosid]=snapshot.id
      elsif direction=='out'
        conditions<<'dependencies.from_snapshot_id=:fromsid'
        values[:fromsid]=snapshot.id
      else
        conditions<<'(dependencies.from_snapshot_id=:fromsid OR dependencies.to_snapshot_id=:tosid)'
        values[:fromsid]=snapshot.id
        values[:tosid]=snapshot.id
      end
    end
  
    dependencies=Dependency.find(:all, :include => ['from','to'], :conditions => [conditions.join(' AND '), values])

    dependencies=dependencies.sort do |x,y|
      result=(x.from.name(true) <=> y.from.name(true))
      if result==0
        result=(x.to.name(true) <=> y.to.name(true))
      end
      result
    end
    
    rest_render(dependencies)
  end

  private
  
  def rest_to_json(dependencies)
    result=[]
    dependencies.each do |dependency|
      result<<dependency.to_json(params)
    end
    JSON(result)
  end

  def rest_to_xml(dependencies)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.deps do
      dependencies.each do |dependency|
        xml.dep do
          dependency.to_xml(xml, params)
        end
      end
    end
  end
end