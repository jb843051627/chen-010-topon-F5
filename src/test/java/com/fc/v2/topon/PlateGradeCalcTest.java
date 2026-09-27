package com.fc.v2.topon;

import com.fc.v2.common.exception.PlateCardException;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.service.support.PlateGradeCalc;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 号段容量档折算：系统拿号数对档案三道上限折算，留两位小数。
 */
class PlateGradeCalcTest {

    private TToponDistrictDoc doc() {
        TToponDistrictDoc d = new TToponDistrictDoc();
        d.setTh1Max(new BigDecimal("40"));
        d.setTh2Max(new BigDecimal("200"));
        d.setTh3Max(new BigDecimal("600"));
        return d;
    }

    @Test
    void grade1ByFirstCeiling() {
        PlateGradeCalc.Grade g = PlateGradeCalc.convert(new BigDecimal("30"), doc());
        assertEquals(1, g.getLevel());
        assertEquals(new BigDecimal("0.75"), g.getValue());
    }

    @Test
    void grade2BySecondCeiling() {
        PlateGradeCalc.Grade g = PlateGradeCalc.convert(new BigDecimal("50"), doc());
        assertEquals(2, g.getLevel());
        assertEquals(new BigDecimal("0.25"), g.getValue());
    }

    @Test
    void grade3ByThirdCeilingRoundedTwoScale() {
        PlateGradeCalc.Grade g = PlateGradeCalc.convert(new BigDecimal("201"), doc());
        assertEquals(3, g.getLevel());
        // 201/600=0.335，四舍五入留两位
        assertEquals(new BigDecimal("0.34"), g.getValue());
    }

    @Test
    void ceilingBoundariesBelongToLowerGrade() {
        assertEquals(1, PlateGradeCalc.convert(new BigDecimal("40"), doc()).getLevel());
        assertEquals(2, PlateGradeCalc.convert(new BigDecimal("200"), doc()).getLevel());
        assertEquals(3, PlateGradeCalc.convert(new BigDecimal("600"), doc()).getLevel());
    }

    @Test
    void overThirdCeilingRejectedWithField() {
        assertFalse(PlateGradeCalc.withinCapacity(new BigDecimal("601"), doc()));
        PlateCardException ex = assertThrows(PlateCardException.class,
                () -> PlateGradeCalc.convert(new BigDecimal("601"), doc()));
        assertEquals("qty", ex.getField());
    }

    @Test
    void nullCeilingsRejected() {
        TToponDistrictDoc d = doc();
        d.setTh3Max(null);
        assertFalse(PlateGradeCalc.withinCapacity(new BigDecimal("10"), d));
        PlateCardException ex = assertThrows(PlateCardException.class,
                () -> PlateGradeCalc.convert(new BigDecimal("10"), d));
        assertEquals("siteId", ex.getField());
    }
}
