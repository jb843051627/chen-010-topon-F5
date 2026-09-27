package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.PlateCardException;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TToponPlateCardHisMapper;
import com.fc.v2.mapper.auto.TToponPlateCardMapper;
import com.fc.v2.mapper.auto.TToponPlateHandlerMapper;
import com.fc.v2.mapper.auto.TToponPlateMergeMapper;
import com.fc.v2.mapper.auto.TToponPlateNumberMapper;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.model.auto.TToponPlateCardHis;
import com.fc.v2.model.auto.TToponPlateHandler;
import com.fc.v2.model.auto.TToponPlateMerge;
import com.fc.v2.model.auto.TToponPlateNumber;
import com.fc.v2.model.custom.PlateRegisterResult;
import com.fc.v2.service.ITToponDistrictDocService;
import com.fc.v2.service.ITToponPlateCardService;
import com.fc.v2.service.support.PlateGradeCalc;
import com.fc.v2.service.support.PlateScopeResolver;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门楼牌号段建档卡Service业务层处理。
 * 规矩都收在这一层：号段先立卡、后发号，一个段一张卡，卡跟着这条街一辈子。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TToponPlateCardServiceImpl extends ServiceImpl<TToponPlateCardMapper, TToponPlateCard>
        implements ITToponPlateCardService {

    /** 进展 0待核对 */
    private static final int STAGE_TODO = 0;
    /** 进展 1已核对 */
    private static final int STAGE_CHECKED = 1;
    /** 进展 2已定档 */
    private static final int STAGE_FIXED = 2;

    /** 删除标记 0正常 */
    private static final int DEL_OK = 0;
    /** 删除标记 1删除/并卡归档 */
    private static final int DEL_GONE = 1;

    @Autowired
    private TToponPlateHandlerMapper handlerMapper;
    @Autowired
    private TToponPlateNumberMapper numberMapper;
    @Autowired
    private TToponPlateCardHisMapper hisMapper;
    @Autowired
    private TToponPlateMergeMapper mergeMapper;
    @Autowired
    private ITToponDistrictDocService districtDocService;
    @Autowired
    private PlateScopeResolver scopeResolver;

    // ==================== 查询 ====================

    @Override
    public TToponPlateCard selectTToponPlateCardById(Long id) {
        if (id == null) {
            return null;
        }
        TToponPlateCard card = this.baseMapper.selectOne(new QueryWrapper<TToponPlateCard>()
                .eq("id", id).eq("del_flag", DEL_OK));
        if (card != null) {
            guardScope(card);
        }
        return card;
    }

    @Override
    public List<TToponPlateCard> selectTToponPlateCardList(Wrapper<TToponPlateCard> queryWrapper) {
        QueryWrapper<TToponPlateCard> wrapper;
        if (queryWrapper instanceof QueryWrapper) {
            wrapper = (QueryWrapper<TToponPlateCard>) queryWrapper;
        } else {
            wrapper = new QueryWrapper<>();
        }
        wrapper.eq("del_flag", DEL_OK);
        // 权限按层级收口：市局见全市，区县见本区，没绑区县的一笔不见
        applyScope(wrapper);
        wrapper.orderByDesc("fine_amt").orderByDesc("id");
        return this.baseMapper.selectList(wrapper);
    }

    /** 把当前登录人层级范围压进查询条件 */
    private void applyScope(QueryWrapper<TToponPlateCard> wrapper) {
        PlateScopeResolver.Scope scope = scopeResolver.currentScope();
        if (scope.isArea()) {
            wrapper.likeRight("site_no", scope.getAreaCode());
        } else if (!scope.isCity()) {
            // 没有全市权限、又没绑区县：宁紧勿松，一笔不放
            wrapper.apply("1 = 0");
        }
    }

    // ==================== 旧签名（不动窝，内部走新规矩） ====================

    /** 旧新增签名保留：内部即登记；失败抛 PlateCardException，rows 恒为 1 或不收手 */
    @Override
    public int insertTToponPlateCard(TToponPlateCard record) {
        return register(record) != null ? 1 : 0;
    }

    /**
     * 旧修改签名保留：只许补备注/两张影像；卡号、创建人、容量档、档案归属一概不许从这条道动。
     */
    @Override
    public int updateTToponPlateCard(TToponPlateCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        TToponPlateCard db = requireCard(record.getId());
        UpdateWrapper<TToponPlateCard> uw = new UpdateWrapper<TToponPlateCard>()
                .eq("id", db.getId()).eq("del_flag", DEL_OK)
                .set("remark", record.getRemark())
                .set("street_view_img", record.getStreetViewImg() != null ? record.getStreetViewImg() : db.getStreetViewImg())
                .set("plate_sample_img", record.getPlateSampleImg() != null ? record.getPlateSampleImg() : db.getPlateSampleImg())
                .set("update_time", new Date());
        int rows = this.baseMapper.update(null, uw);
        if (rows > 0) {
            appendHandler(db.getId(), "rerecord", record.getRemark());
        }
        return rows;
    }

    @Override
    public int deleteTToponPlateCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        int n = 0;
        for (Long id : idArr) {
            n += deleteTToponPlateCardById(id);
        }
        return n;
    }

    @Override
    public int deleteTToponPlateCardById(Long id) {
        TToponPlateCard db = requireCard(id);
        return this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                .eq("id", db.getId()).eq("del_flag", DEL_OK)
                .set("del_flag", DEL_GONE).set("update_time", new Date()));
    }

    // ==================== 登记立卡（唯一写入口） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlateRegisterResult register(TToponPlateCard record) {
        if (record == null) {
            throw new PlateCardException("card", "登记内容是空的，卡落不下去");
        }
        // 段卡号这类单号系统不代填，空着就是错格
        if (StringUtils.isEmpty(record.getBillNo())) {
            throw new PlateCardException("billNo", "门楼牌号段卡号没填，段卡号不代填");
        }
        if (StringUtils.isEmpty(record.getRoadName())) {
            throw new PlateCardException("roadName", "街巷名称没填");
        }
        if (record.getSiteId() == null) {
            throw new PlateCardException("siteId", "没挂乡镇街道档案，立卡要挂档案、只认有档案的");
        }

        // 同一个号段第二回立卡要挡得住（账上活着的卡，卡号不得重复）
        Integer dup = this.baseMapper.selectCount(new QueryWrapper<TToponPlateCard>()
                .eq("bill_no", record.getBillNo()).eq("del_flag", DEL_OK));
        if (dup != null && dup > 0) {
            throw new PlateCardException("billNo", "段卡号 " + record.getBillNo() + " 已立过卡，同一个号段不许第二回立卡");
        }

        // 区里自报那条与市局抄送那条撞上了：凭来源号并成一条，账上只许剩一笔
        if (StringUtils.isNotEmpty(record.getSourceNo())) {
            TToponPlateCard earlier = this.baseMapper.selectOne(new QueryWrapper<TToponPlateCard>()
                    .eq("source_no", record.getSourceNo()).eq("del_flag", DEL_OK)
                    .orderByAsc("create_time").last("limit 1"));
            if (earlier != null) {
                appendHandler(earlier.getId(), "merge",
                        "来源号 " + record.getSourceNo() + " 又来一条(段卡号 " + record.getBillNo()
                                + ")，已并成一条，账上只此一笔");
                return new PlateRegisterResult(earlier, true,
                        "来源号 " + record.getSourceNo() + " 撞上既有段卡 " + earlier.getBillNo() + "，已并成一条，账上只此一笔");
            }
        }

        TToponDistrictDoc doc = districtDocService.selectById(record.getSiteId());
        if (doc == null) {
            throw new PlateCardException("siteId", "乡镇街道档案不存在，立卡只认有档案的");
        }
        if (doc.getStatus() != null && doc.getStatus() == 1) {
            throw new PlateCardException("siteId", "档案「" + doc.getSiteName() + "」已撤并，往后的新卡挂不进去");
        }
        // 区县经办只能把卡挂在本区档案下；市局不拦
        PlateScopeResolver.Scope scope = scopeResolver.currentScope();
        if (scope.isArea() && (doc.getAreaCode() == null || !doc.getAreaCode().equals(scope.getAreaCode()))) {
            throw new PlateCardException("siteId", "该档案不在你的区县范围，卡不能挂到别的区县名下");
        }
        if (!scope.isCity() && !scope.isArea()) {
            throw new PlateCardException("siteId", "你名下没有区县经办范围，立不了卡");
        }

        // 可编号数：负数、零、高过所属档案三档上限一大截，当场说清是哪一格不对
        BigDecimal qty = record.getQty();
        if (qty == null) {
            throw new PlateCardException("qty", "可编号数没填");
        }
        if (qty.signum() <= 0) {
            throw new PlateCardException("qty", "可编号数填成负数或零了（" + qty.toPlainString() + "），卡落不下去");
        }
        if (!PlateGradeCalc.withinCapacity(qty, doc)) {
            throw new PlateCardException("qty",
                    "可编号数 " + qty.toPlainString() + " 高过档案三档上限 " + doc.getTh3Max().toPlainString() + "，卡落不下去");
        }
        BigDecimal fineAmt = record.getFineAmt() == null ? BigDecimal.ZERO : record.getFineAmt();
        if (fineAmt.signum() < 0) {
            throw new PlateCardException("fineAmt", "本年度新编个数不能是负数");
        }
        if (fineAmt.compareTo(qty) > 0) {
            throw new PlateCardException("fineAmt", "本年度新编个数 " + fineAmt.toPlainString() + " 高过可编号数 " + qty.toPlainString());
        }
        record.setFineAmt(fineAmt);

        // 起号止号都填在卡上：一侧走单号、一侧走双号，号数与号段须咬得上
        int slotTotal = validateRanges(record, qty);

        // 区划归属须咬得上：卡上若带了区划代码，必须与档案一致；正常直接用档案的
        if (StringUtils.isNotEmpty(record.getSiteNo()) && !doc.getSiteNo().equals(record.getSiteNo())) {
            throw new PlateCardException("siteNo",
                    "卡上区划代码 " + record.getSiteNo() + " 与档案 " + doc.getSiteNo() + " 咬不上");
        }
        record.setSiteNo(doc.getSiteNo());

        // 容量档由系统折算，留两位小数；人填的一律不算
        PlateGradeCalc.Grade grade = PlateGradeCalc.convert(qty, doc);
        record.setGradeLevel(grade.getLevel());
        record.setGradeValue(grade.getValue());

        record.setVersionNo(1);
        record.setStatus(STAGE_TODO);
        record.setDelFlag(DEL_OK);
        // 创建人：谁按下立卡这一下，这名就记在谁身上
        record.setFounderName(currentLoginName());

        this.baseMapper.insert(record);
        appendHandler(record.getId(), "register",
                "立卡：" + doc.getSiteName() + "，可编 " + qty.toPlainString() + " 个，号段位合计 " + slotTotal + " 个");
        return new PlateRegisterResult(record, false, "登记成功，段卡号 " + record.getBillNo() + " 已立卡，进展：待核对");
    }

    /** 校验单侧起讫号：正数、起≤止、单号侧全单、双号侧全双，两侧位合计须等于可编号数 */
    private int validateRanges(TToponPlateCard r, BigDecimal qty) {
        int oddCount = sideCount(r.getOddStartNo(), r.getOddEndNo(), TToponPlateNumber.SIDE_ODD, "oddStartNo", "oddEndNo");
        int evenCount = sideCount(r.getEvenStartNo(), r.getEvenEndNo(), TToponPlateNumber.SIDE_EVEN, "evenStartNo", "evenEndNo");
        int slotTotal = oddCount + evenCount;
        if (qty.stripTrailingZeros().scale() > 0 || qty.intValue() != slotTotal) {
            throw new PlateCardException("qty",
                    "可编号数 " + qty.toPlainString() + " 与起讫号段位合计 " + slotTotal + "(单" + oddCount + "+双" + evenCount + ")咬不上");
        }
        return slotTotal;
    }

    private int sideCount(Integer start, Integer end, int side, String startField, String endField) {
        if (start == null || end == null) {
            throw new PlateCardException(startField, side == TToponPlateNumber.SIDE_ODD ? "单号侧起号/止号没填齐" : "双号侧起号/止号没填齐");
        }
        if (start <= 0 || end <= 0) {
            throw new PlateCardException(startField, "起号止号得是正整数");
        }
        if (start > end) {
            throw new PlateCardException(endField, "止号小过起号了");
        }
        int parity = side == TToponPlateNumber.SIDE_ODD ? 1 : 0;
        if (start % 2 != parity || end % 2 != parity) {
            throw new PlateCardException(startField,
                    side == TToponPlateNumber.SIDE_ODD ? "单号侧起讫号得都是单数" : "双号侧起讫号得都是双数");
        }
        return (end - start) / 2 + 1;
    }

    // ==================== 并卡：账上只许剩一笔 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard merge(Long keptId, Long droppedId, String remark) {
        if (keptId == null || droppedId == null) {
            throw new PlateCardException("id", "留存卡与并销卡都得指明");
        }
        if (keptId.equals(droppedId)) {
            throw new PlateCardException("id", "留存卡与并销卡不能是同一张");
        }
        TToponPlateCard kept = requireCard(keptId);
        TToponPlateCard dropped = requireCard(droppedId);
        if (StringUtils.isEmpty(kept.getBillNo())) {
            throw new PlateCardException("billNo", "留存卡没有段卡号");
        }
        // 同一条街、同一来源号才许并；来源号都空的两笔不许乱并
        if (StringUtils.isEmpty(kept.getSourceNo()) || StringUtils.isEmpty(dropped.getSourceNo())
                || !kept.getSourceNo().equals(dropped.getSourceNo())) {
            throw new PlateCardException("sourceNo", "两张卡来源号不一致，不能并成一笔");
        }
        // 被并销卡的号码台账并到留存卡下；真撞号（同号侧同号且都在册）要拦
        List<TToponPlateNumber> dropNumbers = numberMapper.selectList(new QueryWrapper<TToponPlateNumber>()
                .eq("card_id", droppedId));
        for (TToponPlateNumber n : dropNumbers) {
            TToponPlateNumber clash = numberMapper.selectOne(new QueryWrapper<TToponPlateNumber>()
                    .eq("card_id", keptId).eq("side", n.getSide()).eq("plate_no", n.getPlateNo()));
            if (clash != null) {
                throw new PlateCardException("plateNo",
                        "并卡撞号：" + sideName(n.getSide()) + n.getPlateNo() + " 两张卡都在册，先核实同号不同址再并");
            }
            n.setCardId(keptId);
            numberMapper.updateById(n);
        }
        // 并销那笔打删除标记，账上不再单列；历史可凭并卡台账追溯
        this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                .eq("id", droppedId).eq("del_flag", DEL_OK)
                .set("del_flag", DEL_GONE).set("update_time", new Date()));
        TToponPlateMerge m = new TToponPlateMerge();
        m.setSourceNo(kept.getSourceNo());
        m.setKeptCardId(keptId);
        m.setDroppedCardId(droppedId);
        m.setCreateBy(currentLoginName());
        m.setCreateTime(new Date());
        mergeMapper.insert(m);
        appendHandler(keptId, "merge",
                "并卡：留存本卡，并销 " + dropped.getBillNo() + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        return kept;
    }

    // ==================== 流转：核对 / 定档 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard check(Long id, String remark) {
        TToponPlateCard card = requireCard(id);
        if (card.getStatus() != null && card.getStatus() >= STAGE_CHECKED) {
            throw new PlateCardException("status", "该卡已核对过，不必再核；要改请走改属/号数增减");
        }
        card.setStatus(STAGE_CHECKED);
        persistStage(card);
        appendHandler(card.getId(), "check", remark);
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard finalize(Long id, String remark) {
        TToponPlateCard card = requireCard(id);
        if (card.getStatus() == null || card.getStatus() < STAGE_CHECKED) {
            throw new PlateCardException("status", "还没核对，定不了档");
        }
        if (card.getStatus() == STAGE_FIXED) {
            throw new PlateCardException("status", "该卡已定档");
        }
        card.setStatus(STAGE_FIXED);
        persistStage(card);
        appendHandler(card.getId(), "finalize", remark);
        return card;
    }

    private void persistStage(TToponPlateCard card) {
        this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                .eq("id", card.getId()).eq("del_flag", DEL_OK)
                .set("status", card.getStatus()).set("update_time", new Date()));
    }

    // ==================== 改属 / 号数增减（旧的归历史，现行只留最新） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard rearchive(Long id, Long newSiteId, String remark) {
        TToponPlateCard card = requireCard(id);
        if (newSiteId == null) {
            throw new PlateCardException("siteId", "没指明改挂哪个档案");
        }
        if (newSiteId.equals(card.getSiteId())) {
            throw new PlateCardException("siteId", "新旧档案是同一个，不算改属");
        }
        TToponDistrictDoc doc = districtDocService.selectById(newSiteId);
        if (doc == null) {
            throw new PlateCardException("siteId", "新档案不存在，挂不进去");
        }
        if (doc.getStatus() != null && doc.getStatus() == 1) {
            throw new PlateCardException("siteId", "新档案「" + doc.getSiteName() + "」已撤并，挂不进去");
        }
        if (!PlateGradeCalc.withinCapacity(card.getQty(), doc)) {
            throw new PlateCardException("qty",
                    "可编号数 " + card.getQty().toPlainString() + " 高过新档案三档上限 " + doc.getTh3Max().toPlainString() + "，改属落不下去");
        }

        archiveHistory(card, TToponPlateCardHis.CHANGE_REARCHIVE);

        PlateGradeCalc.Grade grade = PlateGradeCalc.convert(card.getQty(), doc);
        card.setSiteId(doc.getId());
        card.setSiteNo(doc.getSiteNo());
        card.setGradeLevel(grade.getLevel());
        card.setGradeValue(grade.getValue());
        card.setVersionNo(card.getVersionNo() == null ? 1 : card.getVersionNo() + 1);
        this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                .eq("id", card.getId()).eq("del_flag", DEL_OK)
                .set("site_id", card.getSiteId())
                .set("site_no", card.getSiteNo())
                .set("grade_level", grade.getLevel())
                .set("grade_value", grade.getValue())
                .set("version_no", card.getVersionNo())
                .set("update_time", new Date()));
        // 创建人不挪、旧签名不动窝；只续记一笔经手
        appendHandler(card.getId(), "rearchive",
                "改属：" + card.getBillNo() + " 改挂「" + doc.getSiteName() + "」" + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard renameRoad(Long id, String newRoadName, String remark) {
        TToponPlateCard card = requireCard(id);
        if (StringUtils.isEmpty(newRoadName)) {
            throw new PlateCardException("roadName", "新路名没填，坐落路名不能更成空的");
        }
        if (newRoadName.equals(card.getRoadName())) {
            throw new PlateCardException("roadName", "新旧路名一个样，不算更动");
        }
        String oldName = card.getRoadName();
        archiveHistory(card, TToponPlateCardHis.CHANGE_ROAD_NAME);
        card.setRoadName(newRoadName);
        card.setVersionNo(card.getVersionNo() == null ? 1 : card.getVersionNo() + 1);
        this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                .eq("id", card.getId()).eq("del_flag", DEL_OK)
                .set("road_name", newRoadName)
                .set("version_no", card.getVersionNo())
                .set("update_time", new Date()));
        appendHandler(card.getId(), "renameRoad",
                "路名更动：「" + oldName + "」改为「" + newRoadName + "」"
                        + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        return card;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard adjustQty(Long id, BigDecimal qty, BigDecimal fineAmt, String remark) {
        return adjustQty(id, qty, fineAmt, null, null, null, null, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateCard adjustQty(Long id, BigDecimal qty, BigDecimal fineAmt,
                                     Integer oddStartNo, Integer oddEndNo,
                                     Integer evenStartNo, Integer evenEndNo, String remark) {
        TToponPlateCard card = requireCard(id);
        if (qty == null) {
            throw new PlateCardException("qty", "可编号数没填");
        }
        if (qty.signum() <= 0) {
            throw new PlateCardException("qty", "可编号数填成负数或零了（" + qty.toPlainString() + "），卡落不下去");
        }
        TToponDistrictDoc doc = districtDocService.selectById(card.getSiteId());
        if (doc == null) {
            throw new PlateCardException("siteId", "挂的档案找不着了，先查档案");
        }
        if (!PlateGradeCalc.withinCapacity(qty, doc)) {
            throw new PlateCardException("qty",
                    "可编号数 " + qty.toPlainString() + " 高过档案三档上限 " + doc.getTh3Max().toPlainString());
        }
        if (fineAmt != null && fineAmt.signum() < 0) {
            throw new PlateCardException("fineAmt", "本年度新编个数不能是负数");
        }
        if (fineAmt != null && fineAmt.compareTo(qty) > 0) {
            throw new PlateCardException("fineAmt", "本年度新编个数高过可编号数");
        }

        // 起讫号段位：传了就校验新段位与新号数咬得上；没传沿用旧的，那也得与新号数咬得上
        TToponPlateCard probe = new TToponPlateCard();
        probe.setOddStartNo(oddStartNo != null ? oddStartNo : card.getOddStartNo());
        probe.setOddEndNo(oddEndNo != null ? oddEndNo : card.getOddEndNo());
        probe.setEvenStartNo(evenStartNo != null ? evenStartNo : card.getEvenStartNo());
        probe.setEvenEndNo(evenEndNo != null ? evenEndNo : card.getEvenEndNo());
        validateRanges(probe, qty);

        archiveHistory(card, TToponPlateCardHis.CHANGE_READJ_QTY);

        PlateGradeCalc.Grade grade = PlateGradeCalc.convert(qty, doc);
        card.setQty(qty);
        if (fineAmt != null) {
            card.setFineAmt(fineAmt);
        }
        card.setGradeLevel(grade.getLevel());
        card.setGradeValue(grade.getValue());
        card.setVersionNo(card.getVersionNo() == null ? 1 : card.getVersionNo() + 1);
        UpdateWrapper<TToponPlateCard> uw = new UpdateWrapper<TToponPlateCard>()
                .eq("id", card.getId()).eq("del_flag", DEL_OK)
                .set("qty", qty)
                .set("grade_level", grade.getLevel())
                .set("grade_value", grade.getValue())
                .set("version_no", card.getVersionNo())
                .set("update_time", new Date());
        if (fineAmt != null) {
            uw.set("fine_amt", fineAmt);
        }
        if (oddStartNo != null) {
            uw.set("odd_start_no", probe.getOddStartNo());
            card.setOddStartNo(probe.getOddStartNo());
        }
        if (oddEndNo != null) {
            uw.set("odd_end_no", probe.getOddEndNo());
            card.setOddEndNo(probe.getOddEndNo());
        }
        if (evenStartNo != null) {
            uw.set("even_start_no", probe.getEvenStartNo());
            card.setEvenStartNo(probe.getEvenStartNo());
        }
        if (evenEndNo != null) {
            uw.set("even_end_no", probe.getEvenEndNo());
            card.setEvenEndNo(probe.getEvenEndNo());
        }
        this.baseMapper.update(null, uw);
        appendHandler(card.getId(), "readj",
                "号数增减：可编改为 " + qty.toPlainString()
                        + (fineAmt != null ? "，本年新编改为 " + fineAmt.toPlainString() : "")
                        + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        return card;
    }

    /** 把现行卡旧值快照归历史 */
    private void archiveHistory(TToponPlateCard card, int changeType) {
        TToponPlateCardHis his = new TToponPlateCardHis();
        his.setCardId(card.getId());
        his.setVersionNo(card.getVersionNo());
        his.setChangeType(changeType);
        his.setBillNo(card.getBillNo());
        his.setRoadName(card.getRoadName());
        his.setSiteId(card.getSiteId());
        his.setSiteNo(card.getSiteNo());
        his.setQty(card.getQty());
        his.setFineAmt(card.getFineAmt());
        his.setGradeLevel(card.getGradeLevel());
        his.setGradeValue(card.getGradeValue());
        his.setOddStartNo(card.getOddStartNo());
        his.setOddEndNo(card.getOddEndNo());
        his.setEvenStartNo(card.getEvenStartNo());
        his.setEvenEndNo(card.getEvenEndNo());
        his.setCreateBy(currentLoginName());
        his.setCreateTime(new Date());
        hisMapper.insert(his);
    }

    // ==================== 发号：老号可续，续过不许再发 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateNumber issue(Long id, int side, int plateNo, boolean renew, String remark) {
        TToponPlateCard card = requireCard(id);
        if (side != TToponPlateNumber.SIDE_ODD && side != TToponPlateNumber.SIDE_EVEN) {
            throw new PlateCardException("side", "号侧只认 1单号侧 / 2双号侧");
        }
        if (plateNo <= 0) {
            throw new PlateCardException("plateNo", "门牌号得是正整数");
        }
        int parity = side == TToponPlateNumber.SIDE_ODD ? 1 : 0;
        if (plateNo % 2 != parity) {
            throw new PlateCardException("plateNo",
                    side == TToponPlateNumber.SIDE_ODD ? plateNo + " 是双数，不能落在单号侧" : plateNo + " 是单数，不能落在双号侧");
        }
        Integer start = side == TToponPlateNumber.SIDE_ODD ? card.getOddStartNo() : card.getEvenStartNo();
        Integer end = side == TToponPlateNumber.SIDE_ODD ? card.getOddEndNo() : card.getEvenEndNo();
        if (start == null || end == null || plateNo < start || plateNo > end) {
            throw new PlateCardException("plateNo",
                    plateNo + " 不在本卡" + (side == TToponPlateNumber.SIDE_ODD ? "单号侧" : "双号侧") + "起讫号 " + start + "-" + end + " 之内");
        }

        TToponPlateNumber row = numberMapper.selectOne(new QueryWrapper<TToponPlateNumber>()
                .eq("card_id", card.getId()).eq("side", side).eq("plate_no", plateNo));
        int year = 1900 + new Date().getYear();
        if (row == null) {
            if (renew) {
                throw new PlateCardException("plateNo", plateNo + " 没有空出的老号可续，要续编先载入老号台账");
            }
            row = new TToponPlateNumber();
            row.setCardId(card.getId());
            row.setSide(side);
            row.setPlateNo(plateNo);
            row.setSourceType(TToponPlateNumber.SOURCE_NEW);
            row.setStatus(TToponPlateNumber.STATE_ORIGINAL);
            row.setIssueYear(year);
            numberMapper.insert(row);
            // 本年度新编个数 +1（续编的老号不计新编）；发满可编号数就挡住
            BigDecimal fine = card.getFineAmt() == null ? BigDecimal.ZERO : card.getFineAmt();
            if (fine.add(BigDecimal.ONE).compareTo(card.getQty()) > 0) {
                throw new PlateCardException("fineAmt",
                        "再发一个，本年度新编个数就高过可编号数 " + card.getQty().toPlainString() + " 了");
            }
            this.baseMapper.update(null, new UpdateWrapper<TToponPlateCard>()
                    .eq("id", card.getId()).eq("del_flag", DEL_OK)
                    .set("fine_amt", fine.add(BigDecimal.ONE)).set("update_time", new Date()));
            appendHandler(card.getId(), "issue", "新编号牌 " + sideName(side) + plateNo + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        } else if (renew) {
            // 中间空出的老号可以续编；续过的号不许再发第二遍
            if (row.getStatus() != null && row.getStatus() == TToponPlateNumber.STATE_RENEWED) {
                throw new PlateCardException("plateNo", sideName(side) + plateNo + " 已续编过一回，续过的号不许再发第二遍");
            }
            if (row.getStatus() != null && row.getStatus() == TToponPlateNumber.STATE_ORIGINAL) {
                throw new PlateCardException("plateNo", sideName(side) + plateNo + " 原号在册，不是空号，续不得");
            }
            row.setSourceType(TToponPlateNumber.SOURCE_RENEW);
            row.setStatus(TToponPlateNumber.STATE_RENEWED);
            row.setIssueYear(year);
            numberMapper.updateById(row);
            appendHandler(card.getId(), "issue", "续编老号 " + sideName(side) + plateNo + (StringUtils.isNotEmpty(remark) ? "；" + remark : ""));
        } else {
            // 撞上三十年前发出去的老号也在这一档挡下
            throw new PlateCardException("plateNo",
                    sideName(side) + plateNo + " 已在号册（" + (row.getStatus() == TToponPlateNumber.STATE_RENEWED ? "续号在册" : "原号在册") + "，" + row.getIssueYear() + "年），同号不许再发");
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TToponPlateNumber loadLegacy(Long id, int side, int plateNo, int status, Integer issueYear) {
        TToponPlateCard card = requireCard(id);
        if (side != TToponPlateNumber.SIDE_ODD && side != TToponPlateNumber.SIDE_EVEN) {
            throw new PlateCardException("side", "号侧只认 1单号侧 / 2双号侧");
        }
        if (plateNo <= 0) {
            throw new PlateCardException("plateNo", "门牌号得是正整数");
        }
        if (status != TToponPlateNumber.STATE_ORIGINAL && status != TToponPlateNumber.STATE_VACANT) {
            throw new PlateCardException("status", "老号只认 0原号在册 / 1空号可续");
        }
        TToponPlateNumber exist = numberMapper.selectOne(new QueryWrapper<TToponPlateNumber>()
                .eq("card_id", card.getId()).eq("side", side).eq("plate_no", plateNo));
        if (exist != null) {
            throw new PlateCardException("plateNo", sideName(side) + plateNo + " 号册里已有这笔，不能重复载入");
        }
        TToponPlateNumber row = new TToponPlateNumber();
        row.setCardId(card.getId());
        row.setSide(side);
        row.setPlateNo(plateNo);
        row.setSourceType(TToponPlateNumber.SOURCE_NEW);
        row.setStatus(status);
        row.setIssueYear(issueYear);
        numberMapper.insert(row);
        appendHandler(card.getId(), "loadLegacy",
                "载入老号 " + sideName(side) + plateNo + "（" + (status == TToponPlateNumber.STATE_VACANT ? "空号可续" : "原号在册")
                        + (issueYear != null ? "，" + issueYear + "年" : "") + "）");
        return row;
    }

    // ==================== 明细三页 ====================

    @Override
    public List<TToponPlateHandler> handlers(Long id) {
        requireCard(id);
        return handlerMapper.selectList(new QueryWrapper<TToponPlateHandler>()
                .eq("card_id", id).orderByAsc("create_time").orderByAsc("id"));
    }

    @Override
    public List<TToponPlateCardHis> history(Long id) {
        requireCard(id);
        return hisMapper.selectList(new QueryWrapper<TToponPlateCardHis>()
                .eq("card_id", id).orderByDesc("version_no").orderByDesc("create_time"));
    }

    @Override
    public List<TToponPlateNumber> numbers(Long id) {
        requireCard(id);
        return numberMapper.selectList(new QueryWrapper<TToponPlateNumber>()
                .eq("card_id", id).orderByAsc("side").orderByAsc("plate_no"));
    }

    // ==================== 对账：两条路数出来的段数须对得上 ====================

    @Override
    public List<TToponPlateCard> listByFineAmtDesc() {
        QueryWrapper<TToponPlateCard> w = new QueryWrapper<TToponPlateCard>().eq("del_flag", DEL_OK);
        applyScope(w);
        w.orderByDesc("fine_amt").orderByDesc("id");
        return this.baseMapper.selectList(w);
    }

    @Override
    public List<Map<String, Object>> countGroupBySite() {
        QueryWrapper<TToponPlateCard> w = new QueryWrapper<TToponPlateCard>()
                .select("site_no as siteNo", "count(*) as segCount", "max(site_id) as siteId")
                .eq("del_flag", DEL_OK);
        applyScope(w);
        w.groupBy("site_no").orderByDesc("segCount").orderByAsc("site_no");
        return this.baseMapper.selectMaps(w);
    }

    @Override
    public Map<String, Object> reconcile() {
        List<TToponPlateCard> cityView = listByFineAmtDesc();
        List<Map<String, Object>> groups = countGroupBySite();
        long groupTotal = 0L;
        for (Map<String, Object> g : groups) {
            Object c = g.get("segCount");
            if (c instanceof Number) {
                groupTotal += ((Number) c).longValue();
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("cityTotal", cityView.size());
        out.put("groupTotal", groupTotal);
        out.put("matched", cityView.size() == groupTotal);
        out.put("groups", groups);
        return out;
    }

    // ==================== 内务 ====================

    private TToponPlateCard requireCard(Long id) {
        if (id == null) {
            throw new PlateCardException("id", "段卡id没给");
        }
        TToponPlateCard card = this.baseMapper.selectOne(new QueryWrapper<TToponPlateCard>()
                .eq("id", id).eq("del_flag", DEL_OK));
        if (card == null) {
            throw new PlateCardException("id", "段卡不存在或已删除/并卡归档");
        }
        guardScope(card);
        return card;
    }

    /** 区县的人不许碰别区的卡；没范围的谁也不许碰；市局与本区人点开的是同一笔账 */
    private void guardScope(TToponPlateCard card) {
        PlateScopeResolver.Scope scope = scopeResolver.currentScope();
        if (scope.isCity()) {
            return;
        }
        if (!scope.isArea()) {
            throw new PlateCardException("siteNo", "你名下没有区县经办范围，看不了这条段卡");
        }
        if (card.getSiteNo() == null || !card.getSiteNo().startsWith(scope.getAreaCode())) {
            throw new PlateCardException("siteNo", "这条段卡在别的区县，不在你的清单范围内");
        }
    }

    private void appendHandler(Long cardId, String action, String remark) {
        TToponPlateHandler h = new TToponPlateHandler();
        h.setCardId(cardId);
        h.setHandlerName(currentLoginName());
        h.setAction(action);
        h.setRemark(remark);
        h.setCreateBy(currentLoginName());
        h.setCreateTime(new Date());
        handlerMapper.insert(h);
    }

    private String currentLoginName() {
        try {
            return ShiroUtils.getLoginName();
        } catch (RuntimeException e) {
            return "Task";
        }
    }

    private String sideName(int side) {
        return side == TToponPlateNumber.SIDE_ODD ? "单号侧" : "双号侧";
    }
}
