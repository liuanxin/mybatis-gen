
DROP TABLE IF EXISTS `t_system_log`;
CREATE TABLE IF NOT EXISTS `t_system_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '操作人 id',
  `user_name` varchar(32) NOT NULL COMMENT '操作人名',
  `ip` varchar(64) NOT NULL COMMENT '操作地址',
  `content` VARCHAR(256) NOT NULL COMMENT '操作内容(250 个字以内)',
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='日志表';


DROP TABLE IF EXISTS `t_system_config`;
CREATE TABLE IF NOT EXISTS `t_system_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `con_key` VARCHAR(32) NOT NULL COMMENT '键',
  `con_value` VARCHAR(64) NOT NULL COMMENT '值',
  `con_comment` VARCHAR(128) NOT NULL COMMENT '说明',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `con_key` (`con_key`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='全局配置表';


DROP TABLE IF EXISTS `t_system_dict`;
CREATE TABLE IF NOT EXISTS `t_system_dict` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(32) NOT NULL COMMENT '传递类型(如: gender)',
  `type_name` VARCHAR(32) NOT NULL COMMENT '显示类型(如: 性别)',
  `value` BIGINT(20) NOT NULL COMMENT '传递值(如: 1)',
  `value_name` VARCHAR(64) NOT NULL COMMENT '显示值(如: 男)',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='系统字典表';
