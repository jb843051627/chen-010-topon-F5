package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.service.ITToponDistrictDocService;
import com.fc.v2.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 乡镇街道基础档案 Controller。
 * 立卡要挂档案，只认有档案的；档案撤并后新卡挂不进去，旧卡照旧查得到、也不许借着重改。
 *
 * @author fuce
 * @date 2026-09-27
 */
@Api(value = "乡镇街道基础档案")
@Controller
@RequestMapping("/ToponDistrictDocController")
public class ToponDistrictDocController extends BaseController {

    private final String prefix = "admin/toponDistrictDoc";

    @Autowired
    private ITToponDistrictDocService toponDistrictDocService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("topon:toponDistrictDoc:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "乡镇街道基础档案集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("topon:toponDistrictDoc:list")
    @ResponseBody
    public ResultTable list(TToponDistrictDoc record) {
        QueryWrapper<TToponDistrictDoc> w = new QueryWrapper<TToponDistrictDoc>();
        w.eq("del_flag", 0);
        w.like(StringUtils.isNotEmpty(record.getSiteName()), "site_name", record.getSiteName());
        w.eq(StringUtils.isNotEmpty(record.getSiteNo()), "site_no", record.getSiteNo());
        w.eq(record.getStatus() != null, "status", record.getStatus());
        w.orderByAsc("area_code", "site_no");
        startPage();
        com.github.pagehelper.PageInfo<TToponDistrictDoc> page =
                new com.github.pagehelper.PageInfo<TToponDistrictDoc>(toponDistrictDocService.selectList(w));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "档案新增页跳转", notes = "档案新增页跳转")
    @GetMapping("/add")
    @RequiresPermissions("topon:toponDistrictDoc:view")
    public String addPage() {
        return prefix + "/add";
    }

    @Log(title = "乡镇街道基础档案登记", action = "add")
    @ApiOperation(value = "档案登记", notes = "一个乡镇街道只留一份")
    @PostMapping("/addSave")
    @RequiresPermissions("topon:toponDistrictDoc:add")
    @ResponseBody
    public AjaxResult addSave(TToponDistrictDoc doc) {
        return success(200, "档案已立，乡镇街道只此一份", toponDistrictDocService.register(doc));
    }

    @Log(title = "乡镇街道基础档案撤并", action = "abolish")
    @ApiOperation(value = "撤并", notes = "撤并后新卡挂不进，旧卡照旧查得到")
    @PostMapping("/abolish")
    @RequiresPermissions("topon:toponDistrictDoc:abolish")
    @ResponseBody
    public AjaxResult abolish(Long id, String remark) {
        return toAjax(toponDistrictDocService.abolish(id, remark));
    }
}
