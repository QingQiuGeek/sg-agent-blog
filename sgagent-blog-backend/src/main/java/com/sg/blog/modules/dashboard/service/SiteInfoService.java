package com.sg.blog.modules.dashboard.service;

import com.sg.blog.modules.user.model.vo.WebmasterVO;

public interface SiteInfoService {

    /**
     * 获取前台站长卡片信息
     */
    WebmasterVO getWebmasterInfo();

}
