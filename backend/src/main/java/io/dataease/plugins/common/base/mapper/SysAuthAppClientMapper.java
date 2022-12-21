package io.dataease.plugins.common.base.mapper;

import io.dataease.plugins.common.base.domain.SysAuthAppClient;

public interface SysAuthAppClientMapper {

  /**
   * 通过cid获取信息
   * @return
   */
  SysAuthAppClient selectByPrimaryKey(String id);

  /**
   * 通过cid获取有效的配置信息信息
   * @return
   */
  SysAuthAppClient selectEnabledByPrimaryKey(String id);
}
