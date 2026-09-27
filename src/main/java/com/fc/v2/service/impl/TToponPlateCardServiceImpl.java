package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TToponPlateCardMapper;
import com.fc.v2.mapper.auto.TToponDistrictDocMapper;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.service.ITToponPlateCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 门楼牌号段建档卡Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TToponPlateCardServiceImpl extends ServiceImpl<TToponPlateCardMapper, TToponPlateCard> implements ITToponPlateCardService {

    /** 档案情形：已撤并 */
    private static final int DOC_STATUS_MERGED = 1;

    @Autowired
    private TToponDistrictDocMapper toponDistrictDocMapper;

    @Override
    public TToponPlateCard selectTToponPlateCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TToponPlateCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TToponPlateCard> selectTToponPlateCardList(Wrapper<TToponPlateCard> queryWrapper) {
        // 清单条件由调用方统一构造，服务层不再私设格口、不私翻页
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTToponPlateCard(TToponPlateCard record) {
        AjaxResult result = insertTToponPlateCard(record, null);
        return isSuccess(result) ? 1 : 0;
    }

    @Override
    public AjaxResult insertTToponPlateCard(TToponPlateCard record, String operName) {
        if (record == null) {
            return AjaxResult.error("段卡不能为空");
        }
        // 段卡号是这张卡的牙口，系统不代填
        if (StringUtils.isEmpty(record.getBillNo())) {
            return AjaxResult.error("段卡号不能为空：单号系统不代填，请如实录入");
        }
        // 立卡要挂档案，只认有档案的
        if (record.getSiteId() == null) {
            return AjaxResult.error("所属乡镇街道基础档案不能为空：立卡须挂在档案名下");
        }
        TToponDistrictDoc arch = selectDocById(record.getSiteId());
        if (arch == null) {
            return AjaxResult.error("所属乡镇街道基础档案不存在：立卡只认有档案的");
        }
        // 档案一旦撤并，往后的新卡就挂不进去
        if (isDocMerged(arch)) {
            return AjaxResult.error("所属档案[" + arch.getSiteName() + "]已撤并：新卡挂不进去");
        }
        // 同一个号段第二回立卡要挡得住；来源号撞上的并成一条，账上只许剩一笔
        Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TToponPlateCard>()
                .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
        if (dupCnt != null && dupCnt > 0) {
            return AjaxResult.error("段卡号[" + record.getBillNo() + "]已立卡：同一号段不得重复立卡，账上只许剩一笔");
        }
        // 可编号数：负数、零、高过档案能承接上限的，卡落不下去
        String qtyError = checkQty(record.getQty(), arch);
        if (qtyError != null) {
            return AjaxResult.error(qtyError);
        }
        if (record.getFineAmt() != null && record.getFineAmt().compareTo(BigDecimal.ZERO) < 0) {
            return AjaxResult.error("本年度新编个数不能为负");
        }

        record.setSiteNo(arch.getSiteNo());
        record.setQty(record.getQty().setScale(2, RoundingMode.HALF_UP));
        // 容量档由系统拿号数对三道上限折算，不由人动手填
        record.setGradeLevel(computeGradeLevel(record.getQty(), arch));
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        record.setDelFlag(0);
        // 谁按下立卡那一下，这名就记在谁身上；为空时由全局填充当前登录名
        if (StringUtils.isNotEmpty(operName)) {
            record.setCreateBy(operName);
        }
        int rows = this.baseMapper.insert(record);
        return rows > 0 ? AjaxResult.success("立卡成功") : AjaxResult.error("立卡失败");
    }

    @Override
    public int updateTToponPlateCard(TToponPlateCard record) {
        AjaxResult result = updateTToponPlateCard(record, null);
        return isSuccess(result) ? 1 : 0;
    }

    @Override
    public AjaxResult updateTToponPlateCard(TToponPlateCard record, String operName) {
        if (record == null || record.getId() == null) {
            return AjaxResult.error("段卡主键不能为空");
        }
        TToponPlateCard dbCard = this.baseMapper.selectOne(new QueryWrapper<TToponPlateCard>()
                .eq("id", record.getId()).eq("del_flag", 0));
        if (dbCard == null) {
            return AjaxResult.error("段卡不存在或已删除");
        }
        // 先前挂在撤并档案下面的照旧查得到，但不许借着重改
        TToponDistrictDoc arch = selectDocById(dbCard.getSiteId());
        if (arch == null) {
            return AjaxResult.error("所属乡镇街道基础档案不存在");
        }
        if (isDocMerged(arch)) {
            return AjaxResult.error("所属档案[" + arch.getSiteName() + "]已撤并：旧卡照旧可查，不许借着重改");
        }
        // 街巷改属：换挂的档案只认有档案且在用的，区划归属跟着档案走
        if (record.getSiteId() != null && !record.getSiteId().equals(dbCard.getSiteId())) {
            TToponDistrictDoc newArch = selectDocById(record.getSiteId());
            if (newArch == null) {
                return AjaxResult.error("改属的乡镇街道基础档案不存在：只认有档案的");
            }
            if (isDocMerged(newArch)) {
                return AjaxResult.error("改属的档案[" + newArch.getSiteName() + "]已撤并：新卡挂不进去");
            }
            arch = newArch;
            record.setSiteNo(arch.getSiteNo());
        } else {
            // 未改属，挂接与区划归属不动
            record.setSiteId(null);
            record.setSiteNo(null);
        }
        // 段卡号是牙口：不许抹掉；换号也只许占一笔
        if (StringUtils.isEmpty(record.getBillNo())) {
            record.setBillNo(null);
        } else if (!record.getBillNo().equals(dbCard.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TToponPlateCard>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return AjaxResult.error("段卡号[" + record.getBillNo() + "]已立卡：同一号段不得重复立卡，账上只许剩一笔");
            }
        }
        // 号数有增减要当场验，容量档跟着重折
        if (record.getQty() != null) {
            String qtyError = checkQty(record.getQty(), arch);
            if (qtyError != null) {
                return AjaxResult.error(qtyError);
            }
            record.setQty(record.getQty().setScale(2, RoundingMode.HALF_UP));
        }
        if (record.getFineAmt() != null && record.getFineAmt().compareTo(BigDecimal.ZERO) < 0) {
            return AjaxResult.error("本年度新编个数不能为负");
        }
        BigDecimal effectiveQty = record.getQty() != null ? record.getQty() : dbCard.getQty();
        if (effectiveQty != null) {
            record.setGradeLevel(computeGradeLevel(effectiveQty, arch));
        }
        // 创建人记在立卡人身上，后来人重录也不许挪；经手人一办一换手续记一条
        record.setCreateBy(null);
        record.setCreateTime(null);
        if (StringUtils.isNotEmpty(operName)) {
            record.setUpdateBy(operName);
        }
        int rows = this.baseMapper.update(record, new UpdateWrapper<TToponPlateCard>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
        return rows > 0 ? AjaxResult.success("重录成功") : AjaxResult.error("重录失败");
    }

    @Override
    public int deleteTToponPlateCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTToponPlateCardById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    @Override
    public List<TToponDistrictDoc> selectUsableDistrictDocList() {
        return toponDistrictDocMapper.selectList(new QueryWrapper<TToponDistrictDoc>()
                .eq("del_flag", 0)
                .eq("status", 0)
                .orderByAsc("site_no"));
    }

    /** 按主键取未删除的乡镇街道基础档案 */
    private TToponDistrictDoc selectDocById(Integer siteId) {
        if (siteId == null) {
            return null;
        }
        return toponDistrictDocMapper.selectOne(new QueryWrapper<TToponDistrictDoc>()
                .eq("id", siteId).eq("del_flag", 0));
    }

    /** 档案是否已撤并 */
    private boolean isDocMerged(TToponDistrictDoc doc) {
        return doc != null && doc.getStatus() != null && doc.getStatus() == DOC_STATUS_MERGED;
    }

    /**
     * 可编号数校验：填成负数、填零、或者高过所属档案能承接的上限，卡落不下去，
     * 当场说清是哪一格不对。
     *
     * @return 不为 null 即为当场回话
     */
    private String checkQty(BigDecimal qty, TToponDistrictDoc arch) {
        if (qty == null) {
            return "可编号数不能为空：单侧和双侧合起来的个数要填在卡上";
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return "可编号数不能为负数或零";
        }
        if (arch != null && arch.getTh3Max() != null && qty.compareTo(arch.getTh3Max()) > 0) {
            return "可编号数超出所属档案能承接的上限（三档上限"
                    + arch.getTh3Max().stripTrailingZeros().toPlainString() + "个）";
        }
        return null;
    }

    /** 容量档由系统拿号数去对档案那三道上限折算，不兴由人自己动手填 */
    private int computeGradeLevel(BigDecimal qty, TToponDistrictDoc arch) {
        if (arch.getTh1Max() != null && qty.compareTo(arch.getTh1Max()) <= 0) {
            return 1;
        }
        if (arch.getTh2Max() != null && qty.compareTo(arch.getTh2Max()) <= 0) {
            return 2;
        }
        return 3;
    }

    /** 接口回话是否成了 */
    private boolean isSuccess(AjaxResult result) {
        return result != null && Integer.valueOf(200).equals(result.get("code"));
    }
}
