/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE IF NOT EXISTS `s-pay-mall` DEFAULT CHARACTER SET utf8mb4;
USE `s-pay-mall`;

DROP TABLE IF EXISTS `role_permission`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `permission`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order_main`;
DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `mall_user`;

-- 转储表 pay_order
DROP TABLE IF EXISTS `pay_order`;

CREATE TABLE `pay_order`
(
`id`           int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
`user_id`      varchar(32) CHARACTER SET utf8mb4 NOT NULL COMMENT '用户ID',
`product_id`   varchar(16) NOT NULL COMMENT '商品ID',
`product_name` varchar(64) NOT NULL COMMENT '商品名称',
`order_id`     varchar(32) CHARACTER SET utf8mb4 NOT NULL COMMENT '关联订单ID',
`order_time`   datetime NOT NULL COMMENT '下单时间',
`total_amount` decimal(8, 2) unsigned DEFAULT NULL COMMENT '订单金额',
`status`       varchar(32) CHARACTER SET utf8mb4 NOT NULL COMMENT '订单状态；create-创建完成、pay_wait-等待支付、pay_success-支付成功、deal_done-交易完成、close-订单关单',
`pay_url`      varchar(2014) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '支付信息',
`pay_time`     datetime DEFAULT NULL COMMENT '支付时间',
`create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_order_id` (`order_id`),
KEY `idx_user_id_product_id` (`user_id`, `product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;

-- ==================== mall_user 表 ====================
-- 商城用户表 - 存储系统用户的基本信息（核心登录信息）
-- 第三方绑定信息存储在 user_binding 表中
CREATE TABLE `mall_user` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID，自增主键',
`username` varchar(64) NOT NULL COMMENT '用户名，唯一标识，用于登录',
`password` varchar(128) NOT NULL COMMENT '密码，采用BCrypt加密存储',
`status` tinyint NOT NULL DEFAULT 1 COMMENT '用户状态：1正常 0禁用',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== user_binding 表 ====================
-- 用户第三方联合绑定表 - 存储用户与第三方平台的绑定关系
-- 支持微信公众号、支付宝、手机号等多种登录方式
CREATE TABLE `user_binding` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '绑定ID，自增主键',
`user_id` bigint unsigned NOT NULL COMMENT '关联 mall_user 表的主键',
`identity_type` varchar(32) NOT NULL COMMENT '登录类型: WECHAT_MP / ALIPAY / PHONE',
`identifier` varchar(128) NOT NULL COMMENT '唯一标识: 微信存储 OpenID, 手机号存号码',
`credential` varchar(128) DEFAULT NULL COMMENT '凭证(预留，公众号通常为空)',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uk_type_identifier` (`identity_type`, `identifier`),
KEY `idx_user_id` (`user_id`),
CONSTRAINT `fk_user_binding_user` FOREIGN KEY (`user_id`) REFERENCES `mall_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方联合绑定表';

-- ==================== role 表 ====================
-- 角色表 - 存储系统角色定义，用于RBAC权限控制
-- 支持管理员、会员等角色划分
CREATE TABLE `role` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '角色ID，自增主键',
`role_code` varchar(32) NOT NULL COMMENT '角色编码，如ADMIN、MEMBER',
`role_name` varchar(64) NOT NULL COMMENT '角色名称，如管理员、会员',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== permission 表 ====================
-- 权限表 - 存储系统权限定义，用于细粒度权限控制
-- 定义系统中各个功能模块的访问权限
CREATE TABLE `permission` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '权限ID，自增主键',
`perm_code` varchar(64) NOT NULL COMMENT '权限编码，如user:list、product:create',
`perm_name` varchar(64) NOT NULL COMMENT '权限名称，如用户列表、商品创建',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== user_role 表 ====================
-- 用户角色关联表 - 建立用户与角色的多对多关系
-- 一个用户可以拥有多个角色，一个角色可以被多个用户拥有
CREATE TABLE `user_role` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '关联ID，自增主键',
`user_id` bigint unsigned NOT NULL COMMENT '用户ID，关联mall_user表',
`role_id` bigint unsigned NOT NULL COMMENT '角色ID，关联role表',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_user_role` (`user_id`,`role_id`),
CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `mall_user` (`id`),
CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== role_permission 表 ====================
-- 角色权限关联表 - 建立角色与权限的多对多关系
-- 一个角色可以拥有多个权限，一个权限可以被多个角色拥有
CREATE TABLE `role_permission` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '关联ID，自增主键',
`role_id` bigint unsigned NOT NULL COMMENT '角色ID，关联role表',
`permission_id` bigint unsigned NOT NULL COMMENT '权限ID，关联permission表',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_role_perm` (`role_id`,`permission_id`),
CONSTRAINT `fk_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
CONSTRAINT `fk_role_perm_perm` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== category 表 ====================
-- 商品分类表 - 存储商品分类信息
-- 用于商品的分类管理和展示，如数码、服装、食品等分类
CREATE TABLE `category` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分类ID，自增主键',
`name` varchar(64) NOT NULL COMMENT '分类名称',
`status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== product 表 ====================
-- 商品表 - 存储商品的详细信息
-- 包括商品名称、描述、价格、库存、所属分类等
CREATE TABLE `product` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '商品ID，自增主键',
`category_id` bigint unsigned NOT NULL COMMENT '分类ID，关联category表',
`name` varchar(128) NOT NULL COMMENT '商品名称',
`description` varchar(512) DEFAULT NULL COMMENT '商品描述',
`price` decimal(10,2) NOT NULL COMMENT '商品价格',
`stock` int NOT NULL DEFAULT 0 COMMENT '库存数量',
`category` varchar(32) NOT NULL DEFAULT '数码产品' COMMENT '商品分类: 食品饮料/服装配饰/数码产品',
`status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
KEY `idx_product_category` (`category_id`),
KEY `idx_category` (`category`),
CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==================== cart_item 表 ====================
-- 购物车明细表 - 存储用户购物车中的商品信息
-- 记录用户选择的商品、数量等，支持批量下单
CREATE TABLE `cart_item` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '购物车项ID，自增主键',
`user_id` bigint unsigned NOT NULL COMMENT '用户ID，关联mall_user表',
`product_id` bigint unsigned NOT NULL COMMENT '商品ID，关联product表',
`quantity` int NOT NULL DEFAULT 1 COMMENT '商品数量',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_user_product` (`user_id`,`product_id`),
CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `mall_user` (`id`),
CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== order_main 表 ====================
-- 订单主表 - 存储订单的基本信息
-- 包括订单编号、用户ID、订单金额、订单状态等
CREATE TABLE `order_main` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID，自增主键',
`order_no` varchar(32) NOT NULL COMMENT '订单编号，唯一标识',
`user_id` bigint unsigned NOT NULL COMMENT '用户ID，关联mall_user表',
`total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
`status` varchar(32) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态：CREATED-已创建、PAID-已支付、SHIPPED-已发货、COMPLETED-已完成、CANCELLED-已取消',
`address` varchar(256) DEFAULT NULL COMMENT '收货地址',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uq_order_no` (`order_no`),
KEY `idx_order_user_status` (`user_id`,`status`),
CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `mall_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== order_item 表 ====================
-- 订单明细表 - 存储订单中的商品明细
-- 记录每个订单包含的商品、价格、数量等信息
CREATE TABLE `order_item` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单项ID，自增主键',
`order_id` bigint unsigned NOT NULL COMMENT '订单ID，关联order_main表',
`product_id` bigint unsigned NOT NULL COMMENT '商品ID，关联product表',
`product_name` varchar(128) NOT NULL COMMENT '商品名称（快照）',
`price` decimal(10,2) NOT NULL COMMENT '商品价格（快照）',
`quantity` int NOT NULL COMMENT '商品数量',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`),
KEY `idx_item_order` (`order_id`),
CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `order_main` (`id`),
CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 初始化数据 ====================

-- 角色数据
INSERT INTO `role`(`id`, `role_code`, `role_name`) VALUES
(1, 'ADMIN', '管理员'),
(2, 'MEMBER', '会员');

-- 用户数据 - 管理员账号：admin / 123456
INSERT INTO `mall_user`(`id`, `username`, `password`, `status`) VALUES
(1, 'admin', '$2a$10$nx2T4i2eVpyqKNq.Xs6FGeQQEJwHYcwBC.Ilt8hXKGkoca.ctw6qy', 1);

-- 用户角色关联
INSERT INTO `user_role`(`user_id`, `role_id`) VALUES
(1, 1); -- admin 具有管理员角色

-- 权限数据
INSERT INTO `permission`(`id`, `perm_code`, `perm_name`) VALUES
(1, 'user:list', '用户列表'),
(2, 'user:create', '创建用户'),
(3, 'user:update', '修改用户'),
(4, 'user:delete', '删除用户'),
(5, 'product:list', '商品列表'),
(6, 'product:create', '创建商品'),
(7, 'product:update', '修改商品'),
(8, 'product:delete', '删除商品'),
(9, 'order:list', '订单列表'),
(10, 'order:view', '查看订单'),
(11, 'order:update', '修改订单'),
(12, 'order:delete', '删除订单');

-- 角色权限关联
INSERT INTO `role_permission`(`role_id`, `permission_id`) VALUES
                  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12);

-- 商品分类数据
INSERT INTO `category`(`id`, `name`, `status`) VALUES
(1, '数码产品', 1),
(2, '服装配饰', 1),
(3, '食品饮料', 1);

-- 商品数据
DELETE FROM `product`;
INSERT INTO `product`(`id`, `category_id`, `name`, `description`, `price`, `stock`, `category`, `status`) VALUES
(1, 1, 'iPhone 17 Pro Max 256GB', '2026最新苹果旗舰手机，支持全本地端侧超大模型与全新影像系统', 9999.00, 50, '数码产品', 1),
(2, 1, 'MacBook Air 14英寸 M4', '搭载最新M4芯片，16GB统一内存起步，极致轻薄与全栈编程开发利器', 9499.00, 30, '数码产品', 1),
(3, 1, 'RTX 5060 游戏主机 (i5-13400F/32G D5)', '专业电竞游戏主机，影驰 RTX 5060 8GB 独显，爽玩2026所有3A大作', 6499.00, 20, '数码产品', 1),
(4, 2, 'Nike Air Jordan 1 2026复刻款', '经典篮球鞋，皮革材质，气垫缓震，潮流百搭', 1299.00, 80, '服装配饰', 1),
(5, 2, '优衣库 2026 高科技防风羽绒服', '轻薄保暖羽绒服，防风防水，支持可收纳设计', 599.00, 200, '服装配饰', 1),
(6, 3, '星巴克星冰乐 2026 限定礼盒', '精选咖啡豆礼盒套装，含拿铁、摩卡、焦糖三种口味', 299.00, 150, '食品饮料', 1),
(7, 3, '三只松鼠坚果大礼包', '混合坚果礼盒，包含核桃、杏仁、腰果等多种坚果', 168.00, 300, '食品饮料', 1),
(8, 1, '华为智能手表 GT5', '2026全新运动健康手表，脉搏波特征辨识，超长续航', 1699.00, 60, '数码产品', 1);

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
