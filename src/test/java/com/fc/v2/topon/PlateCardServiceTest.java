package com.fc.v2.topon;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.PlateCardException;
import com.fc.v2.mapper.auto.TToponPlateCardHisMapper;
import com.fc.v2.mapper.auto.TToponPlateCardMapper;
import com.fc.v2.mapper.auto.TToponPlateHandlerMapper;
import com.fc.v2.mapper.auto.TToponPlateMergeMapper;
import com.fc.v2.mapper.auto.TToponPlateNumberMapper;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.model.auto.TToponPlateNumber;
import com.fc.v2.model.custom.PlateRegisterResult;
import com.fc.v2.service.impl.TToponPlateCardServiceImpl;
import com.fc.v2.service.support.PlateGradeCalc;
import com.fc.v2.service.support.PlateScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 段卡服务层核心规矩：
 * 卡号牙口、负数/零/超档当场指格、容量档系统折、撞来源号并一笔、
 * 续号不重发、改属归历史创建人不挪、层级范围、两路口径对账。
 */
class PlateCardServiceTest {

    private TToponPlateCardMapper cardMapper;
    private TToponPlateNumberMapper numberMapper;
    private TToponPlateHandlerMapper handlerMapper;
    private TToponPlateCardHisMapper hisMapper;
    private TToponPlateMergeMapper mergeMapper;
    private PlateScopeResolver scopeResolver;
    private com.fc.v2.service.ITToponDistrictDocService docService;
    private TToponPlateCardServiceImpl service;

    @BeforeEach
    void setUp() {
        cardMapper = mock(TToponPlateCardMapper.class);
        numberMapper = mock(TToponPlateNumberMapper.class);
        handlerMapper = mock(TToponPlateHandlerMapper.class);
        hisMapper = mock(TToponPlateCardHisMapper.class);
        mergeMapper = mock(TToponPlateMergeMapper.class);
        docService = mock(com.fc.v2.service.ITToponDistrictDocService.class);
        scopeResolver = mock(PlateScopeResolver.class);

        service = new TToponPlateCardServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", cardMapper);
        ReflectionTestUtils.setField(service, "numberMapper", numberMapper);
        ReflectionTestUtils.setField(service, "handlerMapper", handlerMapper);
        ReflectionTestUtils.setField(service, "hisMapper", hisMapper);
        ReflectionTestUtils.setField(service, "mergeMapper", mergeMapper);
        ReflectionTestUtils.setField(service, "districtDocService", docService);
        ReflectionTestUtils.setField(service, "scopeResolver", scopeResolver);

        when(scopeResolver.currentScope())
                .thenReturn(PlateScopeResolver.Scope.city());
        when(cardMapper.insert(any(TToponPlateCard.class))).thenAnswer(inv -> {
            ((TToponPlateCard) inv.getArgument(0)).setId(100L);
            return 1;
        });
    }

    private TToponDistrictDoc activeDoc() {
        TToponDistrictDoc d = new TToponDistrictDoc();
        d.setId(0L);
        d.setAreaCode("361002");
        d.setSiteNo("361002000");
        d.setSiteName("明德街道办事处基础档案");
        d.setStatus(0);
        d.setTh1Max(new BigDecimal("40"));
        d.setTh2Max(new BigDecimal("200"));
        d.setTh3Max(new BigDecimal("600"));
        return d;
    }

    private TToponPlateCard form() {
        TToponPlateCard c = new TToponPlateCard();
        c.setBillNo("MP-2026-001");
        c.setRoadName("明德路");
        c.setSiteId(0L);
        c.setQty(new BigDecimal("30"));
        c.setFineAmt(new BigDecimal("0"));
        // 单号侧 1..29 共15、双号侧 2..30 共15，合计30
        c.setOddStartNo(1);
        c.setOddEndNo(29);
        c.setEvenStartNo(2);
        c.setEvenEndNo(30);
        return c;
    }

    @Test
    void registerHappyPathSystemFillsGradeAndFounder() {
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(docService.selectById(0L)).thenReturn(activeDoc());

        PlateRegisterResult r = service.register(form());

        assertFalse(r.isMerged());
        TToponPlateCard saved = r.getCard();
        assertEquals("361002000", saved.getSiteNo());
        assertEquals(1, saved.getGradeLevel());
        assertEquals(new BigDecimal("0.75"), saved.getGradeValue());
        assertEquals(0, saved.getStatus());
        assertEquals(1, saved.getVersionNo());
        assertEquals("Task", saved.getFounderName());
        // 容量档是人给的也得被系统折算覆盖
        verify(handlerMapper).insert(any());
    }

    @Test
    void registerRejectsDuplicateBillNo() {
        when(cardMapper.selectCount(any())).thenReturn(1);
        PlateCardException ex = assertThrows(PlateCardException.class, () -> service.register(form()));
        assertEquals("billNo", ex.getField());
        verify(cardMapper, never()).insert(any());
    }

    @Test
    void registerRejectsMissingBillNo() {
        TToponPlateCard f = form();
        f.setBillNo("  ");
        PlateCardException ex = assertThrows(PlateCardException.class, () -> service.register(f));
        assertEquals("billNo", ex.getField());
    }

    @Test
    void registerRejectsNegativeZeroAndOverCapacityQty() {
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(docService.selectById(0L)).thenReturn(activeDoc());

        TToponPlateCard zero = form();
        zero.setQty(BigDecimal.ZERO);
        assertEquals("qty", assertThrows(PlateCardException.class, () -> service.register(zero)).getField());

        TToponPlateCard neg = form();
        neg.setQty(new BigDecimal("-5"));
        assertEquals("qty", assertThrows(PlateCardException.class, () -> service.register(neg)).getField());

        TToponPlateCard over = form();
        over.setQty(new BigDecimal("601"));
        over.setOddEndNo(601);
        over.setEvenEndNo(600);
        assertEquals("qty", assertThrows(PlateCardException.class, () -> service.register(over)).getField());
    }

    @Test
    void registerRejectsAbolishedOrMissingArchive() {
        when(cardMapper.selectCount(any())).thenReturn(0);
        TToponDistrictDoc gone = activeDoc();
        gone.setStatus(1);
        when(docService.selectById(0L)).thenReturn(gone);
        assertEquals("siteId", assertThrows(PlateCardException.class, () -> service.register(form())).getField());

        when(docService.selectById(0L)).thenReturn(null);
        assertEquals("siteId", assertThrows(PlateCardException.class, () -> service.register(form())).getField());
    }

    @Test
    void registerRejectsQtyNotMatchingRangesAndBadParity() {
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(docService.selectById(0L)).thenReturn(activeDoc());

        TToponPlateCard mismatch = form();
        mismatch.setQty(new BigDecimal("28")); // 号段仍是30个位
        assertEquals("qty", assertThrows(PlateCardException.class, () -> service.register(mismatch)).getField());

        TToponPlateCard parity = form();
        parity.setOddStartNo(2); // 单号侧起了双号
        assertEquals("oddStartNo", assertThrows(PlateCardException.class, () -> service.register(parity)).getField());
    }

    @Test
    void registerMergesIntoExistingCardBySourceNo() {
        when(cardMapper.selectCount(any())).thenReturn(0);
        TToponPlateCard earlier = form();
        earlier.setId(7L);
        earlier.setBillNo("MP-2026-001");
        when(cardMapper.selectOne(any())).thenReturn(earlier);

        TToponPlateCard second = form();
        second.setBillNo("MP-2026-088");
        second.setSourceNo("SRC-9");
        PlateRegisterResult r = service.register(second);

        assertTrue(r.isMerged());
        assertEquals(7L, r.getCard().getId());
        // 并成一条，账上不许另起一笔
        verify(cardMapper, never()).insert(any());
        verify(handlerMapper).insert(any());
    }

    // ---------- 发号：老号可续，续过不许再发 ----------

    private TToponPlateCard persistedCard() {
        TToponPlateCard c = form();
        c.setId(100L);
        c.setStatus(2);
        c.setVersionNo(1);
        c.setFounderName("张三");
        c.setFineAmt(new BigDecimal("0"));
        when(cardMapper.selectOne(any())).thenReturn(c);
        return c;
    }

    @Test
    void issueNewNumberIncrementsFineAmt() {
        persistedCard();
        when(numberMapper.selectOne(any())).thenReturn(null);

        TToponPlateNumber n = service.issue(100L, TToponPlateNumber.SIDE_ODD, 3, false, null);

        assertEquals(TToponPlateNumber.SOURCE_NEW, n.getSourceType());
        assertEquals(TToponPlateNumber.STATE_ORIGINAL, n.getStatus());
        ArgumentCaptor<UpdateWrapper<TToponPlateCard>> cap = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(cardMapper).update(org.mockito.ArgumentMatchers.isNull(), cap.capture());
        // 新编一个，本年度新编个数写回（值是参数化绑定，认列名即可）
        assertTrue(cap.getValue().getSqlSet().contains("fine_amt="));
    }

    @Test
    void issueRejectsOriginalNumberAgain() {
        persistedCard();
        TToponPlateNumber old = new TToponPlateNumber();
        old.setStatus(TToponPlateNumber.STATE_ORIGINAL);
        old.setIssueYear(1996);
        when(numberMapper.selectOne(any())).thenReturn(old);

        PlateCardException ex = assertThrows(PlateCardException.class,
                () -> service.issue(100L, TToponPlateNumber.SIDE_ODD, 3, false, null));
        assertEquals("plateNo", ex.getField());
    }

    @Test
    void renewVacantThenCannotRenewAgain() {
        persistedCard();
        TToponPlateNumber vacant = new TToponPlateNumber();
        vacant.setId(5L);
        vacant.setStatus(TToponPlateNumber.STATE_VACANT);
        when(numberMapper.selectOne(any())).thenReturn(vacant);

        TToponPlateNumber renewed = service.issue(100L, TToponPlateNumber.SIDE_EVEN, 8, true, null);
        assertEquals(TToponPlateNumber.STATE_RENEWED, renewed.getStatus());
        assertEquals(TToponPlateNumber.SOURCE_RENEW, renewed.getSourceType());
        verify(numberMapper).updateById(any());

        // 续过的号不许再发第二遍
        vacant.setStatus(TToponPlateNumber.STATE_RENEWED);
        PlateCardException ex = assertThrows(PlateCardException.class,
                () -> service.issue(100L, TToponPlateNumber.SIDE_EVEN, 8, true, null));
        assertEquals("plateNo", ex.getField());
    }

    @Test
    void issueRejectsWrongParityAndOutOfRange() {
        persistedCard();
        // 双数落到单号侧
        assertEquals("plateNo", assertThrows(PlateCardException.class,
                () -> service.issue(100L, TToponPlateNumber.SIDE_ODD, 4, false, null)).getField());
        // 超出止号
        assertEquals("plateNo", assertThrows(PlateCardException.class,
                () -> service.issue(100L, TToponPlateNumber.SIDE_ODD, 99, false, null)).getField());
    }

    // ---------- 改属：旧的归历史，创建人不挪，容量档重折 ----------

    @Test
    void rearchiveArchivesHistoryRecomputesGradeAndKeepsFounder() {
        TToponPlateCard c = persistedCard();
        c.setQty(new BigDecimal("30"));
        TToponDistrictDoc oldDoc = activeDoc();
        TToponDistrictDoc newDoc = activeDoc();
        newDoc.setId(2L);
        newDoc.setSiteNo("361002001");
        newDoc.setTh1Max(new BigDecimal("10"));
        newDoc.setTh2Max(new BigDecimal("100"));
        newDoc.setTh3Max(new BigDecimal("300"));
        when(docService.selectById(2L)).thenReturn(newDoc);
        when(docService.selectById(0L)).thenReturn(oldDoc);

        TToponPlateCard out = service.rearchive(100L, 2L, null);

        assertEquals("361002001", out.getSiteNo());
        assertEquals(2, out.getGradeLevel()); // 30/100 档
        assertEquals(2, out.getVersionNo());
        assertEquals("张三", out.getFounderName()); // 旧签名不动窝
        verify(hisMapper).insert(any());
        verify(handlerMapper).insert(any());
    }

    @Test
    void rearchiveRejectsAbolishedTarget() {
        persistedCard();
        TToponDistrictDoc gone = activeDoc();
        gone.setId(3L);
        gone.setStatus(1);
        when(docService.selectById(3L)).thenReturn(gone);
        assertEquals("siteId", assertThrows(PlateCardException.class,
                () -> service.rearchive(100L, 3L, null)).getField());
        verify(hisMapper, never()).insert(any());
    }

    @Test
    void adjustQtyWithRangesRecomputesAndArchives() {
        TToponPlateCard c = persistedCard();
        c.setQty(new BigDecimal("30"));
        when(docService.selectById(0L)).thenReturn(activeDoc());

        // 改成50个：单1..49(25) 双2..50(25)；容量档 50 在 40..200 间进二档
        TToponPlateCard out = service.adjustQty(100L, new BigDecimal("50"), null, 1, 49, 2, 50, "延长铺面");
        assertEquals(2, out.getGradeLevel());
        assertEquals(new BigDecimal("0.25"), out.getGradeValue());
        assertEquals(2, out.getVersionNo());
        assertEquals(Integer.valueOf(49), out.getOddEndNo());
        assertEquals("张三", out.getFounderName());
        verify(hisMapper).insert(any());
    }

    @Test
    void adjustQtyRejectsWhenRangesDoNotMatch() {
        TToponPlateCard c = persistedCard();
        c.setQty(new BigDecimal("30"));
        when(docService.selectById(0L)).thenReturn(activeDoc());
        // 说要50个，段位仍是30个位（null 沿用旧的）
        assertEquals("qty", assertThrows(PlateCardException.class,
                () -> service.adjustQty(100L, new BigDecimal("50"), null, null, null, null, null, null)).getField());
        verify(hisMapper, never()).insert(any());
    }

    @Test
    void renameRoadArchivesHistoryAndBumpsVersion() {
        TToponPlateCard c = persistedCard();
        TToponPlateCard out = service.renameRoad(100L, "明德大道", null);
        assertEquals("明德大道", out.getRoadName());
        assertEquals(2, out.getVersionNo());
        assertEquals("张三", out.getFounderName());
        verify(hisMapper).insert(any());
    }

    // ---------- 层级范围 ----------

    @Test
    void districtUserCannotTouchOtherAreaCard() {
        TToponPlateCard c = form();
        c.setId(100L);
        c.setSiteNo("361003000"); // 城西区
        when(cardMapper.selectOne(any())).thenReturn(c);
        when(scopeResolver.currentScope())
                .thenReturn(PlateScopeResolver.Scope.area("361002")); // 城东区经办

        PlateCardException ex = assertThrows(PlateCardException.class,
                () -> service.selectTToponPlateCardById(100L));
        assertEquals("siteNo", ex.getField());
    }

    @Test
    void userWithoutAnyScopeSeesNothing() {
        TToponPlateCard c = form();
        c.setId(100L);
        when(cardMapper.selectOne(any())).thenReturn(c);
        when(scopeResolver.currentScope())
                .thenReturn(PlateScopeResolver.Scope.none());

        assertEquals("siteNo", assertThrows(PlateCardException.class,
                () -> service.selectTToponPlateCardById(100L)).getField());
    }

    // ---------- 年底对账：两路口径须对得上 ----------

    @Test
    @SuppressWarnings("unchecked")
    void reconcileTwoWaysMatch() {
        TToponPlateCard a = form();
        TToponPlateCard b = form();
        when(cardMapper.selectList(any())).thenReturn(new ArrayList<>(Arrays.asList(a, b)));
        Map<String, Object> g1 = new HashMap<>();
        g1.put("siteNo", "361002000");
        g1.put("segCount", 2L);
        when(cardMapper.selectMaps(any())).thenReturn(Collections.singletonList(g1));

        Map<String, Object> out = service.reconcile();
        assertEquals(2, out.get("cityTotal"));
        assertEquals(2L, out.get("groupTotal"));
        assertTrue((Boolean) out.get("matched"));
    }
}
