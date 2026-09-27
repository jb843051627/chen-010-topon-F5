package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TToponDistrictDoc;

import java.util.List;

/**
 * 乡镇街道基础档案 Service接口
 * 立卡要挂档案，只认有档案的；档案撤并后新卡挂不进去，旧卡照旧查得到。
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface ITToponDistrictDocService {

    /** 按主键查档案（含已撤并：旧挂的卡还要照查得到） */
    TToponDistrictDoc selectById(Long id);

    /** 按区划代码查档案 */
    TToponDistrictDoc selectBySiteNo(String siteNo);

    /** 条件查询 */
    List<TToponDistrictDoc> selectList(Wrapper<TToponDistrictDoc> queryWrapper);

    /** 只列在用档案（立卡挂档案的下拉只认这些） */
    List<TToponDistrictDoc> listActive();

    /** 档案登记：区划代码唯一、三道上限依次抬高、区县代码与区划代码前6位咬得上 */
    TToponDistrictDoc register(TToponDistrictDoc doc);

    /** 档案撤并：status 置 1，往后新卡挂不进；旧记录不删不改 */
    int abolish(Long id, String remark);
}
