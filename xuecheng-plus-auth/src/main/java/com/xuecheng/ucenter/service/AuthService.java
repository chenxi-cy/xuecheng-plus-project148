package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @author mechrev
 * @ClassName AuthService
 * @description: TODO
 * @date 2023年02月05日
 * @version: 1.0
 */
public interface AuthService {
    XcUserExt execute(AuthParamsDto authParamsDto);
}
