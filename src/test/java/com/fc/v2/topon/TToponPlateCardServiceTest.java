package com.fc.v2.topon;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.mapper.auto.TToponDistrictDocMapper;
import com.fc.v2.mapper.auto.TToponPlateCardMapper;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.service.impl.TToponPlateCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 门楼牌号段建档卡：服务层校验规矩（不连库，mapper 全部打桩）
 */
class TToponPlateCardServiceTest {

    private TToponPlateCardServiceImpl service;
    private TToponPlateCardMapper cardMapper;
    private TToponDistrictDocMapper docMapper;

    /** 在用档案：三档上限 40/200/600 */
    private TToponDistrictDoc activeDoc;
    /** 已撤并档案 */
    private TToponDistrictDoc mergedDoc;

    @BeforeEach
    void setUp() {
        service = new TToponPlateCardServiceImpl();
        cardMapper = Mockito.mock(TToponPlateCardMapper.class);
        docMapper = Mockito.mock(TToponDistrictDocMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", cardMapper);
        ReflectionTestUtils.setField(service, "toponDistrictDocMapper", docMapper);

        activeDoc = doc(0L, "XZ00", 0);
        mergedDoc = doc(1L, "XZ01", 1);
    }

    private TToponDistrictDoc doc(Long id, String siteNo, int status) {
        TToponDistrictDoc d = new TToponDistrictDoc();
        d.setId(id);
        d.setSiteNo(siteNo);
        d.setSiteName("档案" + siteNo);
        d.setTh1Max(new BigDecimal("40"));
        d.setTh2Max(new BigDecimal("200"));
        d.setTh3Max(new BigDecimal("600"));
        d.setStatus(status);
        d.setDelFlag(0);
        return d;
    }

    private TToponPlateCard card(String billNo, Integer siteId, String qty) {
        TToponPlateCard c = new TToponPlateCard();
        c.setBillNo(billNo);
        c.setSiteId(siteId);
        c.setQty(qty == null ? null : new BigDecimal(qty));
        return c;
    }

    private void stubDoc(TToponDistrictDoc d) {
        when(docMapper.selectOne(any())).thenReturn(d);
    }

    @Test
    void 段卡号为空_系统不代填_当场挡回() {
        AjaxResult r = service.insertTToponPlateCard(card(null, 0, "10"), "张三");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("段卡号"));
        verify(cardMapper, never()).insert(any());
    }

    @Test
    void 立卡只认有档案的() {
        stubDoc(null);
        AjaxResult r = service.insertTToponPlateCard(card("DK001", 9, "10"), "张三");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("档案"));
        verify(cardMapper, never()).insert(any());
    }

    @Test
    void 档案已撤并_新卡挂不进去() {
        stubDoc(mergedDoc);
        AjaxResult r = service.insertTToponPlateCard(card("DK001", 1, "10"), "张三");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("撤并"));
        verify(cardMapper, never()).insert(any());
    }

    @Test
    void 同一号段第二回立卡要挡得住() {
        stubDoc(activeDoc);
        when(cardMapper.selectCount(any())).thenReturn(1);
        AjaxResult r = service.insertTToponPlateCard(card("DK001", 0, "10"), "李四");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("段卡号"));
        verify(cardMapper, never()).insert(any());
    }

    @Test
    void 可编号数负的零的超上限的_逐格说清() {
        stubDoc(activeDoc);
        when(cardMapper.selectCount(any())).thenReturn(0);

        AjaxResult neg = service.insertTToponPlateCard(card("DK001", 0, "-1"), "张三");
        assertEquals(500, neg.get("code"));
        assertTrue(String.valueOf(neg.get("msg")).contains("可编号数"));

        AjaxResult zero = service.insertTToponPlateCard(card("DK001", 0, "0"), "张三");
        assertEquals(500, zero.get("code"));
        assertTrue(String.valueOf(zero.get("msg")).contains("可编号数"));

        AjaxResult over = service.insertTToponPlateCard(card("DK001", 0, "601"), "张三");
        assertEquals(500, over.get("code"));
        assertTrue(String.valueOf(over.get("msg")).contains("可编号数"));
        assertTrue(String.valueOf(over.get("msg")).contains("600"));

        verify(cardMapper, never()).insert(any());
    }

    @Test
    void 容量档由系统折算_留两位小数_创建人记立卡人() {
        stubDoc(activeDoc);
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(cardMapper.insert(any())).thenReturn(1);

        // 40 → 一档；41 → 二档；200 → 二档；201 → 三档；600 → 三档
        assertGrade("40", 1);
        assertGrade("41", 2);
        assertGrade("200", 2);
        assertGrade("201", 3);
        assertGrade("600", 3);
    }

    private void assertGrade(String qty, int expectGrade) {
        reset(cardMapper);
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(cardMapper.insert(any())).thenReturn(1);
        stubDoc(activeDoc);

        TToponPlateCard c = card("DK" + qty, 0, qty);
        AjaxResult r = service.insertTToponPlateCard(c, "立卡人甲");
        assertEquals(200, r.get("code"), "qty=" + qty + " 应立卡成功");

        ArgumentCaptor<TToponPlateCard> captor = ArgumentCaptor.forClass(TToponPlateCard.class);
        verify(cardMapper).insert(captor.capture());
        TToponPlateCard saved = captor.getValue();
        assertEquals(expectGrade, saved.getGradeLevel(), "qty=" + qty + " 的容量档");
        assertEquals(new BigDecimal(qty).setScale(2), saved.getQty(), "号数留两位小数");
        assertEquals("XZ00", saved.getSiteNo(), "区划归属跟着档案走");
        assertEquals("立卡人甲", saved.getCreateBy(), "创建人记在立卡人身上");
        assertEquals(0, saved.getDelFlag());
        assertEquals(0, saved.getStatus());
    }

    @Test
    void 撤并档案下的旧卡_不许借着重改() {
        TToponPlateCard db = card("DK001", 1, "10");
        db.setId(100L);
        when(cardMapper.selectOne(any())).thenReturn(db);
        stubDoc(mergedDoc);

        TToponPlateCard edit = card("DK001", 1, "20");
        edit.setId(100L);
        AjaxResult r = service.updateTToponPlateCard(edit, "经手乙");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("撤并"));
        verify(cardMapper, never()).update(any(), any());
    }

    @Test
    void 重录_创建人不挪_经手人续记_容量档重折() {
        TToponPlateCard db = card("DK001", 0, "10");
        db.setId(100L);
        db.setCreateBy("立卡人甲");
        when(cardMapper.selectOne(any())).thenReturn(db);
        stubDoc(activeDoc);
        when(cardMapper.update(any(), any())).thenReturn(1);

        TToponPlateCard edit = card(null, null, "300"); // 整张卡重录：号数 10 → 300
        edit.setId(100L);
        edit.setCreateBy("想顶替的人"); // 不许挪
        AjaxResult r = service.updateTToponPlateCard(edit, "经手乙");
        assertEquals(200, r.get("code"));

        ArgumentCaptor<TToponPlateCard> captor = ArgumentCaptor.forClass(TToponPlateCard.class);
        verify(cardMapper).update(captor.capture(), any());
        TToponPlateCard saved = captor.getValue();
        assertNull(saved.getCreateBy(), "创建人那一笔不许动");
        assertNull(saved.getCreateTime(), "创建时间不许动");
        assertEquals("经手乙", saved.getUpdateBy(), "经手人续记一条");
        assertEquals(3, saved.getGradeLevel(), "号数增减后容量档跟着重折");
        assertNull(saved.getBillNo(), "段卡号不许抹掉");
    }

    @Test
    void 改属_只认有档案且在用的_区划归属跟着走() {
        TToponPlateCard db = card("DK001", 0, "10");
        db.setId(100L);
        when(cardMapper.selectOne(any())).thenReturn(db);
        // 第一次取现挂档案（在用），第二次取改属目标（已撤并）
        when(docMapper.selectOne(any())).thenReturn(activeDoc).thenReturn(mergedDoc);

        TToponPlateCard edit = card(null, 1, null);
        edit.setId(100L);
        AjaxResult r = service.updateTToponPlateCard(edit, "经手乙");
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("撤并"));
        verify(cardMapper, never()).update(any(), any());
    }

    @Test
    void 旧签名照旧能用_走重载那一套() {
        stubDoc(activeDoc);
        when(cardMapper.selectCount(any())).thenReturn(0);
        when(cardMapper.insert(any())).thenReturn(1);
        assertEquals(1, service.insertTToponPlateCard(card("DK001", 0, "10")));

        stubDoc(null);
        assertEquals(0, service.insertTToponPlateCard(card("DK002", 9, "10")));
    }
}
