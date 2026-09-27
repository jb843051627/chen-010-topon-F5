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
 * 门楼牌号段建档卡对象 t_topon_plate_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_topon_plate_card")
@ApiModel(value = "TToponPlateCard", description = "门楼牌号段建档卡")
public class TToponPlateCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 门楼牌号段卡号(卡的牙口,同段第二回立卡须挡住) */
    @TableField("bill_no")
    @ApiModelProperty(value = "门楼牌号段卡号")
    private String billNo;

    /** 来源单号(区里自报/市局抄送凭此号并成一条) */
    @TableField("source_no")
    @ApiModelProperty(value = "来源单号")
    private String sourceNo;

    /** 街巷名称 */
    @TableField("road_name")
    @ApiModelProperty(value = "街巷名称")
    private String roadName;

    /** 所属乡镇街道基础档案id */
    @TableField("site_id")
    @ApiModelProperty(value = "所属乡镇街道基础档案")
    private Long siteId;

    /** 乡镇街道区划代码 */
    @TableField("site_no")
    @ApiModelProperty(value = "乡镇街道区划代码")
    private String siteNo;

    /** 单双侧合起来可编号码总个数(个) */
    @TableField("qty")
    @ApiModelProperty(value = "单双侧可编号码总个数(个)")
    private BigDecimal qty;

    /** 本年度新编号码个数(个) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "本年度新编号码个数(个)")
    private BigDecimal fineAmt;

    /** 系统折算号段容量档 1一档 2二档 3三档(只由系统填) */
    @TableField("grade_level")
    @ApiModelProperty(value = "号段容量档")
    private Integer gradeLevel;

    /** 号段容量档折算值,两位小数(只由系统填) */
    @TableField("grade_value")
    @ApiModelProperty(value = "号段容量档折算值")
    private BigDecimal gradeValue;

    /** 单号侧起号 */
    @TableField("odd_start_no")
    @ApiModelProperty(value = "单号侧起号")
    private Integer oddStartNo;

    /** 单号侧止号 */
    @TableField("odd_end_no")
    @ApiModelProperty(value = "单号侧止号")
    private Integer oddEndNo;

    /** 双号侧起号 */
    @TableField("even_start_no")
    @ApiModelProperty(value = "双号侧起号")
    private Integer evenStartNo;

    /** 双号侧止号 */
    @TableField("even_end_no")
    @ApiModelProperty(value = "双号侧止号")
    private Integer evenEndNo;

    /** 街巷全貌影像路径 */
    @TableField("street_view_img")
    @ApiModelProperty(value = "街巷全貌影像路径")
    private String streetViewImg;

    /** 号牌实样影像路径 */
    @TableField("plate_sample_img")
    @ApiModelProperty(value = "号牌实样影像路径")
    private String plateSampleImg;

    /** 现行版本号(改属/号数增减自增) */
    @TableField("version_no")
    @ApiModelProperty(value = "现行版本号")
    private Integer versionNo;

    /** 进展 0待核对 1已核对 2已定档 */
    @TableField("status")
    @ApiModelProperty(value = "进展 0待核对 1已核对 2已定档")
    private Integer status;

    /** 删除标记 0正常 1删除/并卡归档 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建人(按下立卡那一下的人,重录也不许挪) */
    @TableField("founder_name")
    @ApiModelProperty(value = "创建人")
    private String founderName;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getSourceNo() {
        return sourceNo;
    }

    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
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

    public String getStreetViewImg() {
        return streetViewImg;
    }

    public void setStreetViewImg(String streetViewImg) {
        this.streetViewImg = streetViewImg;
    }

    public String getPlateSampleImg() {
        return plateSampleImg;
    }

    public void setPlateSampleImg(String plateSampleImg) {
        this.plateSampleImg = plateSampleImg;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getFounderName() {
        return founderName;
    }

    public void setFounderName(String founderName) {
        this.founderName = founderName;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
