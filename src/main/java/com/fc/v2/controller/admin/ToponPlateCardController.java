package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.service.ITToponPlateCardService;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 门楼牌号段建档卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "门楼牌号段建档卡")
@Controller
@RequestMapping("/ToponPlateCardController")
public class ToponPlateCardController extends BaseController {

    private final String prefix = "admin/toponPlateCard";

    @Autowired
    private ITToponPlateCardService toponPlateCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("topon:toponPlateCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @ApiOperation(value = "登记跳转", notes = "登记跳转")
    @GetMapping("/add")
    @RequiresPermissions("topon:toponPlateCard:add")
    public String add(ModelMap model) {
        return prefix + "/add";
    }

    @Log(title = "门楼牌号段建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public ResultTable list(TToponPlateCard record) {
        QueryWrapper<TToponPlateCard> queryWrapper = new QueryWrapper<TToponPlateCard>();
        queryWrapper.eq("del_flag", 0);
        if (record != null) {
            if (StringUtils.isNotEmpty(record.getBillNo())) {
                queryWrapper.like("bill_no", record.getBillNo());
            }
            if (StringUtils.isNotEmpty(record.getSiteNo())) {
                // 区县清单只带本区那一级地段，同一条段卡在两边点开是同一笔账
                queryWrapper.eq("site_no", record.getSiteNo());
            }
            if (record.getSiteId() != null) {
                queryWrapper.eq("site_id", record.getSiteId());
            }
            if (record.getStatus() != null) {
                queryWrapper.eq("status", record.getStatus());
            }
        }
        // 区里按最末一级地段归堆、市局从新编最多号数往下看：
        // 同一笔账两种数法，段数要能对上
        queryWrapper.orderByAsc("site_no").orderByDesc("fine_amt").orderByAsc("id");
        startPage();
        com.github.pagehelper.PageInfo<TToponPlateCard> page =
                new com.github.pagehelper.PageInfo<TToponPlateCard>(toponPlateCardService.selectTToponPlateCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "在用档案选项", notes = "立卡挂接用的在用乡镇街道基础档案")
    @GetMapping("/docOptions")
    @RequiresPermissions("topon:toponPlateCard:add")
    @ResponseBody
    public AjaxResult docOptions() {
        return retobject(200, toponPlateCardService.selectUsableDistrictDocList());
    }

    @Log(title = "门楼牌号段建档卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("topon:toponPlateCard:add")
    @ResponseBody
    public AjaxResult add(TToponPlateCard record) {
        // 成与不成以接口回话为凭；创建人记在按下立卡那一下的人身上
        return toponPlateCardService.insertTToponPlateCard(record, ShiroUtils.getLoginName());
    }

    @Log(title = "门楼牌号段建档卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult editSave(TToponPlateCard record) {
        // 经手人一办一换手就续记一条，与创建人各记各的
        return toponPlateCardService.updateTToponPlateCard(record, ShiroUtils.getLoginName());
    }

    @Log(title = "门楼牌号段建档卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("topon:toponPlateCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(toponPlateCardService.deleteTToponPlateCardByIds(ids));
    }
}
