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

class Api::ViolationsController < Api::ApiController

  def index
    conditions={}

    if params['scopes']
      rest_error('The parameter "scopes" is not supported since version 3.6.')
    end

    if params['qualifiers']
      rest_error('The parameter "qualifiers" is not supported since version 3.6.')
    end

    resource = params[:resource]
    depth=(params['depth'] ? params['depth'].to_i : 0)
    if resource
      if depth==0
        conditions['components'] = resource
      elsif depth>0
        rest_error('The parameter "depth" is not supported since version 3.6.')
      else
        # negative : all the resource tree
        conditions['componentRoots'] = resource
      end
    end

    if params[:rules]
      conditions['rules'] = params[:rules].split(',')
    end

    if params[:priorities]
      conditions['severities'] = params[:priorities].split(',')
    end

    if params[:switched_off] == 'true'
      conditions['resolutions']='FALSE-POSITIVE'
    end

    limit = (params[:limit] ? [params[:limit].to_i, 5000].min : 5000)
    conditions['pageSize']=limit

    results = Api.issues.find(conditions)

    array = results.issues.map do |issue|
      hash={}
      hash[:message] = issue.message if issue.message
      hash[:line] = issue.line.to_i if issue.line
      hash[:priority] = issue.severity if issue.severity
      hash[:createdAt] = Api::Utils.format_datetime(issue.creationDate) if issue.creationDate
      hash[:switchedOff]=true if issue.resolution=='FALSE-POSITIVE'
      rule = results.rule(issue)
      if rule
        hash[:rule] = {:key => rule.ruleKey.toString(), :name => Internal.rules.ruleL10nName(rule)}
      end
      resource = results.component(issue)
      if resource
        hash[:resource] = {:key => resource.key, :name => resource.name, :qualifier => resource.qualifier}
      end
      hash
    end

    respond_to do |format|
      format.json { render :json => jsonp(array) }
      format.xml { render :xml => array.to_xml(:skip_types => true, :root => 'violations') }
    end
  end
end