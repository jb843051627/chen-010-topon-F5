package com.fc.v2.service.support;

import com.fc.v2.common.exception.PlateCardException;
import com.fc.v2.model.auto.TToponDistrictDoc;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 号段容量档折算（只由系统算，不兴由人自己动手填）。
 * 拿可编号数去对档案那三道上限：过一道进一档；折算值 = 号数 / 本档上限，留两位小数。
 * 街巷改属、号数有增减，都拿这个重折一遍。
 *
 * @author fuce
 * @date 2026-09-27
 */
public final class PlateGradeCalc {

    /** 一档 */
    public static final int GRADE_1 = 1;
    /** 二档 */
    public static final int GRADE_2 = 2;
    /** 三档 */
    public static final int GRADE_3 = 3;

    private PlateGradeCalc() {
    }

    /**
     * 折算结果：档别 + 两位小数折算值。
     */
    public static final class Grade {
        private final int level;
        private final BigDecimal value;

        Grade(int level, BigDecimal value) {
            this.level = level;
            this.value = value;
        }

        public int getLevel() {
            return level;
        }

        public BigDecimal getValue() {
            return value;
        }
    }

    /**
     * 号数是否还在档案承接范围之内（高过三档上限一大截，卡落不下去）。
     */
    public static boolean withinCapacity(BigDecimal qty, TToponDistrictDoc doc) {
        return qty != null && doc != null && doc.getTh3Max() != null
                && qty.compareTo(doc.getTh3Max()) <= 0;
    }

    /**
     * 拿号数对三道上限折算。调用前须先经 withinCapacity 与正数校验。
     */
    public static Grade convert(BigDecimal qty, TToponDistrictDoc doc) {
        if (qty == null) {
            throw new PlateCardException("qty", "可编号数没填，没法折算容量档");
        }
        if (doc == null || doc.getTh1Max() == null || doc.getTh2Max() == null || doc.getTh3Max() == null) {
            throw new PlateCardException("siteId", "档案容量三道上限不齐，折算不了容量档");
        }
        BigDecimal th1 = doc.getTh1Max();
        BigDecimal th2 = doc.getTh2Max();
        BigDecimal th3 = doc.getTh3Max();
        int level;
        BigDecimal ceiling;
        if (qty.compareTo(th1) <= 0) {
            level = GRADE_1;
            ceiling = th1;
        } else if (qty.compareTo(th2) <= 0) {
            level = GRADE_2;
            ceiling = th2;
        } else if (qty.compareTo(th3) <= 0) {
            level = GRADE_3;
            ceiling = th3;
        } else {
            throw new PlateCardException("qty",
                    "可编号数 " + qty.toPlainString() + " 高过档案三档上限 " + th3.toPlainString() + "，卡落不下去");
        }
        BigDecimal value = qty.divide(ceiling, 2, RoundingMode.HALF_UP);
        return new Grade(level, value);
    }
}
