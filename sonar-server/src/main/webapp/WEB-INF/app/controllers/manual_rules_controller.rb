#
# SonarQube, open source software quality management tool.
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
class ManualRulesController < ApplicationController

  before_filter :admin_required

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    @rules = Rule.manual_rules()
    @rule=Rule.new
    render :action => 'index'
  end

  def edit
    verify_post_request
    access_denied unless is_admin?
    begin
      # Update rule
      rule=Rule.manual_rule(params['id'].to_i)
      bad_request('Unknown rule') unless rule
      rule.name=(params[:name])
      rule.description=params[:description]
      rule.save!
    rescue Exception => e
      @error= e.message
    end
    @rule = rule
    if @error
      render :partial => 'manual_rules/edit_form', :status => 400
    else
      flash[:notice] = 'Manual rule saved'
      render :text => 'ok', :status => 200
    end

  end

  def create
    verify_post_request
    access_denied unless is_admin?
    begin
        # Create rule
        Rule.create_manual_rule(params)
    rescue Exception => e
      @error= e.message
    end
    if @error
      render :partial => 'manual_rules/create_form', :status => 400
    else
      flash[:notice] = 'Manual rule created'
      render :text => 'ok', :status => 200
    end
  end

  def create_form
    @rule = Rule.new
    render :partial => 'manual_rules/create_form'
  end

  def edit_form
    @rule=Rule.manual_rule(params['id'].to_i)
      render :partial => 'manual_rules/edit_form', :status => 200
  end

  def delete
    verify_post_request
    access_denied unless is_admin?
    rule=Rule.manual_rule(params['id'].to_i)
    bad_request('Missing rule id') unless rule
    rule.status=Rule::STATUS_REMOVED
    unless rule.save
      flash[:error]=rule.errors.to_s
    end
    redirect_to :action => 'index'
  end
end
