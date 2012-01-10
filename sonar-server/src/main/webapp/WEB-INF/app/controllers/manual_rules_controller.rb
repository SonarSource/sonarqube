#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
class ManualRulesController < ApplicationController

  before_filter :admin_required
  verify :method => :post, :only => [:create], :redirect_to => {:action => :index}
  verify :method => :delete, :only => [:delete], :redirect_to => {:action => :index}

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    @rules = Rule.manual_rules()
    @rule=Rule.new
    render :action => 'index'
  end

  def edit
    @rules = Rule.manual_rules()
    @rule=Rule.manual_rule(params['id'].to_i)
    bad_request('Missing rule id') unless @rule
    render :action => 'index'
  end

  def create
    access_denied unless is_admin?
    begin
      if params[:id].to_i>0
        # Update rule
        rule=Rule.manual_rule(params['id'].to_i)
        bad_request('Unknown rule') unless rule

      else
        # Create rule
        rule=Rule.find_or_create_manual_rule(params[:name], true)
      end
      rule.name=(params[:name])
      rule.description=params[:description]
      rule.save!
    rescue Exception => e
      flash[:error]= e.message
    end
    redirect_to :action => 'index'
  end

  def delete
    access_denied unless is_admin?
    rule=Rule.manual_rule(params['id'].to_i)
    bad_request('Missing rule id') unless rule
    rule.enabled=false
    unless rule.save
      flash[:error]=rule.errors.to_s
    end
    redirect_to :action => 'index'
  end
end