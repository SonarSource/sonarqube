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

require "json"

class Api::ViolationsController < Api::ResourceRestController

  def rest_call
    snapshot = @resource.last_snapshot
    
    conditions=[]
    values={}
    if params['scopes']
      conditions << 'snapshots.scope in (:scopes)'
      values[:scopes]=params['scopes'].split(',')
    end
    if params['qualifiers']
      conditions << 'snapshots.qualifier in (:qualifiers)'
      values[:qualifiers]=params['qualifiers'].split(',')
    end

    depth=(params['depth'] ? params['depth'].to_i : 0)
    if depth==0
      conditions << 'snapshots.id=:sid'
      values[:sid]=snapshot.id
      
    elsif depth>0
      # all the resource tree
      conditions << 'snapshots.root_snapshot_id=:root_sid'
      values[:root_sid] = (snapshot.root_snapshot_id || snapshot.id)

      conditions << 'snapshots.path LIKE :path'
      values[:path]="#{snapshot.path}#{snapshot.id}.%"

      conditions << 'snapshots.depth=:depth'
      values[:depth] = snapshot.depth + depth

    else
      # negative : all the resource tree
      conditions << '(snapshots.id=:sid OR (snapshots.root_snapshot_id=:root_sid AND snapshots.path LIKE :path))'
      values[:sid] = snapshot.id
      values[:root_sid] = (snapshot.root_snapshot_id || snapshot.id)
      values[:path]="#{snapshot.path}#{snapshot.id}.%"
    end
    
    if params[:rules]
      rule_ids=params[:rules].split(',').map do |key_or_id|
        Rule.to_i(key_or_id)
      end.compact
      conditions << 'rule_id IN (:rule_ids)'
      values[:rule_ids] = rule_ids
    end
    param_categories=params[:categories] || params[:category]
    if param_categories
      conditions << 'rules.rules_category_id IN (:category_ids)'
      values[:category_ids]=param_categories.split(',').map do |c|
        categ=RulesCategory.by_key(c)
        categ ? categ.id : -1
      end.compact
    end
    if params[:priorities]
      conditions << 'rule_failures.failure_level IN (:priorities)'
      values[:priorities]=params[:priorities].split(',').map do |p|
        Sonar::RulePriority.id(p)
      end.compact
    end

    limit = (params[:limit] ? [params[:limit].to_i,5000].min : 5000) 
    violations = RuleFailure.find(:all,
      :conditions => [ conditions.join(' AND '), values],
      :include => [:snapshot, {:snapshot => :project}, :rule],
      :order => 'rule_failures.failure_level DESC',
      :limit => limit)
    rest_render(violations)
  end

  def rest_to_json(rule_failures)
    JSON(rule_failures.collect{|rule_failure| rule_failure.to_hash_json})
  end

  def rest_to_xml(rule_failures)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.violations do
      rule_failures.each do |rule_failure|
        rule_failure.to_xml(xml)
      end
    end
  end

end