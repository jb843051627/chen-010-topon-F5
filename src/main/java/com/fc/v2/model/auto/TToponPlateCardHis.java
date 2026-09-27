package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 门楼牌号段卡历史版本对象 t_topon_plate_card_his
 * 改属/号数增减时旧的一笔归历史存着，现行页只留最新一份。
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_topon_plate_card_his")
@ApiModel(value = "TToponPlateCardHis", description = "门楼牌号段卡历史版本")
public class TToponPlateCardHis implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 变动类型 1街巷改属 */
    public static final int CHANGE_REARCHIVE = 1;
    /** 变动类型 2号数增减 */
    public static final int CHANGE_READJ_QTY = 2;
    /** 变动类型 3路名更动 */
    public static final int CHANGE_ROAD_NAME = 3;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 段卡id */
    @TableField("card_id")
    @ApiModelProperty(value = "段卡id")
    private Long cardId;

    /** 归档时的旧版本号 */
    @TableField("version_no")
    @ApiModelProperty(value = "归档时的旧版本号")
    private Integer versionNo;

    /** 变动类型 1街巷改属 2号数增减 3路名更动 */
    @TableField("change_type")
    @ApiModelProperty(value = "变动类型")
    private Integer changeType;

    /** 段卡号(快照) */
    @TableField("bill_no")
    @ApiModelProperty(value = "段卡号")
    private String billNo;

    /** 街巷名称(快照) */
    @TableField("road_name")
    @ApiModelProperty(value = "街巷名称")
    private String roadName;

    /** 旧档案id */
    @TableField("site_id")
    @ApiModelProperty(value = "旧档案id")
    private Long siteId;

    /** 旧区划代码 */
    @TableField("site_no")
    @ApiModelProperty(value = "旧区划代码")
    private String siteNo;

    /** 旧可编号数 */
    @TableField("qty")
    @ApiModelProperty(value = "旧可编号数")
    private BigDecimal qty;

    /** 旧本年度新编数 */
    @TableField("fine_amt")
    @ApiModelProperty(value = "旧本年度新编数")
    private BigDecimal fineAmt;

    /** 旧容量档 */
    @TableField("grade_level")
    @ApiModelProperty(value = "旧容量档")
    private Integer gradeLevel;

    /** 旧容量档折算值 */
    @TableField("grade_value")
    @ApiModelProperty(value = "旧容量档折算值")
    private BigDecimal gradeValue;

    /** 单号侧起号(快照) */
    @TableField("odd_start_no")
    @ApiModelProperty(value = "单号侧起号")
    private Integer oddStartNo;

    /** 单号侧止号(快照) */
    @TableField("odd_end_no")
    @ApiModelProperty(value = "单号侧止号")
    private Integer oddEndNo;

    /** 双号侧起号(快照) */
    @TableField("even_start_no")
    @ApiModelProperty(value = "双号侧起号")
    private Integer evenStartNo;

    /** 双号侧止号(快照) */
    @TableField("even_end_no")
    @ApiModelProperty(value = "双号侧止号")
    private Integer evenEndNo;

    /** 归档经手人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "归档经手人")
    private String createBy;

    /** 归档时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "归档时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getChangeType() {
        return changeType;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public BigDecimal getGradeValue() {
        return gradeValue;
    }

    public void setGradeValue(BigDecimal gradeValue) {
        this.gradeValue = gradeValue;
    }

    public Integer getOddStartNo() {
        return oddStartNo;
    }

    public void setOddStartNo(Integer oddStartNo) {
        this.oddStartNo = oddStartNo;
    }

    public Integer getOddEndNo() {
        return oddEndNo;
    }

    public void setOddEndNo(Integer oddEndNo) {
        this.oddEndNo = oddEndNo;
    }

    public Integer getEvenStartNo() {
        return evenStartNo;
    }

    public void setEvenStartNo(Integer evenStartNo) {
        this.evenStartNo = evenStartNo;
    }

    public Integer getEvenEndNo() {
        return evenEndNo;
    }

    public void setEvenEndNo(Integer evenEndNo) {
        this.evenEndNo = evenEndNo;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
