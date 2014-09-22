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

  def create_form
    @group = Group.new
    render :partial => 'groups/create_form'
  end

  def edit_form
    require_parameters :id
    @group = Group.find(params[:id])
    render :partial => 'groups/edit_form'
  end

  def create
    verify_post_request
    group = Group.new(params[:group])
    if group.save
      flash[:notice] = 'The new group is created.'
      render :text => 'ok', :status => 200
    else
      @group = group
      @errors = []
      group.errors.full_messages.each{|msg| @errors<<msg}
      render :partial => 'groups/create_form', :status => 400
    end
  end

  def update
    verify_post_request
    require_parameters :id

    @group = Group.find(params[:id])
    if @group.update_attributes(params[:group])
      flash[:notice] = 'Group is updated.'
      render :text => 'ok', :status => 200
    else
      @errors = []
      @group.errors.full_messages.each{|msg| @errors<<msg}
      render :partial => 'groups/edit_form', :status => 400
    end
  end

  def delete
    verify_post_request
    require_parameters :id
    group = Group.find(params[:id])
    call_backend do
      Internal.permission_templates.removeGroupFromTemplates(group.name)
      if group.destroy
        flash[:notice] = 'Group is deleted.'
      end
    end
    to_index(group.errors, nil)
  end

  # TO BE REMOVED ?
  def select_user
    @group = Group.find(params[:id])
    render :partial => 'groups/select_user'
  end

  # TO BE REMOVED ?
  def set_users
    @group = Group.find(params[:id])
    if  @group.set_users(params[:users])
      flash[:notice] = 'Group is updated.'
    end
  
    redirect_to(:action => 'index')
  end


  # Used for selection of group members
  #
  # GET /groups/search_users?group=<group_name>&page=1&pageSize=10
  #
  #
  def search_users
    require_parameters :group, :page, :pageSize

    group = Group.first(:conditions => {:name => params[:group]})
    group_id = group.id
    selected = params[:selected]||'all'
    query = params[:query]
    page_id = params[:page].to_i
    page_size = [params[:pageSize].to_i, 1000].min

    conditions = ['users.active=?']
    condition_values = [true]
    if selected=='selected'
      conditions << "groups_users.group_id=?"
      condition_values << group_id
    elsif selected=='deselected'
      conditions << "groups_users.group_id is null"
    end
    if query
      conditions << "users.name like ?"
      condition_values << "%#{query}%"
    end

    users = User.find(:all, 
      :select => 'users.id,users.name,users.login,groups_users.group_id',
      :joins => "left join groups_users on users.id=groups_users.user_id and groups_users.group_id=#{group_id}",
      :conditions => [conditions.join(' and ')].concat(condition_values),
      :offset => (page_id-1) * page_size,
      :limit => page_size + 1,
      :order => 'users.name')

    more = false
    if users.size>page_size
      users = users[0...page_size]
      more = true
    end

    respond_to do |format|
      format.json { 
        render :json => {
          :more => more,
          :results => users.map {|user| {:id => user.id, :name => user.name, :login => user.login, :selected => (user.group_id != nil)}}
        } 
      }
    end
  end

  def add_member
    verify_post_request
    require_parameters :group, :user

    user = User.find(:first, :conditions => {:login => params[:user], :active => true})
    group = Group.first(:conditions => {:name => params[:group]})
    status = 400
    if user && group
      group.users << user
      status = 200 if group.save
    end
    render :status => status, :text => '{}'
  end

  def remove_member
    verify_post_request
    require_parameters :group, :user

    user = User.find(:first, :conditions => {:login => params[:user], :active => true})
    user_id = user.id
    group = Group.first(:conditions => {:name => params[:group]})
    status = 400
    if group
      user_from_group = group.users.find(user_id)
      if user_from_group
        group.users.delete(user_from_group)
        status = 200 if group.save
      else
        status = 200  
      end
    end
    render :status => status, :text => '{}'
  end

  private

  def to_index(errors, id)
    flash[:error] = errors.full_messages.join("<br/>\n") unless errors.empty?
    redirect_to(:action => 'index', :id => id)
  end

end
