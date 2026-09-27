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
 * 门楼牌号段并卡台账对象 t_topon_plate_merge
 * 来源号撞上时并成一条，账上只许剩一笔。
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_topon_plate_merge")
@ApiModel(value = "TToponPlateMerge", description = "门楼牌号段并卡台账")
public class TToponPlateMerge implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 来源单号 */
    @TableField("source_no")
    @ApiModelProperty(value = "来源单号")
    private String sourceNo;

    /** 留存段卡id */
    @TableField("kept_card_id")
    @ApiModelProperty(value = "留存段卡id")
    private Long keptCardId;

    /** 并销段卡id(打删除标记,账上不再单列) */
    @TableField("dropped_card_id")
    @ApiModelProperty(value = "并销段卡id")
    private Long droppedCardId;

    /** 并卡经手人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "并卡经手人")
    private String createBy;

    /** 并卡时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "并卡时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceNo() {
        return sourceNo;
    }

    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
    }

    public Long getKeptCardId() {
        return keptCardId;
    }

    public void setKeptCardId(Long keptCardId) {
        this.keptCardId = keptCardId;
    }

    public Long getDroppedCardId() {
        return droppedCardId;
    }

    public void setDroppedCardId(Long droppedCardId) {
        this.droppedCardId = droppedCardId;
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
