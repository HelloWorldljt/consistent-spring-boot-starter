
-- 最终一致性事务事件表（h2）
CREATE TABLE common_consistent_event (
	id INT (11) NOT NULL PRIMARY KEY auto_increment,
	serial_number VARCHAR (32) DEFAULT NULL,
	app_name VARCHAR (60) NOT NULL,
	api_class VARCHAR (255) NOT NULL,
	api_method VARCHAR (255) NOT NULL,
	api_arg_types VARCHAR (255) NULL,
	api_arg_values text NULL,
	api_invocation blob NOT NULL,
	api_ret_type VARCHAR (255) NOT NULL,
	api_ret_value text DEFAULT NULL,
	concurrency boolean DEFAULT FALSE,
	retry_interval INT (11) DEFAULT 1,
	invoke_status VARCHAR (10) NOT NULL DEFAULT 0,
	retry INT (11) DEFAULT 0,
	remark VARCHAR (500) DEFAULT NULL,
	create_time datetime NOT NULL,
	update_time datetime NOT NULL,
	version INT (11) NOT NULL DEFAULT 0
);
CREATE INDEX idx_api_signature ON common_consistent_event (api_class,api_method,api_arg_types);
CREATE INDEX idx_ctime ON common_consistent_event (create_time);
CREATE UNIQUE INDEX u_idx_sn ON common_consistent_event (serial_number);




-- 最终一致性事务事件表（mysql）
CREATE TABLE common_consistent_event (
	id BIGINT (19) NOT NULL PRIMARY KEY auto_increment COMMENT '主键',
	serial_number VARCHAR (32) DEFAULT NULL COMMENT '业务流水号',
	app_name VARCHAR (60) NOT NULL COMMENT '应用名称',
	api_class VARCHAR (255) NOT NULL COMMENT '接口类',
	api_method VARCHAR (255) NOT NULL COMMENT '接口方法名',
	api_arg_types VARCHAR (255) NOT NULL COMMENT '接口方法参数类型列表',
	api_arg_values text NOT NULL COMMENT '方法参数值列表',
	api_invocation longblob NOT NULL COMMENT '方法调用快照',
	api_ret_type VARCHAR (255) NOT NULL COMMENT '接口方法返回值类型',
	api_ret_value text DEFAULT NULL COMMENT '接口方法返回值',
	concurrency TINYINT (1) DEFAULT 0 COMMENT '是否支持并行重放，并行重放不保证顺序',
	retry_interval int(11) default 1 comment '接口重试时间间隔，单位：分钟',
	invoke_status VARCHAR (10) NOT NULL DEFAULT 0 COMMENT '方法调用状态',
	retry INT (11) DEFAULT 0 COMMENT '重试次数（不算首次调用）',
	remark VARCHAR (500) DEFAULT NULL COMMENT '备注信息，一般存储异常',
	create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	version INT (11) NOT NULL DEFAULT 0 COMMENT '版本号',
	INDEX u_idx_sn(serial_number),
	INDEX idx_appname_status(app_name,invoke_status),
	INDEX idx_ctime (create_time)
) ENGINE = INNODB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8 COMMENT = '最终一致性事务事件表';


-- 0.0.3版本更新，添加字段api_invocation，方法调用参数快照，用kryo序列化成二进制数据，比fastjson效率高，反序列化问题少。
ALTER TABLE common_consistent_event 
ADD COLUMN api_invocation LONGBLOB NULL COMMENT '方法调用快照' AFTER api_arg_values,
MODIFY COLUMN api_arg_types VARCHAR (255) NULL COMMENT '接口方法参数类型列表',
MODIFY COLUMN api_arg_values text NULL COMMENT '方法参数值列表';


-- 0.0.9版本更新，添加center_id字段，用于区分在多中心情况下最终一致性数据，只处理自己中心生成的数据，保证数据一致性。
ALTER TABLE common_consistent_event
ADD COLUMN center_id VARCHAR(10) NULL COMMENT '应用中心标识' AFTER retry;