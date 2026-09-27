package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.model.auto.TToponPlateCard;

import java.util.List;

/**
 * 门楼牌号段建档卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITToponPlateCardService {

    /** 按主键查询 */
    TToponPlateCard selectTToponPlateCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TToponPlateCard> selectTToponPlateCardList(Wrapper<TToponPlateCard> queryWrapper);

    /** 新增 */
    int insertTToponPlateCard(TToponPlateCard record);

    /** 修改 */
    int updateTToponPlateCard(TToponPlateCard record);

    /** 批量删除 */
    int deleteTToponPlateCardByIds(String ids);

    /** 按主键删除 */
    int deleteTToponPlateCardById(Long id);

    /**
     * 立卡登记（重载）：校验收进服务层，成与不成以接口回话为凭。
     * 段卡号系统不代填；同一号段第二回立卡当场挡回；只认有档案且在用的；
     * 可编号数为负、为零、超出档案能承接上限的逐格说清；容量档由系统折算。
     *
     * @param record   段卡
     * @param operName 按下立卡那一下的人（创建人，记上就不许挪）
     * @return 接口回话
     */
    AjaxResult insertTToponPlateCard(TToponPlateCard record, String operName);

    /**
     * 重录/改属（重载）：创建人不挪、经手人续记一条；档案已撤并的旧卡照旧可查、
     * 不许借着重改；街巷改属或号数有增减时容量档跟着重折，不由人动手填。
     *
     * @param record   段卡
     * @param operName 这趟经手的人
     * @return 接口回话
     */
    AjaxResult updateTToponPlateCard(TToponPlateCard record, String operName);

    /** 在用（未撤并）的乡镇街道基础档案，供立卡挂接选择 */
    List<TToponDistrictDoc> selectUsableDistrictDocList();
}
