-- 租户表
-- ----------------------------
create table sys_tenant
(
    id                bigint(20)    not null        comment 'id',
    tenant_id         varchar(20)   not null        comment '租户编号',
    contact_user_name varchar(20)                   comment '联系人',
    contact_phone     varchar(20)                   comment '联系电话',
    company_name      varchar(30)                   comment '企业名称',
    license_number    varchar(30)                   comment '统一社会信用代码',
    address           varchar(200)                  comment '地址',
    intro             varchar(200)                  comment '企业简介',
    domain            varchar(200)                  comment '域名',
    remark            varchar(200)                  comment '备注',
    package_id        bigint(20)                    comment '租户套餐编号',
    expire_time       datetime                      comment '过期时间',
    account_count     int           default -1      comment '用户数量（-1不限制）',
    status            char(1)       default '0'     comment '租户状态（0正常 1停用）',
    del_flag          char(1)       default '0'     comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)                    comment '创建部门',
    create_by         bigint(20)                    comment '创建者',
    create_time       datetime                      comment '创建时间',
    update_by         bigint(20)                    comment '更新者',
    update_time       datetime                      comment '更新时间',
    primary key (id)
) engine=innodb comment = '租户表';


-- ----------------------------
-- 初始化-租户表数据
-- ----------------------------

insert into sys_tenant values(1, '000000', '管理组', '15888888888', 'XXX有限公司', null, null, '多租户通用后台管理管理系统', null, null, null, null, -1, '0', '0', 103, 1, sysdate(), null, null);


-- ----------------------------
-- 租户套餐表
-- ----------------------------
create table sys_tenant_package (
    package_id              bigint(20)     not null    comment '租户套餐id',
    package_name            varchar(20)                comment '套餐名称',
    menu_ids                varchar(3000)              comment '关联菜单id',
    remark                  varchar(200)               comment '备注',
    menu_check_strictly     tinyint(1)     default 1   comment '菜单树选择项是否关联显示',
    status                  char(1)        default '0' comment '状态（0正常 1停用）',
    del_flag                char(1)        default '0' comment '删除标志（0代表存在 1代表删除）',
    create_dept             bigint(20)                 comment '创建部门',
    create_by               bigint(20)                 comment '创建者',
    create_time             datetime                   comment '创建时间',
    update_by               bigint(20)                 comment '更新者',
    update_time             datetime                   comment '更新时间',
    primary key (package_id)
) engine=innodb comment = '租户套餐表';


-- ----------------------------
-- 1、部门表
-- ----------------------------
create table sys_dept (
    dept_id           bigint(20)      not null                   comment '部门id',
    tenant_id         varchar(20)     default '000000'           comment '租户编号',
    parent_id         bigint(20)      default 0                  comment '父部门id',
    ancestors         varchar(500)    default ''                 comment '祖级列表',
    dept_name         varchar(30)     default ''                 comment '部门名称',
    dept_category     varchar(100)    default null               comment '部门类别编码',
    order_num         int(4)          default 0                  comment '显示顺序',
    leader            bigint(20)      default null               comment '负责人',
    phone             varchar(11)     default null               comment '联系电话',
    email             varchar(50)     default null               comment '邮箱',
    status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (dept_id)
) engine=innodb comment = '部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------


insert into sys_dept values(100, '000000', 0,   '0',          'XXX科技',   null,0, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(101, '000000', 100, '0,100',      '深圳总公司', null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(102, '000000', 100, '0,100',      '长沙分公司', null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(103, '000000', 101, '0,100,101',  '研发部门',   null,1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(104, '000000', 101, '0,100,101',  '市场部门',   null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(105, '000000', 101, '0,100,101',  '测试部门',   null,3, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(106, '000000', 101, '0,100,101',  '财务部门',   null,4, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(107, '000000', 101, '0,100,101',  '运维部门',   null,5, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(108, '000000', 102, '0,100,102',  '市场部门',   null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);
insert into sys_dept values(109, '000000', 102, '0,100,102',  '财务部门',   null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, sysdate(), null, null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
create table sys_user (
    user_id           bigint(20)      not null                   comment '用户ID',
    tenant_id         varchar(20)     default '000000'           comment '租户编号',
    dept_id           bigint(20)      default null               comment '部门ID',
    user_name         varchar(30)     not null                   comment '用户账号',
    nick_name         varchar(30)     not null                   comment '用户昵称',
    user_type         varchar(10)     default 'sys_user'         comment '用户类型（sys_user系统用户）',
    email             varchar(50)     default ''                 comment '用户邮箱',
    phonenumber       varchar(11)     default ''                 comment '手机号码',
    sex               char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
    avatar            bigint(20)                                 comment '头像地址',
    password          varchar(100)    default ''                 comment '密码',
    status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    login_ip          varchar(128)    default ''                 comment '最后登录IP',
    login_date        datetime                                   comment '最后登录时间',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (user_id)
) engine=innodb comment = '用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user values(1, '000000', 103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, '管理员');
insert into sys_user values(3, '000000', 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), 3, sysdate(), null);
insert into sys_user values(4, '000000', 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), 4, sysdate(), null);

-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
create table sys_post
(
    post_id       bigint(20)      not null                   comment '岗位ID',
    tenant_id     varchar(20)     default '000000'           comment '租户编号',
    dept_id       bigint(20)      not null                   comment '部门id',
    post_code     varchar(64)     not null                   comment '岗位编码',
    post_category varchar(100)    default null               comment '岗位类别编码',
    post_name     varchar(50)     not null                   comment '岗位名称',
    post_sort     int(4)          not null                   comment '显示顺序',
    status        char(1)         not null                   comment '状态（0正常 1停用）',
    create_dept   bigint(20)      default null               comment '创建部门',
    create_by     bigint(20)      default null               comment '创建者',
    create_time   datetime                                   comment '创建时间',
    update_by     bigint(20)      default null               comment '更新者',
    update_time   datetime                                   comment '更新时间',
    remark        varchar(500)    default null               comment '备注',
    primary key (post_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, '000000', 103, 'ceo',  null, '董事长',    1, '0', 103, 1, sysdate(), null, null, '');
insert into sys_post values(2, '000000', 100, 'se',   null, '项目经理',  2, '0', 103, 1, sysdate(), null, null, '');
insert into sys_post values(3, '000000', 100, 'hr',   null, '人力资源',  3, '0', 103, 1, sysdate(), null, null, '');
insert into sys_post values(4, '000000', 100, 'user', null, '普通员工',  4, '0', 103, 1, sysdate(), null, null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
create table sys_role (
    role_id              bigint(20)      not null                   comment '角色ID',
    tenant_id            varchar(20)     default '000000'           comment '租户编号',
    role_name            varchar(30)     not null                   comment '角色名称',
    role_key             varchar(100)    not null                   comment '角色权限字符串',
    role_sort            int(4)          not null                   comment '显示顺序',
    data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
    menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
    dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
    status               char(1)         not null                   comment '角色状态（0正常 1停用）',
    del_flag             char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept          bigint(20)      default null               comment '创建部门',
    create_by            bigint(20)      default null               comment '创建者',
    create_time          datetime                                   comment '创建时间',
    update_by            bigint(20)      default null               comment '更新者',
    update_time          datetime                                   comment '更新时间',
    remark               varchar(500)    default null               comment '备注',
    primary key (role_id)
) engine=innodb comment = '角色信息表';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values(1, '000000', '超级管理员',  'superadmin',  1, 1, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '超级管理员');
insert into sys_role values(3, '000000', '本部门及以下', 'test1', 3, 4, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '');
insert into sys_role values(4, '000000', '仅本人',      'test2', 4, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '');

-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
create table sys_menu (
    menu_id           bigint(20)      not null                   comment '菜单ID',
    menu_name         varchar(50)     not null                   comment '菜单名称',
    parent_id         bigint(20)      default 0                  comment '父菜单ID',
    order_num         int(4)          default 0                  comment '显示顺序',
    path              varchar(200)    default ''                 comment '路由地址',
    component         varchar(255)    default null               comment '组件路径',
    query_param       varchar(255)    default null               comment '路由参数',
    is_frame          int(1)          default 1                  comment '是否为外链（0是 1否）',
    is_cache          int(1)          default 0                  comment '是否缓存（0缓存 1不缓存）',
    menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
    visible           char(1)         default 0                  comment '显示状态（0显示 1隐藏）',
    status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
    perms             varchar(100)    default null               comment '权限标识',
    icon              varchar(100)    default '#'                comment '菜单图标',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default ''                 comment '备注',
    primary key (menu_id)
) engine=innodb comment = '菜单权限表';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system',           null, '', 1, 0, 'M', '0', '0', '', 'system',   103, 1, sysdate(), null, null, '系统管理目录');
insert into sys_menu values('2', '系统监控', '0', '3', 'monitor',          null, '', 1, 0, 'M', '0', '0', '', 'monitor',  103, 1, sysdate(), null, null, '系统监控目录');
-- 二级菜单
insert into sys_menu values('100',  '用户管理',     '1',   '1', 'user',             'system/user/index',            '', 1, 0, 'C', '0', '0', 'system:user:list',            'user',          103, 1, sysdate(), null, null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理',     '1',   '2', 'role',             'system/role/index',            '', 1, 0, 'C', '0', '0', 'system:role:list',            'peoples',       103, 1, sysdate(), null, null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理',     '1',   '3', 'menu',             'system/menu/index',            '', 1, 0, 'C', '0', '0', 'system:menu:list',            'tree-table',    103, 1, sysdate(), null, null, '菜单管理菜单');
insert into sys_menu values('108',  '日志管理',     '1',   '9', 'log',              '',                             '', 1, 0, 'M', '0', '0', '',                            'log',           103, 1, sysdate(), null, null, '日志管理菜单');
insert into sys_menu values('130', '分配用户',     '1',   '2', 'role-auth/user/:roleId', 'system/role/authUser', '', 1, 1, 'C', '1', '0', 'system:role:edit',      '#',               103, 1, sysdate(), null, null, '/system/role');
insert into sys_menu values('131', '分配角色',     '1',   '1', 'user-auth/role/:userId', 'system/user/authRole', '', 1, 1, 'C', '1', '0', 'system:user:edit',      '#',               103, 1, sysdate(), null, null, '/system/user');

-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          103, 1, sysdate(), null, null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    103, 1, sysdate(), null, null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1001', '用户查询', '100', '1',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1002', '用户新增', '100', '2',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1003', '用户修改', '100', '3',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1004', '用户删除', '100', '4',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1005', '用户导出', '100', '5',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1006', '用户导入', '100', '6',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1007', '重置密码', '100', '7',  '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 103, 1, sysdate(), null, null, '');
-- 角色管理按钮
insert into sys_menu values('1008', '角色查询', '101', '1',  '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1009', '角色新增', '101', '2',  '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1010', '角色修改', '101', '3',  '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1011', '角色删除', '101', '4',  '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1012', '角色导出', '101', '5',  '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 103, 1, sysdate(), null, null, '');
-- 菜单管理按钮
insert into sys_menu values('1013', '菜单查询', '102', '1',  '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1014', '菜单新增', '102', '2',  '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1015', '菜单修改', '102', '3',  '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1016', '菜单删除', '102', '4',  '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 103, 1, sysdate(), null, null, '');
-- 部门管理按钮
-- 岗位管理按钮
-- 字典管理按钮
-- 参数设置按钮
-- 操作日志按钮
insert into sys_menu values('1040', '操作查询', '500', '1', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1041', '操作删除', '500', '2', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1042', '日志导出', '500', '4', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 103, 1, sysdate(), null, null, '');
-- 登录日志按钮
insert into sys_menu values('1043', '登录查询', '501', '1', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1044', '登录删除', '501', '2', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1045', '日志导出', '501', '3', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('1050', '账户解锁', '501', '4', '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 103, 1, sysdate(), null, null, '');

-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
create table sys_user_role (
    user_id   bigint(20) not null comment '用户ID',
    role_id   bigint(20) not null comment '角色ID',
    primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('3', '3');
insert into sys_user_role values ('4', '4');

-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
create table sys_role_menu (
    role_id   bigint(20) not null comment '角色ID',
    menu_id   bigint(20) not null comment '菜单ID',
    primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('3', '1');
insert into sys_role_menu values ('3', '5');
insert into sys_role_menu values ('3', '100');
insert into sys_role_menu values ('3', '101');
insert into sys_role_menu values ('3', '102');
insert into sys_role_menu values ('3', '103');
insert into sys_role_menu values ('3', '104');
insert into sys_role_menu values ('3', '105');
insert into sys_role_menu values ('3', '106');
insert into sys_role_menu values ('3', '107');
insert into sys_role_menu values ('3', '108');
insert into sys_role_menu values ('3', '118');
insert into sys_role_menu values ('3', '123');
insert into sys_role_menu values ('3', '130');
insert into sys_role_menu values ('3', '131');
insert into sys_role_menu values ('3', '132');
insert into sys_role_menu values ('3', '133');
insert into sys_role_menu values ('3', '500');
insert into sys_role_menu values ('3', '501');
insert into sys_role_menu values ('3', '1001');
insert into sys_role_menu values ('3', '1002');
insert into sys_role_menu values ('3', '1003');
insert into sys_role_menu values ('3', '1004');
insert into sys_role_menu values ('3', '1005');
insert into sys_role_menu values ('3', '1006');
insert into sys_role_menu values ('3', '1007');
insert into sys_role_menu values ('3', '1008');
insert into sys_role_menu values ('3', '1009');
insert into sys_role_menu values ('3', '1010');
insert into sys_role_menu values ('3', '1011');
insert into sys_role_menu values ('3', '1012');
insert into sys_role_menu values ('3', '1013');
insert into sys_role_menu values ('3', '1014');
insert into sys_role_menu values ('3', '1015');
insert into sys_role_menu values ('3', '1016');
insert into sys_role_menu values ('3', '1017');
insert into sys_role_menu values ('3', '1018');
insert into sys_role_menu values ('3', '1019');
insert into sys_role_menu values ('3', '1020');
insert into sys_role_menu values ('3', '1021');
insert into sys_role_menu values ('3', '1022');
insert into sys_role_menu values ('3', '1023');
insert into sys_role_menu values ('3', '1024');
insert into sys_role_menu values ('3', '1025');
insert into sys_role_menu values ('3', '1026');
insert into sys_role_menu values ('3', '1027');
insert into sys_role_menu values ('3', '1028');
insert into sys_role_menu values ('3', '1029');
insert into sys_role_menu values ('3', '1030');
insert into sys_role_menu values ('3', '1031');
insert into sys_role_menu values ('3', '1032');
insert into sys_role_menu values ('3', '1033');
insert into sys_role_menu values ('3', '1034');
insert into sys_role_menu values ('3', '1035');
insert into sys_role_menu values ('3', '1036');
insert into sys_role_menu values ('3', '1037');
insert into sys_role_menu values ('3', '1038');
insert into sys_role_menu values ('3', '1039');
insert into sys_role_menu values ('3', '1040');
insert into sys_role_menu values ('3', '1041');
insert into sys_role_menu values ('3', '1042');
insert into sys_role_menu values ('3', '1043');
insert into sys_role_menu values ('3', '1044');
insert into sys_role_menu values ('3', '1045');
insert into sys_role_menu values ('3', '1050');
insert into sys_role_menu values ('3', '1061');
insert into sys_role_menu values ('3', '1062');
insert into sys_role_menu values ('3', '1063');
insert into sys_role_menu values ('3', '1064');
insert into sys_role_menu values ('3', '1065');
insert into sys_role_menu values ('3', '1500');
insert into sys_role_menu values ('3', '1501');
insert into sys_role_menu values ('3', '1502');
insert into sys_role_menu values ('3', '1503');
insert into sys_role_menu values ('3', '1504');
insert into sys_role_menu values ('3', '1505');
insert into sys_role_menu values ('3', '1506');
insert into sys_role_menu values ('3', '1507');
insert into sys_role_menu values ('3', '1508');
insert into sys_role_menu values ('3', '1509');
insert into sys_role_menu values ('3', '1510');
insert into sys_role_menu values ('3', '1511');
insert into sys_role_menu values ('3', '1600');
insert into sys_role_menu values ('3', '1601');
insert into sys_role_menu values ('3', '1602');
insert into sys_role_menu values ('3', '1603');
insert into sys_role_menu values ('3', '1620');
insert into sys_role_menu values ('3', '1621');
insert into sys_role_menu values ('3', '1622');
insert into sys_role_menu values ('3', '1623');
insert into sys_role_menu values ('3', '11616');
insert into sys_role_menu values ('3', '11618');
insert into sys_role_menu values ('3', '11619');
insert into sys_role_menu values ('3', '11622');
insert into sys_role_menu values ('3', '11623');
insert into sys_role_menu values ('3', '11629');
insert into sys_role_menu values ('3', '11632');
insert into sys_role_menu values ('3', '11633');
insert into sys_role_menu values ('3', '11638');
insert into sys_role_menu values ('3', '11639');
insert into sys_role_menu values ('3', '11640');
insert into sys_role_menu values ('3', '11641');
insert into sys_role_menu values ('3', '11642');
insert into sys_role_menu values ('3', '11643');
insert into sys_role_menu values ('3', '11701');
insert into sys_role_menu values ('4', '5');
insert into sys_role_menu values ('4', '1500');
insert into sys_role_menu values ('4', '1501');
insert into sys_role_menu values ('4', '1502');
insert into sys_role_menu values ('4', '1503');
insert into sys_role_menu values ('4', '1504');
insert into sys_role_menu values ('4', '1505');
insert into sys_role_menu values ('4', '1506');
insert into sys_role_menu values ('4', '1507');
insert into sys_role_menu values ('4', '1508');
insert into sys_role_menu values ('4', '1509');
insert into sys_role_menu values ('4', '1510');
insert into sys_role_menu values ('4', '1511');

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
create table sys_role_dept (
    role_id   bigint(20) not null comment '角色ID',
    dept_id   bigint(20) not null comment '部门ID',
    primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
create table sys_user_post
(
    user_id   bigint(20) not null comment '用户ID',
    post_id   bigint(20) not null comment '岗位ID',
    primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values ('1', '1');

-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
create table sys_oper_log (
    oper_id           bigint(20)      not null                   comment '日志主键',
    tenant_id         varchar(20)     default '000000'           comment '租户编号',
    title             varchar(50)     default ''                 comment '模块标题',
    business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
    method            varchar(100)    default ''                 comment '方法名称',
    request_method    varchar(10)     default ''                 comment '请求方式',
    operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
    oper_name         varchar(50)     default ''                 comment '操作人员',
    dept_name         varchar(50)     default ''                 comment '部门名称',
    oper_url          varchar(255)    default ''                 comment '请求URL',
    oper_ip           varchar(128)    default ''                 comment '主机地址',
    oper_location     varchar(255)    default ''                 comment '操作地点',
    oper_param        varchar(4000)   default ''                 comment '请求参数',
    json_result       varchar(4000)   default ''                 comment '返回参数',
    status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
    error_msg         varchar(4000)   default ''                 comment '错误消息',
    oper_time         datetime                                   comment '操作时间',
    cost_time         bigint(20)      default 0                  comment '消耗时间',
    primary key (oper_id),
    key idx_sys_oper_log_bt (business_type),
    key idx_sys_oper_log_s  (status),
    key idx_sys_oper_log_ot (oper_time)
) engine=innodb comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
create table sys_dict_type
(
    dict_id          bigint(20)      not null                   comment '字典主键',
    tenant_id        varchar(20)     default '000000'           comment '租户编号',
    dict_name        varchar(100)    default ''                 comment '字典名称',
    dict_type        varchar(100)    default ''                 comment '字典类型',
    create_dept      bigint(20)      default null               comment '创建部门',
    create_by        bigint(20)      default null               comment '创建者',
    create_time      datetime                                   comment '创建时间',
    update_by        bigint(20)      default null               comment '更新者',
    update_time      datetime                                   comment '更新时间',
    remark           varchar(500)    default null               comment '备注',
    primary key (dict_id),
    unique (tenant_id, dict_type)
) engine=innodb comment = '字典类型表';

insert into sys_dict_type values(1, '000000', '用户性别', 'sys_user_sex',        103, 1, sysdate(), null, null, '用户性别列表');
insert into sys_dict_type values(2, '000000', '菜单状态', 'sys_show_hide',       103, 1, sysdate(), null, null, '菜单状态列表');
insert into sys_dict_type values(3, '000000', '系统开关', 'sys_normal_disable',  103, 1, sysdate(), null, null, '系统开关列表');
insert into sys_dict_type values(6, '000000', '系统是否', 'sys_yes_no',          103, 1, sysdate(), null, null, '系统是否列表');
insert into sys_dict_type values(9, '000000', '操作类型', 'sys_oper_type',       103, 1, sysdate(), null, null, '操作类型列表');
insert into sys_dict_type values(10, '000000', '系统状态', 'sys_common_status',  103, 1, sysdate(), null, null, '登录状态列表');
insert into sys_dict_type values(11, '000000', '授权类型', 'sys_grant_type',     103, 1, sysdate(), null, null, '认证授权类型');
insert into sys_dict_type values(12, '000000', '设备类型', 'sys_device_type',    103, 1, sysdate(), null, null, '客户端设备类型');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
create table sys_dict_data
(
    dict_code        bigint(20)      not null                   comment '字典编码',
    tenant_id        varchar(20)     default '000000'           comment '租户编号',
    dict_sort        int(4)          default 0                  comment '字典排序',
    dict_label       varchar(100)    default ''                 comment '字典标签',
    dict_value       varchar(100)    default ''                 comment '字典键值',
    dict_type        varchar(100)    default ''                 comment '字典类型',
    css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
    list_class       varchar(100)    default null               comment '表格回显样式',
    is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
    create_dept      bigint(20)      default null               comment '创建部门',
    create_by        bigint(20)      default null               comment '创建者',
    create_time      datetime                                   comment '创建时间',
    update_by        bigint(20)      default null               comment '更新者',
    update_time      datetime                                   comment '更新时间',
    remark           varchar(500)    default null               comment '备注',
    primary key (dict_code)
) engine=innodb comment = '字典数据表';

insert into sys_dict_data values(1, '000000', 1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', 103, 1, sysdate(), null, null, '性别男');
insert into sys_dict_data values(2, '000000', 2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', 103, 1, sysdate(), null, null, '性别女');
insert into sys_dict_data values(3, '000000', 3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', 103, 1, sysdate(), null, null, '性别未知');
insert into sys_dict_data values(4, '000000', 1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', 103, 1, sysdate(), null, null, '显示菜单');
insert into sys_dict_data values(5, '000000', 2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', 103, 1, sysdate(), null, null, '隐藏菜单');
insert into sys_dict_data values(6, '000000', 1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', 103, 1, sysdate(), null, null, '正常状态');
insert into sys_dict_data values(7, '000000', 2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', 103, 1, sysdate(), null, null, '停用状态');
insert into sys_dict_data values(12, '000000', 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', 103, 1, sysdate(), null, null, '系统默认是');
insert into sys_dict_data values(13, '000000', 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', 103, 1, sysdate(), null, null, '系统默认否');
insert into sys_dict_data values(29, '000000', 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', 103, 1, sysdate(), null, null, '其他操作');
insert into sys_dict_data values(18, '000000', 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', 103, 1, sysdate(), null, null, '新增操作');
insert into sys_dict_data values(19, '000000', 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', 103, 1, sysdate(), null, null, '修改操作');
insert into sys_dict_data values(20, '000000', 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', 103, 1, sysdate(), null, null, '删除操作');
insert into sys_dict_data values(21, '000000', 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', 103, 1, sysdate(), null, null, '授权操作');
insert into sys_dict_data values(22, '000000', 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', 103, 1, sysdate(), null, null, '导出操作');
insert into sys_dict_data values(23, '000000', 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', 103, 1, sysdate(), null, null, '导入操作');
insert into sys_dict_data values(24, '000000', 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', 103, 1, sysdate(), null, null, '强退操作');
insert into sys_dict_data values(25, '000000', 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', 103, 1, sysdate(), null, null, '生成操作');
insert into sys_dict_data values(26, '000000', 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', 103, 1, sysdate(), null, null, '清空操作');
insert into sys_dict_data values(27, '000000', 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', 103, 1, sysdate(), null, null, '正常状态');
insert into sys_dict_data values(28, '000000', 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', 103, 1, sysdate(), null, null, '停用状态');
insert into sys_dict_data values(30, '000000', 0,  '密码认证', 'password',   'sys_grant_type',   'el-check-tag',   'default', 'N', 103, 1, sysdate(), null, null, '密码认证');
insert into sys_dict_data values(31, '000000', 0,  '短信认证', 'sms',        'sys_grant_type',   'el-check-tag',   'default', 'N', 103, 1, sysdate(), null, null, '短信认证');
insert into sys_dict_data values(32, '000000', 0,  '邮件认证', 'email',      'sys_grant_type',   'el-check-tag',   'default', 'N', 103, 1, sysdate(), null, null, '邮件认证');
insert into sys_dict_data values(33, '000000', 0,  '小程序认证', 'xcx',      'sys_grant_type',   'el-check-tag',   'default', 'N', 103, 1, sysdate(), null, null, '小程序认证');
insert into sys_dict_data values(34, '000000', 0,  '三方登录认证', 'social', 'sys_grant_type',   'el-check-tag',   'default', 'N', 103, 1, sysdate(), null, null, '三方登录认证');
insert into sys_dict_data values(35, '000000', 0,  'PC',    'pc',         'sys_device_type',     '',   'default', 'N', 103, 1, sysdate(), null, null, 'PC');
insert into sys_dict_data values(36, '000000', 0,  '安卓', 'android',     'sys_device_type',     '',   'default', 'N', 103, 1, sysdate(), null, null, '安卓');
insert into sys_dict_data values(37, '000000', 0,  'iOS', 'ios',          'sys_device_type',     '',   'default', 'N', 103, 1, sysdate(), null, null, 'iOS');
insert into sys_dict_data values(38, '000000', 0,  '小程序', 'xcx',       'sys_device_type',     '',   'default', 'N', 103, 1, sysdate(), null, null, '小程序');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
create table sys_config (
    config_id         bigint(20)      not null                   comment '参数主键',
    tenant_id         varchar(20)     default '000000'           comment '租户编号',
    config_name       varchar(100)    default ''                 comment '参数名称',
    config_key        varchar(100)    default ''                 comment '参数键名',
    config_value      varchar(500)    default ''                 comment '参数键值',
    config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (config_id)
) engine=innodb comment = '参数配置表';

insert into sys_config values(1, '000000', '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 103, 1, sysdate(), null, null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '000000', '用户管理-账号初始密码',        'sys.user.initPassword',         '123456',        'Y', 103, 1, sysdate(), null, null, '初始化密码 123456' );
insert into sys_config values(3, '000000', '主框架页-侧边栏主题',          'sys.index.sideTheme',           'theme-dark',    'Y', 103, 1, sysdate(), null, null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(5, '000000', '账号自助-是否开启用户注册功能',  'sys.account.registerUser',      'false',         'Y', 103, 1, sysdate(), null, null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(11, '000000', 'OSS预览列表资源开关',         'sys.oss.previewListResource',   'true',          'Y', 103, 1, sysdate(), null, null, 'true:开启, false:关闭');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
create table sys_logininfor (
    info_id        bigint(20)     not null                  comment '访问ID',
    tenant_id      varchar(20)    default '000000'          comment '租户编号',
    user_name      varchar(50)    default ''                comment '用户账号',
    client_key     varchar(32)    default ''                comment '客户端',
    device_type    varchar(32)    default ''                comment '设备类型',
    ipaddr         varchar(128)   default ''                comment '登录IP地址',
    login_location varchar(255)   default ''                comment '登录地点',
    browser        varchar(50)    default ''                comment '浏览器类型',
    os             varchar(50)    default ''                comment '操作系统',
    status         char(1)        default '0'               comment '登录状态（0成功 1失败）',
    msg            varchar(255)   default ''                comment '提示消息',
    login_time     datetime                                 comment '访问时间',
    primary key (info_id),
    key idx_sys_logininfor_s  (status),
    key idx_sys_logininfor_lt (login_time)
) engine=innodb comment = '系统访问记录';


-- ----------------------------
-- OSS对象存储表
-- ----------------------------
create table sys_oss (
    oss_id          bigint(20)   not null                   comment '对象存储主键',
    tenant_id       varchar(20)           default '000000'  comment '租户编号',
    file_name       varchar(255) not null default ''        comment '文件名',
    original_name   varchar(255) not null default ''        comment '原名',
    file_suffix     varchar(10)  not null default ''        comment '文件后缀名',
    url             varchar(500) not null                   comment 'URL地址',
    ext1            text                  default null      comment '扩展字段',
    create_dept     bigint(20)            default null      comment '创建部门',
    create_time     datetime              default null      comment '创建时间',
    create_by       bigint(20)            default null      comment '上传人',
    update_time     datetime              default null      comment '更新时间',
    update_by       bigint(20)            default null      comment '更新人',
    service         varchar(20)  not null default 'minio'   comment '服务商',
    primary key (oss_id)
) engine=innodb comment ='OSS对象存储表';

-- ----------------------------
-- OSS对象存储动态配置表
-- ----------------------------
create table sys_oss_config (
    oss_config_id   bigint(20)    not null                  comment '主键',
    tenant_id       varchar(20)             default '000000'comment '租户编号',
    config_key      varchar(20)   not null  default ''      comment '配置key',
    access_key      varchar(255)            default ''      comment 'accessKey',
    secret_key      varchar(255)            default ''      comment '秘钥',
    bucket_name     varchar(255)            default ''      comment '桶名称',
    prefix          varchar(255)            default ''      comment '前缀',
    endpoint        varchar(255)            default ''      comment '访问站点',
    domain          varchar(255)            default ''      comment '自定义域名',
    is_https        char(1)                 default 'N'     comment '是否https（Y=是,N=否）',
    region          varchar(255)            default ''      comment '域',
    access_policy   char(1)       not null  default '1'     comment '桶权限类型(0=private 1=public 2=custom)',
    status          char(1)                 default '1'     comment '是否默认（0=是,1=否）',
    ext1            varchar(255)            default ''      comment '扩展字段',
    create_dept     bigint(20)              default null    comment '创建部门',
    create_by       bigint(20)              default null    comment '创建者',
    create_time     datetime                default null    comment '创建时间',
    update_by       bigint(20)              default null    comment '更新者',
    update_time     datetime                default null    comment '更新时间',
    remark          varchar(500)            default null    comment '备注',
    primary key (oss_config_id)
) engine=innodb comment='对象存储配置表';

insert into sys_oss_config values (1, '000000', 'minio',  'ruoyi',            'ruoyi123',        'ruoyi',             '', '127.0.0.1:9000',                '','N', '',             '1' ,'0', '', 103, 1, sysdate(), 1, sysdate(), null);
insert into sys_oss_config values (2, '000000', 'qiniu',  'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 's3-cn-north-1.qiniucs.com',     '','N', '',             '1' ,'1', '', 103, 1, sysdate(), 1, sysdate(), null);
insert into sys_oss_config values (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 'oss-cn-beijing.aliyuncs.com',   '','N', '',             '1' ,'1', '', 103, 1, sysdate(), 1, sysdate(), null);
insert into sys_oss_config values (4, '000000', 'qcloud', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi-1240000000',  '', 'cos.ap-beijing.myqcloud.com',   '','N', 'ap-beijing',   '1' ,'1', '', 103, 1, sysdate(), 1, sysdate(), null);
insert into sys_oss_config values (5, '000000', 'image',  'ruoyi',            'ruoyi123',        'ruoyi',             'image', '127.0.0.1:9000',           '','N', '',             '1' ,'1', '', 103, 1, sysdate(), 1, sysdate(), null);

-- ----------------------------
-- 系统授权表
-- ----------------------------
create table sys_client (
    id                  bigint(20)    not null            comment 'id',
    client_id           varchar(64)   default null        comment '客户端id',
    client_key          varchar(32)   default null        comment '客户端key',
    client_secret       varchar(255)  default null        comment '客户端秘钥',
    grant_type          varchar(255)  default null        comment '授权类型',
    device_type         varchar(32)   default null        comment '设备类型',
    active_timeout      int(11)       default 1800        comment 'token活跃超时时间',
    timeout             int(11)       default 604800      comment 'token固定超时',
    status              char(1)       default '0'         comment '状态（0正常 1停用）',
    del_flag            char(1)       default '0'         comment '删除标志（0代表存在 1代表删除）',
    create_dept         bigint(20)    default null        comment '创建部门',
    create_by           bigint(20)    default null        comment '创建者',
    create_time         datetime      default null        comment '创建时间',
    update_by           bigint(20)    default null        comment '更新者',
    update_time         datetime      default null        comment '更新时间',
    primary key (id)
) engine=innodb comment='系统授权表';

insert into sys_client values (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, 0, 0, 103, 1, sysdate(), 1, sysdate());
insert into sys_client values (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, 0, 0, 103, 1, sysdate(), 1, sysdate());


