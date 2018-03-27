package com.skoo.stock.sys.service;

import com.skoo.orm.service.BaseService;
import com.skoo.orm.service.support.paging.PageInfo;
import com.skoo.orm.service.support.query.Condition;
import com.skoo.orm.service.support.query.Criteria;
import com.skoo.orm.service.support.query.Restrictions;
import com.skoo.stock.sys.domain.SysroleMenu;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: autoCode
 * @history:
 */
@Component
public class SysroleMenuService extends BaseService<SysroleMenu> {

    /**
     * 根据条件查询分页
     *
     * @param condition
     * @return
     */
    public PageInfo<SysroleMenu> queryPaged(Condition condition) {
        Assert.notNull(condition);
        Criteria c = condition.createCriteria(SysroleMenu.class);
        String roleId = condition.get("roleId");
        if (!StringUtils.isEmpty(roleId))
            c.add(
                    Restrictions.like("roleId", "%" + roleId
                            + "%"));
        return super.queryPaged(condition);
    }

    /**
     * 查询有效菜单
     *
     * @param map 参数
     * @return
     */
    public List<String> selectIdList(Map map) {
        Assert.notNull(map);
        return super.getSqlSession().selectList("SysroleMenu_idlist", map);
    }

    /**
     * 查询无效菜单
     *
     * @param map 参数
     * @return
     */
    public List<String> selectIdListDel(Map map) {
        Assert.notNull(map);
        return super.getSqlSession().selectList("SysroleMenu_idlist_del", map);
    }

    /**
     * 批量新增角色菜单数据
     *
     * @param map 参数
     * @return
     */
    public int insertMenuList(Map<String, Object> map) {
        Assert.notNull(map);
        return super.getSqlSession().insert("SysroleMenu_insert_batch", map);
    }

    /**
     * 批量逻辑新增角色菜单数据
     *
     * @param map 参数
     * @return
     */
    public int insertMenuListLogic(Map<String, Object> map) {
        Assert.notNull(map);
        return super.getSqlSession().insert("SysroleMenu_insert_batch_logic", map);
    }

    /**
     * 批量删除角色菜单数据
     *
     * @param map 参数
     * @return
     */
    public int deleteMenuListLogic(Map<String, Object> map) {
        Assert.notNull(map);
        return super.getSqlSession().insert("SysroleMenu_delete_batch_logic", map);
    }

    /**
     * 批量处理角色菜单数据
     *
     * @param id   角色ID
     * @param diff 参数
     * @return
     */
    public void processMenuList(long id, List<List<String>> diff, List<List<String>> diffDel) {
        Assert.notNull(diff);

        // 删除
        if (diff.get(1).size() > 0) {
            Map<String, Object> delMap = new HashMap<>();
            delMap.put("roleId", id);
            delMap.put("list", diff.get(1));
            //删除数据
            this.deleteMenuListLogic(delMap);
        }

        // 逻辑新增
        if (diffDel.get(2).size() > 0) {
            Map<String, Object> insMap = new HashMap<>();
            insMap.put("roleId", id);
            insMap.put("list", diffDel.get(2));
            // 新增数据
            this.insertMenuListLogic(insMap);
        }

        // 物理新增
        if (diffDel.get(0).size() > 0) {
            Map<String, Object> insMap = new HashMap<>();
            insMap.put("roleId", id);
            insMap.put("list", diffDel.get(0));
            // 新增数据
            this.insertMenuList(insMap);
        }
    }
}
