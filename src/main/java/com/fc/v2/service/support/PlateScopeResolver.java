package com.fc.v2.service.support;

import com.fc.v2.mapper.auto.TToponAreaUserMapper;
import com.fc.v2.model.auto.TToponAreaUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 段卡清单数据范围：权限按层级收口。
 * 持全市权限（topon:plate:scope:city）的见全市；其余区县经办岗只见本人绑定的区县；
 * 没绑定区县、也没有全市权限的，一笔都不见（安全默认宁紧勿松）；
 * 同一条段卡，市局与区里点开是同一笔账。
 *
 * @author fuce
 * @date 2026-09-27
 */
@Component
public class PlateScopeResolver {

    /** 全市范围权限标识 */
    public static final String PERM_SCOPE_CITY = "topon:plate:scope:city";

    /** 范围：全市 / 本区县 / 谁也不是 */
    public enum ScopeType { CITY, AREA, NONE }

    /** 解析出来的数据范围 */
    public static final class Scope {
        private final ScopeType type;
        private final String areaCode;

        Scope(ScopeType type, String areaCode) {
            this.type = type;
            this.areaCode = areaCode;
        }

        public static Scope city() {
            return new Scope(ScopeType.CITY, null);
        }

        public static Scope area(String areaCode) {
            return new Scope(ScopeType.AREA, areaCode);
        }

        public static Scope none() {
            return new Scope(ScopeType.NONE, null);
        }
        public ScopeType getType() {
            return type;
        }

        public String getAreaCode() {
            return areaCode;
        }

        public boolean isCity() {
            return type == ScopeType.CITY;
        }

        public boolean isArea() {
            return type == ScopeType.AREA;
        }
    }

    @Resource
    private TToponAreaUserMapper areaUserMapper;

    /**
     * 当前登录人是否全市范围。
     */
    public boolean isCityScope() {
        try {
            return SecurityUtils.getSubject() != null
                    && SecurityUtils.getSubject().isPermitted(PERM_SCOPE_CITY);
        } catch (UnavailableSecurityManagerException | IllegalStateException e) {
            // 无会话（定时任务）时不算全市
            return false;
        }
    }

    /**
     * 解析当前登录人的数据范围。
     */
    public Scope currentScope() {
        if (isCityScope()) {
            return new Scope(ScopeType.CITY, null);
        }
        Long uid = currentUserIdOrNull();
        if (uid == null) {
            return new Scope(ScopeType.NONE, null);
        }
        TToponAreaUser bind = areaUserMapper.selectOne(new QueryWrapper<TToponAreaUser>()
                .eq("user_id", uid));
        if (bind == null || bind.getAreaCode() == null || bind.getAreaCode().trim().isEmpty()) {
            // 区县的人没绑区县：一笔都不放，不能误当成全市
            return new Scope(ScopeType.NONE, null);
        }
        return new Scope(ScopeType.AREA, bind.getAreaCode());
    }

    /**
     * 当前登录人所属区县代码；全市范围返回 null。
     * 注意：NONE（未绑定）也返回 null，调用方要区分时请用 {@link #currentScope()}。
     */
    public String currentAreaCode() {
        Scope scope = currentScope();
        return scope.isArea() ? scope.getAreaCode() : null;
    }

    private Long currentUserIdOrNull() {
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal == null) {
                return null;
            }
            return ((com.fc.v2.model.auto.TSysUser) principal).getId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
