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
class Api::DependencyTreeController < Api::ApiController
    
  def index
    begin
      @resource = load_resource(params[:resource], :user)
      scopes = (params[:scopes] || 'PRJ,DIR,FIL,PAC,CLA').split(',')
      dependencies=[]
      if @resource.last_snapshot
        project_sid = @resource.last_snapshot.project_snapshot.id
        dependencies = Dependency.find(:all, :include => ['to','to_snapshot'], :conditions => ["project_snapshot_id=? and (from_scope in (?) or to_scope in (?))", project_sid, scopes, scopes])
      end
      
      dependencies_by_from={}
      dependencies.each do |dep|
        dependencies_by_from[dep.from_snapshot_id]||=[]
        dependencies_by_from[dep.from_snapshot_id]<<dep
      end

      respond_to do |format| 
        format.json{ render :json => jsonp(to_json(dependencies_by_from, @resource.last_snapshot.id)) }
        format.xml { render :xml => xml_not_supported}
        format.text { render :text => text_not_supported}
      end
      
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end
  
  private
  
  def load_resource(resource_id, role=nil)
    resource=Project.by_key(resource_id)
    if resource.nil?
      raise ApiException.new 404, "Resource not found: #{resource_id}"
    end
    if role && !has_role?(role, resource)
      raise ApiException.new 401, "Unauthorized"
    end
    resource
  end
  
  def to_json(dependencies_by_from, from_sid)
    json = []
    dependencies = dependencies_by_from.delete(from_sid)
    if dependencies
      dependencies.each do |dep|
        hash={
          :did => dep.id.to_s,
          :rid => dep.to_resource_id.to_s,
          :w => dep.weight,
          :u => dep.usage,
          :s => dep.to_scope,
          :q => dep.to_snapshot.qualifier}
        hash[:v]=dep.to_snapshot.version if dep.to_snapshot.version
        if dep.to
          hash[:k]=dep.to.key
          hash[:n]=dep.to.name(true)
        else
          hash[:k]=''
          hash[:n]='[missing title, please re-analyze the project]'
        end

        to=to_json(dependencies_by_from, dep.to_snapshot_id)
        hash[:to]=to if to && !to.empty?
        json<<hash
      end
    end
    json
  end
end