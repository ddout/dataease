package io.dataease.plugins.common.base.domain;

import java.io.Serializable;
import lombok.Data;

/**
 * 系统auth-code授权配置信息
 */
@Data
public class SysAuthAppClient implements Serializable {

  /**
   * id，即cid
   */
  private String id;
  /**
   * 应用名称
   */
  private String appame;
  /**
   * 回调授权url
   */
  private String callbackUrl;
  /**
   * 状态；1=有效，999=无效
   */
  private int status;
  /**
   * 生效开始时间
   */
  private long utimeBegin;
  /**
   * 生效结束时间
   */
  private long utimeEnd;
  /**
   * 创建时间
   */
  private long createTime;
  /**
   * 更新时间
   */
  private long updateTime;
  /**
   * 创建人ID
   */
  private String createBy;

  private static final long serialVersionUID = 1L;
}
