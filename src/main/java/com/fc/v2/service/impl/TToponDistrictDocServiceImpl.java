package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.PlateCardException;
import com.fc.v2.mapper.auto.TToponDistrictDocMapper;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.service.ITToponDistrictDocService;
import com.fc.v2.util.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 乡镇街道基础档案 Service业务层处理
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TToponDistrictDocServiceImpl
        extends ServiceImpl<TToponDistrictDocMapper, TToponDistrictDoc>
        implements ITToponDistrictDocService {

    /** 地貌类只认这三样 */
    private static final List<String> SITE_TYPES = Arrays.asList("城镇街区", "乡村区域", "毗界地段");

    /** 档案在用 */
    private static final int STATUS_ACTIVE = 0;
    /** 档案已撤并 */
    private static final int STATUS_ABOLISHED = 1;

    @Override
    public TToponDistrictDoc selectById(Long id) {
        if (id == null) {
            return null;
        }
        // 含已撤并：先前挂在下面的卡照旧查得到，撤并不是删除
        return this.baseMapper.selectOne(new QueryWrapper<TToponDistrictDoc>()
                .eq("id", id).eq("del_flag", 0));
    }

    @Override
    public TToponDistrictDoc selectBySiteNo(String siteNo) {
        if (StringUtils.isEmpty(siteNo)) {
            return null;
        }
        return this.baseMapper.selectOne(new QueryWrapper<TToponDistrictDoc>()
                .eq("site_no", siteNo).eq("del_flag", 0));
    }

    @Override
    public List<TToponDistrictDoc> selectList(Wrapper<TToponDistrictDoc> queryWrapper) {
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<TToponDistrictDoc> listActive() {
        return this.baseMapper.selectList(new QueryWrapper<TToponDistrictDoc>()
                .eq("del_flag", 0).eq("status", STATUS_ACTIVE)
                .orderByAsc("area_code", "site_no"));
    }

    @Override
    public TToponDistrictDoc register(TToponDistrictDoc doc) {
        if (doc == null) {
            throw new PlateCardException("doc", "档案内容是空的，立不了档");
        }
        if (StringUtils.isEmpty(doc.getSiteNo())) {
            throw new PlateCardException("siteNo", "乡镇街道区划代码没填，立不了档");
        }
        if (StringUtils.isEmpty(doc.getSiteName())) {
            throw new PlateCardException("siteName", "档案名称没填");
        }
        if (StringUtils.isEmpty(doc.getSiteType()) || !SITE_TYPES.contains(doc.getSiteType())) {
            throw new PlateCardException("siteType", "地貌类只认：城镇街区 / 乡村区域 / 毗界地段");
        }
        if (StringUtils.isEmpty(doc.getRoadName())) {
            throw new PlateCardException("roadName", "坐落(省—市—区县)没填");
        }
        // 区划代码前6位即区县；区县代码格须咬得上，错一格当场指出
        String areaCode = doc.getAreaCode();
        if (StringUtils.isEmpty(areaCode)) {
            throw new PlateCardException("areaCode", "所属区县代码没填");
        }
        if (doc.getSiteNo().length() < 6 || !doc.getSiteNo().startsWith(areaCode)) {
            throw new PlateCardException("areaCode",
                    "区县代码 " + areaCode + " 与区划代码 " + doc.getSiteNo() + " 前6位咬不上");
        }
        // 三道上限：都得是正数，且一档<二档<三档，依次抬高
        BigDecimal th1 = doc.getTh1Max();
        BigDecimal th2 = doc.getTh2Max();
        BigDecimal th3 = doc.getTh3Max();
        if (th1 == null || th2 == null || th3 == null) {
            throw new PlateCardException("thMax", "可编容量三道上限都得填齐");
        }
        if (th1.signum() <= 0 || th2.signum() <= 0 || th3.signum() <= 0) {
            throw new PlateCardException("thMax", "容量上限填成负数或零了，落不下去");
        }
        if (!(th1.compareTo(th2) < 0 && th2.compareTo(th3) < 0)) {
            throw new PlateCardException("thMax", "三档上限须依次抬高：一档<二档<三档");
        }
        Integer dup = this.baseMapper.selectCount(new QueryWrapper<TToponDistrictDoc>()
                .eq("site_no", doc.getSiteNo()));
        if (dup != null && dup > 0) {
            throw new PlateCardException("siteNo", "区划代码 " + doc.getSiteNo() + " 已立过档案，一个乡镇街道只留一份");
        }

        doc.setStatus(STATUS_ACTIVE);
        doc.setDelFlag(0);
        this.baseMapper.insert(doc);
        return doc;
    }

    @Override
    public int abolish(Long id, String remark) {
        TToponDistrictDoc doc = selectById(id);
        if (doc == null) {
            throw new PlateCardException("id", "档案不存在，撤并不了");
        }
        if (doc.getStatus() != null && doc.getStatus() == STATUS_ABOLISHED) {
            throw new PlateCardException("status", "该档案早已撤并，不许借着重改");
        }
        doc.setStatus(STATUS_ABOLISHED);
        doc.setUpdateTime(new Date());
        if (StringUtils.isNotEmpty(remark)) {
            doc.setRemark(remark);
        }
        // 只翻情形标记：旧卡挂在下面照旧查得到，也不许借撤并动旧账
        return this.baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TToponDistrictDoc>()
                .eq("id", id).eq("del_flag", 0)
                .set("status", STATUS_ABOLISHED)
                .set("update_time", new Date())
                .set(StringUtils.isNotEmpty(remark), "remark", remark));
    }
}
