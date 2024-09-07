CREATE TABLE `t_users` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `username` varchar(255) DEFAULT NULL,
    `password` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE `t_images` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL,
    `file_name` varchar(255) DEFAULT NULL,
    `file_size` bigint(20) DEFAULT NULL,
    `file_type` varchar(255) DEFAULT NULL,
    `file_extension` varchar(255) DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `s3_url` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `t_users` (`id`)
);

