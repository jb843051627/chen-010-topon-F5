-- topon 地名与界线管理 -- schema (chen-010)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_010

CREATE TABLE IF NOT EXISTS t_topon_bound_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '勘定基准代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '勘定基准名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档桩间平距上限(米)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档桩间平距上限(米)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档桩间平距上限(米)',
  eff_start datetime DEFAULT NULL COMMENT '启用日',
  eff_end datetime DEFAULT NULL COMMENT '交棒日(不含)',
  priority int DEFAULT NULL COMMENT '取用顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '线的情形 0当家 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='界线勘定基准档线';

CREATE TABLE IF NOT EXISTS t_topon_check_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '定期检验派发单号',
  due_at datetime DEFAULT NULL COMMENT '本轮应检之期的日终截止时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前派发的自然日数',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2派不出去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定期检验派发条目';

-- 乡镇街道那一头只留一份基础档案：撤并后新卡挂不进，旧卡照查。
CREATE TABLE IF NOT EXISTS t_topon_district_doc (
  id bigint NOT NULL COMMENT '主键',
  area_code varchar(6) DEFAULT NULL COMMENT '所属区县代码(区划代码前6位)',
  site_no varchar(64) DEFAULT NULL COMMENT '乡镇街道区划代码',
  site_name varchar(128) DEFAULT NULL COMMENT '乡镇街道基础档案名称',
  site_type varchar(32) DEFAULT NULL COMMENT '所属地貌类 城镇街区/乡村区域/毗界地段',
  road_name varchar(128) DEFAULT NULL COMMENT '坐落(省—市—区县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '号段可编容量一档上限(个)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(个)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(个)',
  status int DEFAULT NULL COMMENT '档案情形 0在用 1已撤并',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_district_site_no (site_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='乡镇街道基础档案';

-- 门楼牌号段建档卡：一个段一张卡，卡跟着这条街一辈子。
CREATE TABLE IF NOT EXISTS t_topon_plate_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '门楼牌号段卡号(卡的牙口,同段第二回立卡须挡住)',
  source_no varchar(64) DEFAULT NULL COMMENT '来源单号(区里自报/市局抄送凭此号并成一条)',
  road_name varchar(128) DEFAULT NULL COMMENT '街巷名称',
  site_id bigint DEFAULT NULL COMMENT '所属乡镇街道基础档案id',
  site_no varchar(64) DEFAULT NULL COMMENT '乡镇街道区划代码(须咬得上档案)',
  qty decimal(12,2) DEFAULT NULL COMMENT '单双侧合起来可编号码总个数(个)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新编号码个数(个)',
  grade_level int DEFAULT NULL COMMENT '系统折算号段容量档 1一档 2二档 3三档(只由系统填)',
  grade_value decimal(12,2) DEFAULT NULL COMMENT '号段容量档折算值,两位小数(只由系统填)',
  odd_start_no int DEFAULT NULL COMMENT '单号侧起号',
  odd_end_no int DEFAULT NULL COMMENT '单号侧止号',
  even_start_no int DEFAULT NULL COMMENT '双号侧起号',
  even_end_no int DEFAULT NULL COMMENT '双号侧止号',
  street_view_img varchar(255) DEFAULT NULL COMMENT '街巷全貌影像路径',
  plate_sample_img varchar(255) DEFAULT NULL COMMENT '号牌实样影像路径',
  version_no int DEFAULT '1' COMMENT '现行版本号(改属/号数增减自增)',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已定档',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除/并卡归档',
  founder_name varchar(64) DEFAULT NULL COMMENT '创建人(按下立卡那一下的人,重录也不许挪)',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_plate_card_bill (bill_no),
  KEY idx_plate_card_source (source_no),
  KEY idx_plate_card_site (site_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门楼牌号段建档卡';

-- 经手人续记：一办一换手就续记一条，只追加不改写。
CREATE TABLE IF NOT EXISTS t_topon_plate_handler (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '段卡id',
  handler_name varchar(64) DEFAULT NULL COMMENT '经手人(本趟跑腿的人)',
  action varchar(32) DEFAULT NULL COMMENT '本办动作 register立卡 check核对 finalize定档 rearchive改属 readj号数增减 rerecord重录 merge并卡',
  remark varchar(500) DEFAULT NULL COMMENT '记事',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_handler_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门楼牌号段卡经手人续记';

-- 已发号码台账：老号在册/空号/续号都留一笔，三十年老号也撞得上；续过的号不许再发第二遍。
CREATE TABLE IF NOT EXISTS t_topon_plate_number (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '段卡id',
  side int NOT NULL COMMENT '号侧 1单号侧 2双号侧',
  plate_no int NOT NULL COMMENT '门牌号',
  source_type int DEFAULT '0' COMMENT '来号方式 0新编 1续编空出的老号',
  status int DEFAULT '0' COMMENT '号情形 0原号在册 1空号可续 2续号在册(终态)',
  issue_year int DEFAULT NULL COMMENT '发放年度',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plate_number (card_id, side, plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门楼牌号段已发号码台账';

-- 改属/号数增减历史：旧的一笔归历史，现行页只留最新一份。
CREATE TABLE IF NOT EXISTS t_topon_plate_card_his (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '段卡id',
  version_no int DEFAULT NULL COMMENT '归档时的旧版本号',
  change_type int DEFAULT NULL COMMENT '变动类型 1街巷改属 2号数增减 3路名更动',
  bill_no varchar(64) DEFAULT NULL COMMENT '段卡号(快照)',
  road_name varchar(128) DEFAULT NULL COMMENT '街巷名称(快照)',
  site_id bigint DEFAULT NULL COMMENT '旧档案id',
  site_no varchar(64) DEFAULT NULL COMMENT '旧区划代码',
  qty decimal(12,2) DEFAULT NULL COMMENT '旧可编号数',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '旧本年度新编数',
  grade_level int DEFAULT NULL COMMENT '旧容量档',
  grade_value decimal(12,2) DEFAULT NULL COMMENT '旧容量档折算值',
  odd_start_no int DEFAULT NULL COMMENT '单号侧起号(快照)',
  odd_end_no int DEFAULT NULL COMMENT '单号侧止号(快照)',
  even_start_no int DEFAULT NULL COMMENT '双号侧起号(快照)',
  even_end_no int DEFAULT NULL COMMENT '双号侧止号(快照)',
  create_by varchar(64) DEFAULT NULL COMMENT '归档经手人',
  create_time datetime DEFAULT NULL COMMENT '归档时间',
  PRIMARY KEY (id),
  KEY idx_card_his_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门楼牌号段卡历史版本';

-- 并卡台账：区里自报与市局抄送撞来源号时，并成一条，账上只许剩一笔。
CREATE TABLE IF NOT EXISTS t_topon_plate_merge (
  id bigint NOT NULL COMMENT '主键',
  source_no varchar(64) NOT NULL COMMENT '来源单号',
  kept_card_id bigint NOT NULL COMMENT '留存段卡id',
  dropped_card_id bigint NOT NULL COMMENT '并销段卡id(打删除标记,账上不再单列)',
  create_by varchar(64) DEFAULT NULL COMMENT '并卡经手人',
  create_time datetime DEFAULT NULL COMMENT '并卡时间',
  PRIMARY KEY (id),
  KEY idx_merge_source (source_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门楼牌号段并卡台账';

-- 区县经办岗与区县代码绑定：区县的人打开清单只见本区的段。
CREATE TABLE IF NOT EXISTS t_topon_area_user (
  id bigint NOT NULL COMMENT '主键',
  user_id bigint NOT NULL COMMENT '用户id',
  area_code varchar(6) NOT NULL COMMENT '所属区县代码(乡镇街道区划代码前6位)',
  area_name varchar(128) DEFAULT NULL COMMENT '所属区县名称',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_area_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地名区县经办岗绑定';

-- 初始档案数据（id=0 在用 / id=1 已撤并）
-- 号段可编容量上限取 40/200/600 个。
INSERT IGNORE INTO t_topon_district_doc (id, area_code, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, '361002', '361002000', '临川市城东区明德街道办事处基础档案', '城镇街区', '江南省—临川市—城东区', 40.00, 200.00, 600.00, 0, 0, 'seed', NOW()),
       (1, '361003', '361003000', '旧临川市城西乡人民政府基础档案（已撤并）', '毗界地段', '江南省—临川市—城西区', 40.00, 200.00, 600.00, 1, 0, 'seed', NOW());

-- 菜单与按钮权限（挂在「系统管理」目录 411522822607867904 之下），并授予管理员角色 488243256161730560。
INSERT IGNORE INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES (7100000000000001001, '门楼牌号段', '门楼牌号段建档卡展示', '/ToponPlateCardController/view', 0, 411522822607867904, 'topon:toponPlateCard:view', 1, 'icon icon-map', 90, 0, 'seed', NOW()),
       (7100000000000001002, '段卡集合', '门楼牌号段卡集合查询', '/ToponPlateCardController/list', 0, 7100000000000001001, 'topon:toponPlateCard:list', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001003, '段卡登记', '门楼牌号段卡登记(唯一写入口)', '/ToponPlateCardController/register', 0, 7100000000000001001, 'topon:toponPlateCard:add', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001004, '段卡修改', '门楼牌号段卡改办(改属/号数增减)', '/ToponPlateCardController/edit', 0, 7100000000000001001, 'topon:toponPlateCard:edit', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001005, '段卡删除', '门楼牌号段卡删除', '/ToponPlateCardController/remove', 0, 7100000000000001001, 'topon:toponPlateCard:remove', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001006, '段卡核对', '门楼牌号段卡核对', '/ToponPlateCardController/check', 0, 7100000000000001001, 'topon:toponPlateCard:check', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001007, '段卡定档', '门楼牌号段卡定档', '/ToponPlateCardController/finalize', 0, 7100000000000001001, 'topon:toponPlateCard:finalize', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001008, '段卡发号', '新编/续编号码发放', '/ToponPlateCardController/issue', 0, 7100000000000001001, 'topon:toponPlateCard:issue', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001009, '全市数据范围', '市局视角见全市段卡', NULL, 0, 7100000000000001001, 'topon:plate:scope:city', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001010, '乡镇街道档案', '乡镇街道基础档案展示', '/ToponDistrictDocController/view', 0, 411522822607867904, 'topon:toponDistrictDoc:view', 1, 'icon icon-building', 91, 0, 'seed', NOW()),
       (7100000000000001011, '档案集合', '乡镇街道基础档案集合查询', '/ToponDistrictDocController/list', 0, 7100000000000001010, 'topon:toponDistrictDoc:list', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001012, '档案新增', '乡镇街道基础档案新增', '/ToponDistrictDocController/add', 0, 7100000000000001010, 'topon:toponDistrictDoc:add', 2, '', NULL, 0, 'seed', NOW()),
       (7100000000000001013, '档案撤并', '乡镇街道基础档案撤并', '/ToponDistrictDocController/abolish', 0, 7100000000000001010, 'topon:toponDistrictDoc:abolish', 2, '', NULL, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 7200000000000010001 + n, 488243256161730560, 7100000000000001001 + n, 'seed', NOW()
FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
      UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) seq;
