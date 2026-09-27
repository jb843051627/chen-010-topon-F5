package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TToponPlateCard;

import java.io.Serializable;

/**
 * 段卡登记回话：成与不成、是否撞上来源号并成一条，都由服务层裁定。
 *
 * @author fuce
 * @date 2026-09-27
 */
public class PlateRegisterResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 账上这一笔段卡（新立或并卡后留存的那笔） */
    private TToponPlateCard card;

    /** true=区里自报与市局抄送撞来源号，已并入既有那笔，账上没有另起一笔 */
    private boolean merged;

    /** 接口回话原话，页面原样摆出，不得涂改 */
    private String message;

    public PlateRegisterResult() {
    }

    public PlateRegisterResult(TToponPlateCard card, boolean merged, String message) {
        this.card = card;
        this.merged = merged;
        this.message = message;
    }

    public TToponPlateCard getCard() {
        return card;
    }

    public void setCard(TToponPlateCard card) {
        this.card = card;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
