package com.tianque.sysadmin.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.tianque.component.dao.SessionDao;
import com.tianque.controller.mode.ClientMode;
import com.tianque.core.base.AbstractBaseDao;
import com.tianque.core.cache.constant.MemCacheConstant;
import com.tianque.core.cache.service.CacheService;
import com.tianque.core.cache.util.CacheKeyGenerator;
import com.tianque.core.util.CalendarUtil;
import com.tianque.core.util.StringUtil;
import com.tianque.core.vo.PageInfo;
import com.tianque.domain.Organization;
import com.tianque.domain.Role;
import com.tianque.domain.Session;
import com.tianque.domain.User;
import com.tianque.domain.vo.UserCountAboutVo;
import com.tianque.domain.vo.UserCountVo;
import com.tianque.domain.vo.UserVo;
import com.tianque.exception.base.BusinessValidationException;
import com.tianque.exception.base.ServiceValidationException;
import com.tianque.sysadmin.dao.OrganizationDao;
import com.tianque.sysadmin.dao.UserDao;
import com.tianque.sysadmin.service.OrganizationService;

@Repository("userDao")
public class UserDaoImpl extends AbstractBaseDao implements UserDao {

	@Autowired
	private CacheService cacheService;
	@Autowired
	private OrganizationDao organizationDao;
	@Autowired
	private OrganizationService organizationService;
	@Autowired
	private SessionDao sessionDao;

	private User loadSimpleUserFromDatabaseById(Long id) {
		return (User) getSqlMapClientTemplate().queryForObject(
				"user.getSimpleUserById", id);
	}

	@Override
	public User addUser(User user) {
		checkUser(user);
		Long id = (Long) getSqlMapClientTemplate().insert("user.addUser", user);
		return loadSimpleUserFromDatabaseById(id);
	}

	private void checkUser(User user) {
		if (null == user) {
			throw new BusinessValidationException("用户信息不能为空");
		}
		if (!StringUtil.isStringAvaliable(user.getUserName())) {
			throw new BusinessValidationException("用户名不能为空");
		}
	}

	@Override
	public void deleteUserById(Long id) {
		deleteUserRoleRelasByUserId(id);
		getSqlMapClientTemplate().delete("user.deleteUserById", id);
		cacheService.remove(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, id));
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@Override
	public User updateUser(User user) {
		checkUser(user);
		getSqlMapClientTemplate().update("user.updateUser", user);
		User result = loadSimpleUserFromDatabaseById(user.getId());
		try {
			cacheService.set(
					MemCacheConstant.USER_NAMESPACE,
					CacheKeyGenerator.generateCacheKeyFromId(User.class,
							user.getId()), user);
			invalidateFindPermissionsByUserIdNamespaceCached(user.getId());
		} catch (Exception e) {
			throw new ServiceValidationException("缓存异常", e);
		}
		return result;
	}

	private void invalidateFindPermissionsByUserIdNamespaceCached(Long userId) {
		cacheService.remove(MemCacheConstant.PERMISSION_NAMESPACE,
				MemCacheConstant.getUserPermissionKey(
						MemCacheConstant.USERPERMISSION_KEY, userId,
						MemCacheConstant.USERPERMISSION_KEYPARAME_STRING));
		// cacheService
		// .invalidateNamespaceCache(CacheNameSpace.PermissionDao_findPermissionsEnameByUserId);
		// cacheService
		// .invalidateNamespaceCache(CacheNameSpace.PermissionDao_findPermissionsByUserId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public User findUserByUserName(String userName) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userName", userName);
		List<User> result = getSqlMapClientTemplate().queryForList(
				"user.findUsers", map);
		return result.size() > 0 ? result.get(0) : null;
	}

	public List<User> findChildUsersByEnameAndOrgCode(String ename,
			String orgcode) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ename", ename);
		map.put("orgcode", orgcode);
		List<User> result = getSqlMapClientTemplate().queryForList(
				"user.findChildUsersByEnameAndOrgCode", map);
		return result;
	}

	public List<User> findChildUsersByEnameAndInternalCode(String ename,
			String internalCode) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ename", ename);
		map.put("internalCode", internalCode);
		List<User> result = getSqlMapClientTemplate().queryForList(
				"user.findChildUsersByEnameAndInternalCode", map);
		return result;
	}

	public List<User> findChildUsersByEnameAndParentOrgId(String ename,
			long orgid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ename", ename);
		map.put("orgid", orgid);
		List<User> result = getSqlMapClientTemplate().queryForList(
				"user.findChildUsersByEnameAndPrentOrgId", map);
		return result;
	}

	// modify by FCY at 2011.12.26 start
	@Override
	public void resetUserPasswordByUserName(String userName, String password) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userName", userName);
		map.put("password", password);
		getSqlMapClientTemplate().update("user.resetUserPassword2", map);
		cacheService.remove(MemCacheConstant.USER_NAMESPACE, CacheKeyGenerator
				.generateCacheKeyFromString(User.class, userName));
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	// modify by FCY at 2011.12.26 end
	@Override
	public void resetUserPasswordByUserId(Long userId, String password) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", userId);
		map.put("password", password);
		getSqlMapClientTemplate().update("user.resetUserPassword", map);
		cacheService.remove(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, userId));
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@Override
	public User getSimpleUserById(Long id) {
		User result = (User) cacheService.get(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, id));
		if (result == null) {
			result = (User) getSqlMapClientTemplate().queryForObject(
					"user.getSimpleUserById", id);
			cacheService.set(MemCacheConstant.USER_NAMESPACE,
					CacheKeyGenerator.generateCacheKeyFromId(User.class, id),
					result);
		}
		if(result!=null&&result.getRoles() == null){
			List<Role> roles = getUserRolesByUserId(result.getId());
			result.setRoles(roles);
		    cacheService.set(MemCacheConstant.USER_NAMESPACE,
					CacheKeyGenerator.generateCacheKeyFromId(User.class, id),
					result);
		}
		return result;
	}

	@Override
	public void deleteUserRoleRelasByUserId(Long userId) {
		getSqlMapClientTemplate().delete("user.deleteUserRoleRelasByUserId",
				userId);
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@Override
	public Long addUserRoleRela(Long userId, Long roleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userId", userId);
		map.put("roleId", roleId);
		Long rows = (Long) getSqlMapClientTemplate().insert(
				"user.addUserRoleRela", map);
		// invalidateFindPermissionsByUserIdNamespaceCached();
		return rows;
	}

	public void deleteUserMultiZoneByUserId(Long userId) {
		getSqlMapClientTemplate().delete("user.deleteUserMultiZoneByUserId",
				userId);
	}

	public Long addUserMultiZone(Long userId, Long orgId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("organizationId", orgId);
		map.put("userId", userId);
		return (Long) getSqlMapClientTemplate().insert("user.addUserMultiZone",
				map);
	}

	@Override
	public void updateIsLockById(Long userId, boolean isLock) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", userId);
		map.put("isLock", isLock);
		getSqlMapClientTemplate().update("user.updateIsLockById", map);

		cacheService.remove(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, userId));
	}

	@Override
	public void updateUserPassword(Long id, String currentPassword, String email) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("password", currentPassword);
		map.put("email", email);
		map.put("updatepswtime", CalendarUtil.now());
		getSqlMapClientTemplate().update("user.updateUserPassword", map);

		cacheService.remove(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, id));
	}

	protected String getRolesIdsStr(String ids) {
		if (null == ids || "null".equals(ids))
			return "";
		StringBuffer sb = new StringBuffer("");
		String[] strArr = StringUtils.split(ids, ",");
		for (String id : strArr) {
			sb.append(id);
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PageInfo<User> findUsersForPageByOrgInternalCodeAndLockState(
			String orgInternalCode, Boolean locked, User user, String roleIds,
			int pageNum, int pageSize, String sortField, String order) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgInternalCode", orgInternalCode + "%");
		map.put("queryUserName", user.getUserName());
		map.put("queryName", user.getName());
		if (null != locked) {
			map.put("isLock", locked);
		}
		map.put("lastLoginTime", CalendarUtil.format(user.getLastLoginTime()));
		map.put("timeforQuery", CalendarUtil.format(user.getTimeforQuery()));
		map.put("ignoreIsShutDownOrNot", user.getIgnoreIsShutDownOrNot());

		map.put("gpsOrNot", user.getGpsOrNot());
		map.put("fourTeamsOrNot", user.getFourTeamsOrNot());
		map.put("accountType", user.getAccountType());
		map.put("onLineState", user.getOnLineState());
		map.put("state", user.getState());

		map.put("sortField", sortField);
		map.put("order", order);
		if (null != roleIds) {
			map.put("rolesId", getRolesIdsStr(roleIds));
		}

		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUsers", map);
		int pageCount = 0;
		if ((countNum % pageSize) == 0) {
			pageCount = countNum / pageSize;
		} else {
			pageCount = countNum / pageSize + 1;
		}
		pageNum = pageNum > pageCount ? pageCount : pageNum;
		// 根据用户层级排序关联组织机构表查询
		List<User> list = null;
		if ("orgLevel".equals(sortField)) {
			list = getSqlMapClientTemplate().queryForList(
					"user.findUsersByOrgLevel", map, (pageNum - 1) * pageSize,
					pageSize);
		} else {
			list = getSqlMapClientTemplate().queryForList("user.findUsers",
					map, (pageNum - 1) * pageSize, pageSize);
		}
		List<User> result = getFullUsers(list);
		PageInfo<User> pageInfo = new PageInfo<User>();
		pageInfo.setResult(result);
		pageInfo.setTotalRowSize(countNum);
		pageInfo.setCurrentPage(pageNum > pageCount ? pageCount : pageNum);
		pageInfo.setPerPageSize(pageSize);
		return pageInfo;
	}

	private List<User> getFullUsers(List<User> users) {
		List<User> result = new ArrayList<User>();
		for (User user : users) {
			User fullUser = getFullUserById(user.getId());
			Organization fullOrg = organizationDao.getOrgAndLevelInfo(user
					.getOrganization().getId());
			fullUser.setOrganization(fullOrg);
			// 设置在线状态
			fullUser.setLogin(getUserLoginState(fullUser.getUserName(),
					Boolean.FALSE));
			result.add(fullUser);
		}
		return result;
	}

	// 获取用户的在线情况
	private boolean getUserLoginState(String userName, boolean isMobile) {
		List<Session> userSessions = sessionDao.findSessionByUserName(userName);
		boolean loginFlg = false;
		for (Session session : userSessions) {
			if (isMobile) {
				if (session.isLogin()
						&& session.getClientMode() == ClientMode.CLIENT_MODE_MOBILE) {
					loginFlg = true;
					break;
				}
			} else {
				if (session.isLogin()) {
					loginFlg = true;
					break;
				}
			}
		}
		return loginFlg;
	}

	@Override
	public int getUsedRoleCount(Long id) {
		return (Integer) getSqlMapClientTemplate().queryForObject(
				"user.usedRoleCount", id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<User> findUserForAutocompleteByLikeOrgInternalCode(
			String rootOrgInternalCode, String text, Boolean locked, int size) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("orgInnerCode", rootOrgInternalCode + "%");
		if (locked != null) {
			params.put("searchLockStatus", locked.booleanValue());
		}
		params.put("condition", text + "%");
		return getSqlMapClientTemplate().queryForList(
				"user.findUserForAutocomplete", params, 0, size);
	}

	@Override
	public void addMultizoneByUserIdAndZoneId(Long userId, Long zoneId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("zoneId", zoneId);
		map.put("userId", userId);
		getSqlMapClientTemplate().insert("user.addMultizoneByUserIdAndZoneId",
				map);
	}

	@Override
	public void deleteMultizoneByUserId(Long userId) {
		getSqlMapClientTemplate()
				.delete("user.deleteMultizoneByUserId", userId);
	}

	@Override
	public void deleteMultizoneByUserIdAndZoneIds(Long userId,
			List<Long> zoneIds) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("zoneIds", zoneIds);
		map.put("userId", userId);
		getSqlMapClientTemplate().delete(
				"user.deleteMultizoneByUserIdAndZoneIds", map);
	}

	@Override
	public void addRoleByUserIdAndRoleId(Long userId, Long roleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("roleId", roleId);
		map.put("userId", userId);
		getSqlMapClientTemplate().insert("user.addRoleByUserIdAndRoleId", map);
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@Override
	public void deleteRoleByUserIdAndRoleIds(Long userId,
			List<Long> deleteRoleIds) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("roleIds", deleteRoleIds);
		map.put("userId", userId);
		getSqlMapClientTemplate().delete("user.deleteRoleByUserIdAndRoleIds",
				map);
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PageInfo<User> findUsersForPageByOrgIdAndLockState(Long orgId,
			Boolean locked, String roleIds, int pageNum, int pageSize,
			String sortField, String order, User user) {

		Map<String, Object> map = new HashMap<String, Object>();
		if (null != locked) {
			map.put("isLock", locked);
		}
		map.put("organizationId", orgId);
		map.put("queryUserName", user.getUserName());
		map.put("queryName", user.getName());
		map.put("lastLoginTime", CalendarUtil.format(user.getLastLoginTime()));
		map.put("timeforQuery", CalendarUtil.format(user.getTimeforQuery()));
		map.put("ignoreIsShutDownOrNot", user.getIgnoreIsShutDownOrNot());

		map.put("gpsOrNot", user.getGpsOrNot());
		map.put("fourTeamsOrNot", user.getFourTeamsOrNot());
		map.put("accountType", user.getAccountType());
		map.put("onLineState", user.getOnLineState());
		map.put("state", user.getState());
		map.put("sortField", sortField);
		map.put("order", order);
		if (null != roleIds) {
			map.put("rolesId", getRolesIdsStr(roleIds));
		}

		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUsers", map);
		int pageCount = 0;
		if ((countNum % pageSize) == 0) {
			pageCount = countNum / pageSize;
		} else {
			pageCount = countNum / pageSize + 1;
		}
		pageNum = pageNum > pageCount ? pageCount : pageNum;
		List<User> list = null;

		if ("orgLevel".equals(sortField)) {
			list = getSqlMapClientTemplate().queryForList(
					"user.findUsersByOrgLevel", map, (pageNum - 1) * pageSize,
					pageSize);
		} else {
			list = getSqlMapClientTemplate().queryForList("user.findUsers",
					map, (pageNum - 1) * pageSize, pageSize);
		}
		List<User> result = getFullUsers(list);
		PageInfo<User> pageInfo = new PageInfo<User>();
		pageInfo.setResult(result);
		pageInfo.setTotalRowSize(countNum);
		pageInfo.setCurrentPage(pageNum > pageCount ? pageCount : pageNum);
		pageInfo.setPerPageSize(pageSize);
		return pageInfo;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<User> findUserForAutocompleteByParentOrgId(Long parentOrgId,
			String text, Boolean locked, int size) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("organizationId", parentOrgId);
		if (locked != null) {
			params.put("searchLockStatus", locked);
		}
		params.put("condition", text + "%");
		return getSqlMapClientTemplate().queryForList(
				"user.findUserForAutocomplete", params, 0, size);
	}

	@Override
	public int countUsersByOrgInternalCode(String orgInternalCode) {
		return (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUsersByOrgInternalCode", orgInternalCode);
	}

	@Override
	public void updateFailureTimesById(Long userId, Integer failureTimes) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", userId);
		map.put("failureTimes", failureTimes);
		getSqlMapClientTemplate().update("user.updateFailureTimesById", map);

		cacheService.remove(MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class, userId));
	}

	@Override
	public User getFullUserById(Long id) {
		User user = (User) getSqlMapClientTemplate().queryForObject("user.getFullUserById", id);
		setUserPropExtra(user);
		return user;
	}

	public void setUserPropExtra(User user)
	{
		user.setRoles(getUserRolesByUserId(user.getId()));
		user.setMultiZone(getUserMultiZoneByUserId(user.getId()));
	}
	
	@SuppressWarnings("unchecked")
	private List<Organization> getUserMultiZoneByUserId(Long userId){
		return getSqlMapClientTemplate().queryForList("user.loadUserMultiZone",userId);	
	}
	
	@SuppressWarnings("unchecked")
	private List<Role> getUserRolesByUserId(Long userId){
		return getSqlMapClientTemplate().queryForList("user.loadUserRoles",userId);
	}
	

	@Override
	public Long getUserOrgLevelByUserId(Long userId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userId", userId);
		return (Long) getSqlMapClientTemplate().queryForObject(
				"user.getUserOrgLevelByUserId", map);

	}

	@Override
	public void updateUserDetails(User user) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", user.getId());
		map.put("userName", user.getUserName());
		map.put("name", user.getName());
		map.put("mobile", user.getMobile());
		map.put("workPhone", user.getWorkPhone());
		map.put("workCompany", user.getWorkCompany());
		map.put("homePhone", user.getHomePhone());
		map.put("headerUrl", user.getHeaderUrl());
		getSqlMapClientTemplate().update("user.updateUserDetails", map);
		User result = loadSimpleUserFromDatabaseById(user.getId());
		cacheService.set(
				MemCacheConstant.USER_NAMESPACE,
				CacheKeyGenerator.generateCacheKeyFromId(User.class,
						user.getId()), result);
		// invalidateFindPermissionsByUserIdNamespaceCached();
	}

	@Override
	public List<User> findUsersByOrgId(Long id) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("organizationId", id);
		map.put("isLock", false);
		return getSqlMapClientTemplate().queryForList("user.findUsers", map);
	}

	@Override
	public PageInfo<User> findUsers(User user, Integer page, Integer rows,
			String sidx, String sord) {
		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUsersByUserCondition", user);
		List<User> list = getSqlMapClientTemplate().queryForList(
				"user.findUsersByUserCondition", user, (page - 1) * rows, rows);
		PageInfo<User> pageInfo = new PageInfo<User>();
		list = getFullUsers(list);
		pageInfo.setResult(list);
		pageInfo.setTotalRowSize(countNum);
		pageInfo.setCurrentPage(page);
		pageInfo.setPerPageSize(rows);
		return pageInfo;
	}

	@Override
	public PageInfo<User> findUsersBylockStatus(User user, Integer page,
			Integer rows, String sidx, String sord) {
		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUsersByUserConditionBylockStatus", user);
		List<User> list = getSqlMapClientTemplate().queryForList(
				"user.findUsersByUserConditionBylockStatus", user,
				(page - 1) * rows, rows);
		PageInfo<User> pageInfo = new PageInfo<User>();
		list = getFullUsers(list);
		pageInfo.setResult(list);
		pageInfo.setTotalRowSize(countNum);
		pageInfo.setCurrentPage(page);
		pageInfo.setPerPageSize(rows);
		return pageInfo;
	}

	@Override
	public User getFullUserByUserName(String userName) {
		User user = (User) getSqlMapClientTemplate().queryForObject(
				"user.getFullUserByUerName", userName);
		if(user!=null&&user.getId()!=null){
			user.setRoles(getUserRolesByUserId(user.getId()));
			user.setMultiZone(getUserMultiZoneByUserId(user.getId()));
		}
		return user;
	}

	@Override
	public void updateIsFristWorkBenchById(Long id, Boolean isFristWorkBench) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("isFristWorkBench", isFristWorkBench);
		getSqlMapClientTemplate().update("user.initWorkBench", map);
		User result = loadSimpleUserFromDatabaseById(id);
		try {
			cacheService.set(MemCacheConstant.USER_NAMESPACE,
					CacheKeyGenerator.generateCacheKeyFromId(User.class, id),
					result);
			// invalidateFindPermissionsByUserIdNamespaceCached();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public PageInfo<User> findUsersForPageInfoListByOrgInternalCodeAndRoleId(
			String orgInternalCode, Long roleId, Integer page, Integer rows,
			String sidx, String sord) {
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("orgInternalCode", orgInternalCode);
		query.put("roleId", roleId);
		query.put("sortField", "u." + sidx);
		query.put("order", sord);

		Integer countUsers = (Integer) getSqlMapClientTemplate()
				.queryForObject("user.countUsersList", query);

		int pageCount = 0;
		if ((countUsers % rows) == 0) {
			pageCount = countUsers / rows;
		} else {
			pageCount = countUsers / rows + 1;
		}
		page = page > pageCount ? pageCount : page;

		List<User> list = getSqlMapClientTemplate().queryForList(
				"user.findUsersList", query, (page - 1) * rows, rows);
		PageInfo<User> pageInfo = new PageInfo<User>();
		for(User user:list){
			setUserPropExtra(user);
		}
		pageInfo.setResult(list);
		pageInfo.setCurrentPage(page);
		pageInfo.setTotalRowSize(countUsers);
		pageInfo.setPerPageSize(rows);
		return pageInfo;
	}

	@Override
	public List<Long> findUserIdsByRoleId(Long roleId) {
		List<Long> userIds = getSqlMapClientTemplate().queryForList(
				"user.findUserIdsByRoleId", roleId);
		for (Long userId : userIds) {
			updateIsFristWorkBenchById(userId, true);
		}
		return userIds;
	}

	@Override
	public List<Long> findUserIdsByOrgIds(List<Long> orgIdList) {
		Map map = new HashMap();
		map.put("orgIdList", orgIdList);
		return getSqlMapClientTemplate().queryForList(
				"user.findUserIdsByOrgIds", map);
	}

	public List<Long> findUserIdsByRoleIds(List<Long> roleIdList) {
		Map map = new HashMap();
		map.put("roleIdList", roleIdList);
		return getSqlMapClientTemplate().queryForList(
				"user.findRoleIdsByOrgIds", map);
	}

	@Override
	public List<User> findUsersByRoleIds(List<Long> roleIdList){
		Map map = new HashMap();
		map.put("roleIdList", roleIdList);
		List<User> users = getSqlMapClientTemplate().queryForList(
				"user.findUsersByRoleIds", map);
		return users;
	}
	
	public List<Long> findUserIdsByByOrgTypeAndOrgLevelAndOrgParentId(
			Long orgTypeId, Long orgLevelId, String parentOrgInternalCode) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("parentOrgInternalCode", parentOrgInternalCode + "%");
		map.put("orgLevelId", orgLevelId);
		map.put("orgTypeId", orgTypeId);
		return getSqlMapClientTemplate().queryForList(
				"user.findUserIdsByByOrgTypeAndOrgLevelAndOrgParentId", map);
	}

	public List<Long> findUserIdsByOrgCodeAndRoleId(String orgCode, Long roleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgCode", orgCode + "%");
		map.put("roleId", roleId);
		return getSqlMapClientTemplate().queryForList(
				"user.findUserIdsByOrgCodeAndRoleId", map);

	}

	@Override
	public int countMobileUsers(UserVo userVo, int pageNum, int pageSize,
			String sortField, String sord) {
		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countMobileUser", userVo);
		return countNum;
	}

	@Override
	public PageInfo<User> findMobileUsers(UserVo userVo, int pageNum,
			int pageSize, String sortField, String sord) {
		userVo.setSortField(sortField);
		userVo.setOrder(sord);
		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countMobileUser", userVo);
		if (countNum == 0) {
			return new PageInfo<User>();
		}
		List<User> list = getSqlMapClientTemplate().queryForList(
				"user.findMobileUser", userVo, (pageNum - 1) * pageSize,
				pageSize);
		// 设置手机在线状态
		list = getFullMobileUsers(list);
		return new PageInfo<User>(pageNum, pageSize, countNum, list);
	}

	// 设置手机在线状态
	private List<User> getFullMobileUsers(List<User> users) {
		List<User> result = new ArrayList<User>();
		for (User user : users) {
			User fullUser = getFullUserById(user.getId());
			// 设置在线状态
			Organization fullOrg = organizationService.getFullOrgById(user
					.getOrganization().getId());
			fullUser.setOrganization(fullOrg);
			fullUser.setLogin(getUserLoginState(fullUser.getUserName(),
					Boolean.TRUE));
			result.add(fullUser);
		}
		return result;
	}

	@Override
	public void matchupOrganizationMobileUserByIds(Long id,
			Organization organization, int status) {
		Map map = new HashMap();
		map.put("id", id);
		map.put("organizationId", organization.getId());
		map.put("orgInternalCode", organization.getOrgInternalCode());
		map.put("status", status);
		getSqlMapClientTemplate().update(
				"user.matchupOrganizationMobileUserByIds", map);
	}

	@Override
	public int countFourthAccountUserByOrg(Organization organization) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgId", organization.getId());
		map.put("OrgInternalCode", organization.getOrgInternalCode());
		Integer count = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countFourthAccountUserByOrg", map);
		return count == null ? 0 : count;
	}

	@Override
	public User findUserByImsi(String imsi) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("imsi", imsi);
		List<User> result = getSqlMapClientTemplate().queryForList(
				"user.findUserByImsi", map);
		return result.size() > 0 ? result.get(0) : null;
	}

	@Override
	public void updateOpenOrCloseGpsById(Long id, boolean openOrClose) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("isGps", openOrClose);
		getSqlMapClientTemplate().update("user.updateOpenOrCloseGpsById", map);
	}

	@Override
	public void updateOpenOrCloseFourTeamsById(Long id, boolean openOrClose) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("isFourTeams", openOrClose);
		getSqlMapClientTemplate().update("user.updateOpenOrCloseFourTeamsById",
				map);
	}

	@Override
	public PageInfo<UserCountVo> findUserCount(Long orgId, Long orgLevel,
			String orgCode, Long accountType, Integer page, Integer rows,
			String sortField, String order) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgId", orgId);
		map.put("orgLevel", orgLevel);
		map.put("orgCode", orgCode);
		map.put("accountType", accountType);
		map.put("sortField", sortField);
		map.put("order", order);
		Integer count = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countUserCount", map);
		PageInfo<UserCountVo> pageInfo = new PageInfo<UserCountVo>();
		Integer tempCount = 0;
		if (tempCount.equals(count)) {
			return pageInfo;
		}
		int pageCount = 0;
		if ((count % rows) == 0) {
			pageCount = count / rows;
		} else {
			pageCount = count / rows + 1;
		}
		List<UserCountVo> list = getSqlMapClientTemplate().queryForList(
				"user.findUserCount", map, (page - 1) * rows, rows);
		pageInfo.setResult(list);
		pageInfo.setTotalRowSize(count);
		pageInfo.setCurrentPage(page > pageCount ? pageCount : page);
		pageInfo.setPerPageSize(rows);
		return pageInfo;
	}
	
	@Override
	public int countAllLoginCount(Long orgId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgId", orgId);
		Integer count = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.countAllLoginCount", map);
		return count == null ? 0 : count;
	}

	@Override
	public PageInfo<UserCountAboutVo> findUsersAboutUserCount(String orgInternalCode,
			Date loginBeginDate, Date loginEndDate, Date createBeginDate, Date createEndDate, int pageNum, int pageSize,
			String sortField, String order, Long pcOrMobile, Long isSelectLoginTime, Date beginActivationTime, Date endActivationTime, Long isSelectActivationTime) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orgInternalCode", orgInternalCode);
		map.put("loginBeginDate", loginBeginDate);
		map.put("loginEndDate", loginEndDate);
		map.put("createBeginDate", createBeginDate);
		map.put("createEndDate", createEndDate);
		map.put("sortField", sortField);
		map.put("order", order);
		map.put("pcOrMoblie", pcOrMobile);
		map.put("isSelectLoginTime", isSelectLoginTime);
		map.put("isSelectActivationTime", isSelectActivationTime);
		map.put("beginActivationTime", beginActivationTime);
		map.put("endActivationTime", endActivationTime);

		Integer countNum = (Integer) getSqlMapClientTemplate().queryForObject(
				"user.findUsersAboutUserCount", map);
		int pageCount = 0;
		if ((countNum % pageSize) == 0) {
			pageCount = countNum / pageSize;
		} else {
			pageCount = countNum / pageSize + 1;
		}
		pageNum = pageNum > pageCount ? pageCount : pageNum;
		// 根据用户层级排序关联组织机构表查询
		List<UserCountAboutVo> list = getSqlMapClientTemplate().queryForList(
					"user.UserCountAboutVoResult", map, (pageNum - 1) * pageSize,
					pageSize);
		getFullUserVo(list);
		PageInfo<UserCountAboutVo> pageInfo = new PageInfo<UserCountAboutVo>();
		pageInfo.setResult(list);
		pageInfo.setTotalRowSize(countNum);
		pageInfo.setCurrentPage(pageNum > pageCount ? pageCount : pageNum);
		pageInfo.setPerPageSize(pageSize);
		return pageInfo;
	}
	
	private void getFullUserVo(List<UserCountAboutVo> vos) {
		for (UserCountAboutVo vo : vos) {
			Organization fullOrg = organizationDao.getOrgAndLevelInfo(vo
					.getOrganization().getId());
			vo.setOrganization(fullOrg);
			vo.setRoles(getUserRolesByUserId(vo.getUserId()));
		}
	}
	@Override
	public User updateUserFromGridTeam(User user){
		getSqlMapClientTemplate().update("user.updateUserFromGridTeam", user);
		return loadSimpleUserFromDatabaseById(user.getId());
	}
}
