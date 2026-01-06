CREATE TABLE IF NOT EXISTS `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `content` text,
  `role` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `deleted` bit(1) NOT NULL COMMENT 'Soft-delete indicator',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=250 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
;
