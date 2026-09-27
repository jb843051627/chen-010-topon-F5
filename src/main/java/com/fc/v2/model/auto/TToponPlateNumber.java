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
import java.util.Date;

/**
 * 门楼牌号段已发号码台账对象 t_topon_plate_number
 * 老号在册/空号/续号都留一笔；续过的号不许再发第二遍。
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_topon_plate_number")
@ApiModel(value = "TToponPlateNumber", description = "门楼牌号段已发号码台账")
public class TToponPlateNumber implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 号侧 1单号侧 2双号侧 */
    public static final int SIDE_ODD = 1;
    /** 号侧 2双号侧 */
    public static final int SIDE_EVEN = 2;

    /** 来号方式 0新编 */
    public static final int SOURCE_NEW = 0;
    /** 来号方式 1续编空出的老号 */
    public static final int SOURCE_RENEW = 1;

    /** 号情形 0原号在册 */
    public static final int STATE_ORIGINAL = 0;
    /** 号情形 1空号可续 */
    public static final int STATE_VACANT = 1;
    /** 号情形 2续号在册(终态) */
    public static final int STATE_RENEWED = 2;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 段卡id */
    @TableField("card_id")
    @ApiModelProperty(value = "段卡id")
    private Long cardId;

    /** 号侧 1单号侧 2双号侧 */
    @TableField("side")
    @ApiModelProperty(value = "号侧 1单号侧 2双号侧")
    private Integer side;

    /** 门牌号 */
    @TableField("plate_no")
    @ApiModelProperty(value = "门牌号")
    private Integer plateNo;

    /** 来号方式 0新编 1续编空出的老号 */
    @TableField("source_type")
    @ApiModelProperty(value = "来号方式 0新编 1续编空出的老号")
    private Integer sourceType;

    /** 号情形 0原号在册 1空号可续 2续号在册(终态) */
    @TableField("status")
    @ApiModelProperty(value = "号情形 0原号在册 1空号可续 2续号在册")
    private Integer status;

    /** 发放年度 */
    @TableField("issue_year")
    @ApiModelProperty(value = "发放年度")
    private Integer issueYear;

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

    public Integer getSide() {
        return side;
    }

    public void setSide(Integer side) {
        this.side = side;
    }

    public Integer getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(Integer plateNo) {
        this.plateNo = plateNo;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getStatus() {
        return status;

    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIssueYear() {
        return issueYear;
    }

    public void setIssueYear(Integer issueYear) {
        this.issueYear = issueYear;
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
}
