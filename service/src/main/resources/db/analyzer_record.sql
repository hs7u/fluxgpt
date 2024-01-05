drop table if exists `product_advice_record`;

create table `product_advice_record` (
    `id` bigint not null auto_increment,
    `code` varchar(64) not null comment '編碼',
    `content` mediumtext not null comment 'AI回答',
    `create_time` timestamp not null default current_timestamp ,
    `update_time` timestamp on update current_timestamp ,
    `type` tinyint(1) not null default 0 comment '0: AI回答; 1:人工修正',
    primary key (`id`) using btree,
    key `idx_product_advice_record_code_ct` (`code`, `create_time`) using btree
) engine=innodb auto_increment=100019 default charset=utf8 row_format=compact comment='AI回答' ;