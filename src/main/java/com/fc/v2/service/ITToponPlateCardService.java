package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.model.auto.TToponPlateCardHis;
import com.fc.v2.model.auto.TToponPlateHandler;
import com.fc.v2.model.auto.TToponPlateNumber;
import com.fc.v2.model.custom.PlateRegisterResult;

import java.util.List;
import java.util.Map;

/**
 * 门楼牌号段建档卡 Service接口
 * 旧签名一概不动窝，添新活儿走重载。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITToponPlateCardService {

    /** 按主键查询（现行卡，已删除/已并销的查不到） */
    TToponPlateCard selectTToponPlateCardById(Long id);

    /** 按条件查询列表（分页与数据范围由调用方统一处理） */
    List<TToponPlateCard> selectTToponPlateCardList(Wrapper<TToponPlateCard> queryWrapper);

    /** 新增（旧签名保留：内部即登记，校验不过抛 PlateCardException） */
    int insertTToponPlateCard(TToponPlateCard record);

    /** 修改（旧签名保留：只许动备注/影像，卡号、创建人、容量档一概不动） */
    int updateTToponPlateCard(TToponPlateCard record);

    /** 批量删除（旧签名保留：改为打删除标记的逻辑删除） */
    int deleteTToponPlateCardByIds(String ids);

    /** 按主键删除（逻辑删除） */
    int deleteTToponPlateCardById(Long id);

    // ===== 添的新活儿 =====

    /** 登记立卡：唯一写入口。撞来源号不另起一笔，并成一条回话。 */
    PlateRegisterResult register(TToponPlateCard record);

    /** 两张已立的卡事后认定同号段/同来源号：留存一笔、另一笔打并销，账上只许剩一笔 */
    TToponPlateCard merge(Long keptId, Long droppedId, String remark);

    /** 核对：待核对(0) → 已核对(1) */
    TToponPlateCard check(Long id, String remark);

    /** 定档：已核对(1) → 已定档(2) */
    TToponPlateCard finalize(Long id, String remark);

    /** 街巷改属：旧一笔归历史，容量档照新档案重折，版本号自增，创建人不动 */
    TToponPlateCard rearchive(Long id, Long newSiteId, String remark);

    /** 坐落路名更动：旧一笔归历史，版本号自增，区划归属与号数不动 */
    TToponPlateCard renameRoad(Long id, String newRoadName, String remark);

    /** 号数增减：旧一笔归历史，容量档重折，版本号自增 */
    TToponPlateCard adjustQty(Long id, java.math.BigDecimal qty, java.math.BigDecimal fineAmt, String remark);

    /** 号数增减（重载）：起讫号也一并更动时走这条，号数与号段位须咬得上；传 null 的段位保持不变 */
    TToponPlateCard adjustQty(Long id, java.math.BigDecimal qty, java.math.BigDecimal fineAmt,
                              Integer oddStartNo, Integer oddEndNo, Integer evenStartNo, Integer evenEndNo,
                              String remark);

    /** 新编/续编发号。side=1单号侧 2双号侧；renew=true 时只许续空号，续过的号不许再发 */
    TToponPlateNumber issue(Long id, int side, int plateNo, boolean renew, String remark);

    /** 载入三十年前的老号台账：status 0原号在册 1空号可续 */
    TToponPlateNumber loadLegacy(Long id, int side, int plateNo, int status, Integer issueYear);

    /** 卡的经手人续记 */
    List<TToponPlateHandler> handlers(Long id);

    /** 卡的历史版本 */
    List<TToponPlateCardHis> history(Long id);

    /** 卡的已发号码台账 */
    List<TToponPlateNumber> numbers(Long id);

    /** 市局口径：按本年度新编号数从多到少 */
    List<TToponPlateCard> listByFineAmtDesc();

    /** 区县口径：按最末一级地段（乡镇街道区划代码）归堆数出来的段数 */
    List<Map<String, Object>> countGroupBySite();

    /** 年底对账：两种口径的段数都在这里，须对得上 */
    Map<String, Object> reconcile();
}
