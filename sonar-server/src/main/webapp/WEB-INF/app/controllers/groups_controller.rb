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
class GroupsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required

  def index
    @groups = Group.find(:all, :order => 'name')
    if params[:id]
      @group = Group.find(params[:id])
    else
      @group = Group.new
    end
  end
  
  def create
	  group = Group.new(params[:group])
	  if group.save
      flash[:notice] = 'Group is created.'
    end
    
	  to_index(group.errors, nil)
  end

  def update
    group = Group.find(params[:id])
    if group.update_attributes(params[:group])
      flash[:notice] = 'Group is updated.'
    end
	
    to_index(group.errors, nil)
  end

  def destroy
    group = Group.find(params[:id])
    if group.destroy
      flash[:notice] = 'Group is deleted.'
    end
	
    to_index(group.errors, nil)
  end
  
  def select_user
    @group = Group.find(params[:id])
  end
  
  def set_users
    @group = Group.find(params[:id])
    if  @group.set_users(params[:users])
      flash[:notice] = 'Group is updated.'
    end
  
    redirect_to(:action => 'index')
  end

  def to_index(errors, id)
    if !errors.empty?
      flash[:error] = errors.full_messages.join("<br/>\n")
    end

    redirect_to(:action => 'index', :id => id)
  end

end
