package com.skoo.stock.pub.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.skoo.orm.service.BaseService;
import com.skoo.orm.service.support.paging.PageInfo;
import com.skoo.orm.service.support.query.Condition;
import com.skoo.orm.service.support.query.Criteria;
import com.skoo.orm.service.support.query.Restrictions;
import com.skoo.stock.pub.domain.ContentAttachment;

/**
 * @description:
 * @author: autoCode
 * @history:
 */
@Component
public class  ContentAttachmentService extends BaseService<ContentAttachment> {

	/**
	 * 根据条件查询分页
	 * @param condition
	 * @return
	 */
	public PageInfo<ContentAttachment> queryPaged(Condition condition) {
		Assert.notNull(condition);
		Criteria c = condition.createCriteria(ContentAttachment.class);
		String contentId = condition.get("contentId");
		if (!StringUtils.isEmpty(contentId))
			c.add(Restrictions.like("contentId", "%" + contentId + "%"));
		return super.queryPaged(condition);
	}
}
