/**
 * tianque-com.tianque.core.user.dao-ISysMenuDao.java Created on Mar 29, 2009
 * Copyright (c) 2010 by 杭州天阙科技有限公司
 */
package com.tianque.sysadmin.dao;

import java.util.List;

import com.tianque.core.vo.PageInfo;
import com.tianque.domain.Permission;
import com.tianque.mobile.vo.PermissionVo;

/**
 * Title: ***<br>
 * 
 * @author <a href=mailto:nifeng@hztianque.com>倪峰</a><br>
 * @description ***<br/>
 * @version 1.0
 */
public interface PermissionDao {
	/**
	 * 分页查询权限
	 * 
	 * @return
	 */
	public PageInfo findAllPermissionsForPage(int pageNum, int pageSize);

	public List<PermissionVo> findUserAllPermissionEnameAndCnameByUserId(
			Long userId);

	public List<Permission> findPermissionsByRoleId(Long roleId);

	public List<String> findPermissionsEnameByUserId(Long userId);

	public Permission addPermission(Permission permission);

	public Permission findPermissionByEname(String ename);

	public List<Permission> findPermissionsByEnames(String[] enames);

	public Permission findPermissionsByIdFetchParent(Long permissionId);

	public List<Permission> getRootPermissions();

	List<Permission> getPermissionByParentId(Long parentId);

	public List<Permission> getPermissionByParentIdAnduserId(Long parentId,
			Long userId);

	public Permission updatePermissionName(Permission permission);

	public boolean updatePermissionIndexId(Long id, Long indexId);

	public Permission getPermissionByParentIdAndIndexId(Long parentId,
			Long indexId);

	public Permission getSimplePermissionById(Long id);

	public List<Permission> findPermissionsByPermissionName(String name,
			int pageNum, int pageSize);

	public Permission getPermissionByNullParentIdAndIndexId(Long indexId);

	public Long countPermissionByParentId(Long parentId);

	public List<Permission> findMenuPermissions();

	public List<Permission> findMenuPermissions(String nodeId);

	public List<Permission> findMenuPermissions(Long userId);

	public List<Permission> findMenuPermissions(Long userId, String nodeId);

	public long getPermissionIndexIdById(Long id);

	public Permission findPermissionByNormalUrl(String normalUrl);

	public Integer getPermissionHasChildren(Long id);

	public List<Permission> findUserAllPermissionByUserIdAndPermissionEname(
			Long userId, String ename);

	public List<Permission> findMenuLeaderPermissions(String nodeId);

	public List<Permission> getLeaderPermissionByParentId(Long parentId);

	public List<Permission> getLeaderPermissionByParentId(Long parentId,
			Long userId);

	public List<Permission> findMenuLeaderPermissionsByUserId(Long parentId);

	public Integer getLeaderPermissionHasChildren(Long id);

	/**
	 * 获取当前用户的所有权限列表
	 * 
	 * @param userId
	 * @return
	 */
	public List<Permission> findAllPermissionsByCurrentUserRoleId(Long userId,
			Integer permissiontype);

	/**
	 * 由于岗位新增修改时不需要领导视图的权限所以为权限管理新增一个方法
	 * 
	 * @param parentId
	 * @return
	 */
	public List<Permission> getPermissionByParentIdToPermissionTree(
			Long parentId);

	/************************** 组织机构迁移合并方法 *************************/
	/***
	 * 通过权限的ID查询所有子权限
	 * 
	 * @param permissionId
	 * @return
	 */
	public List<Permission> getAllChildPermissionsByParentId(Long permissionId);

	public List<Permission> findUserAllPermissionByUserIdAndPermissionEnames(
			Long userId, String[] enames);

	/**
	 * @param userId
	 * @return
	 */
	public List<Permission> findAllPermissionMobile();
}
