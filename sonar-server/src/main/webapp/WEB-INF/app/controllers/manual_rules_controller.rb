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
class ManualRulesController < ApplicationController

  before_filter :admin_required

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    @rules = Rule.manual_rules()
    render :action => 'index'
  end

  def edit
    verify_post_request
    call_backend do
      rule_update = {
          'ruleKey' => params[:key],
          'name' => params[:name],
          'htmlDescription' => params[:description]
      }
      Internal.rules.updateManualRule(rule_update)
      render :text => 'ok', :status => 200
    end
  end

  # Information : if the key already exists but is removed, it will be automatically reactivated without any message to the user
  def create
    verify_post_request
    require_parameters 'name'

    call_backend do
      manual_key = params[:name].strip.downcase.gsub(/\s/, '_')
      new_rule = {
          'manualKey' => manual_key,
          'name' => params[:name],
          'htmlDescription' => params[:description]
      }
      Internal.rules.createManualRule(new_rule)
      render :text => 'ok', :status => 200
    end
  end

  def create_form
    render :partial => 'manual_rules/create_form'
  end

  def edit_form
    @rule = Internal.rules.findByKey(params['key'])
    render :partial => 'manual_rules/edit_form', :status => 200
  end

  def delete
    verify_post_request

    call_backend do
      Internal.rules.deleteManualRule(params['key'])
      redirect_to :action => 'index'
    end
  end
end
