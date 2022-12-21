CREATE TABLE IF NOT EXISTS `sys_auth_app_client` (
    `id`          varchar(50) NOT NULL DEFAULT '' COMMENT 'ID',
    `app_name`     varchar(50) NOT NULL COMMENT '应用名称',
    `callback_url`  varchar(2000) COMMENT '回调授权url',
    `status`  int NOT NULL DEFAULT 1 COMMENT '状态；1=有效，999=无效',
    `utime_begin`   bigint(13) NOT NULL COMMENT '生效开始时间',
    `utime_end`   bigint(13) NOT NULL COMMENT '生效结束时间',
    `create_time` bigint(13)  NOT NULL COMMENT 'Create timestamp',
    `update_time` bigint(13) NOT NULL COMMENT 'Update timestamp',
    `create_by` varchar(50)  COMMENT '创建人ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci;
