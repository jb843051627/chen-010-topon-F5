package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.conf.V2Config;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TToponDistrictDoc;
import com.fc.v2.model.auto.TToponPlateCard;
import com.fc.v2.model.custom.PlateRegisterResult;
import com.fc.v2.service.ITToponDistrictDocService;
import com.fc.v2.service.ITToponPlateCardService;
import com.fc.v2.util.StringUtils;
import com.fc.v2.util.file.FileUploadUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 门楼牌号段建档卡 Controller。
 * 门脸只留「登记」一处写入口；登出来的成与不成页面不涂改，一切以接口回话为凭；
 * 校验的本事收在服务层，页面上没有。
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
    @Autowired
    private ITToponDistrictDocService toponDistrictDocService;
    @Autowired
    private V2Config v2Config;

    // ==================== 旧签名（不动窝） ====================

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("topon:toponPlateCard:view")
    public String view(ModelMap model) {
        model.put("docs", toponDistrictDocService.listActive());
        return prefix + "/list";
    }

    @Log(title = "门楼牌号段建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public ResultTable list(TToponPlateCard record) {
        QueryWrapper<TToponPlateCard> queryWrapper = new QueryWrapper<TToponPlateCard>();
        queryWrapper.eq(StringUtils.isNotEmpty(record.getBillNo()), "bill_no", record.getBillNo());
        queryWrapper.like(StringUtils.isNotEmpty(record.getRoadName()), "road_name", record.getRoadName());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getSiteNo()), "site_no", record.getSiteNo());
        queryWrapper.eq(record.getStatus() != null, "status", record.getStatus());
        startPage();
        com.github.pagehelper.PageInfo<TToponPlateCard> page =
                new com.github.pagehelper.PageInfo<TToponPlateCard>(toponPlateCardService.selectTToponPlateCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    /** 旧新增端点保留：内部即登记，成与不成凭服务层回话 */
    @Log(title = "门楼牌号段建档卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("topon:toponPlateCard:add")
    @ResponseBody
    public AjaxResult add(TToponPlateCard record) {
        PlateRegisterResult r = toponPlateCardService.register(record);
        return success(200, r.getMessage(), r.getCard());
    }

    /** 旧修改端点保留：只许补备注/影像，卡号/创建人/容量档/归属一概不经此道 */
    @Log(title = "门楼牌号段建档卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult editSave(TToponPlateCard record) {
        return toAjax(toponPlateCardService.updateTToponPlateCard(record));
    }

    @Log(title = "门楼牌号段建档卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("topon:toponPlateCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(toponPlateCardService.deleteTToponPlateCardByIds(ids));
    }

    // ==================== 添的新活儿 ====================

    @ApiOperation(value = "登记页跳转", notes = "门脸唯一写入口")
    @GetMapping("/register")
    @RequiresPermissions("topon:toponPlateCard:view")
    public String registerPage(ModelMap model) {
        model.put("docs", toponDistrictDocService.listActive());
        return prefix + "/register";
    }

    /**
     * 登记立卡（唯一写入口）：表单字段 + 街巷全貌、号牌实样两张影像。
     * 段卡号这类单号系统不代填。
     */
    @Log(title = "门楼牌号段卡登记", action = "register")
    @ApiOperation(value = "登记立卡", notes = "登记立卡")
    @PostMapping("/registerSave")
    @RequiresPermissions("topon:toponPlateCard:add")
    @ResponseBody
    public AjaxResult registerSave(TToponPlateCard record,
                                   @RequestParam(value = "streetViewFile", required = false) MultipartFile streetViewFile,
                                   @RequestParam(value = "plateSampleFile", required = false) MultipartFile plateSampleFile) {
        record.setStreetViewImg(storeImage(streetViewFile, record.getStreetViewImg()));
        record.setPlateSampleImg(storeImage(plateSampleFile, record.getPlateSampleImg()));
        PlateRegisterResult r = toponPlateCardService.register(record);
        Map<String, Object> data = new HashMap<>();
        data.put("card", r.getCard());
        data.put("merged", r.isMerged());
        return success(200, r.getMessage(), data);
    }

    @ApiOperation(value = "详情页跳转", notes = "现行页只留最新一份，历史另列")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("topon:toponPlateCard:view")
    public String detail(@PathVariable("id") Long id, ModelMap model) {
        model.put("card", toponPlateCardService.selectTToponPlateCardById(id));
        return prefix + "/detail";
    }

    @ApiOperation(value = "段卡现行数据", notes = "段卡现行数据")
    @GetMapping("/detailData/{id}")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public AjaxResult detailData(@PathVariable("id") Long id) {
        return retobject(200, toponPlateCardService.selectTToponPlateCardById(id));
    }

    @ApiOperation(value = "经手人续记", notes = "一办一换手续记一条，与创建人各记各的")
    @GetMapping("/handlers/{id}")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public AjaxResult handlers(@PathVariable("id") Long id) {
        return retobject(200, toponPlateCardService.handlers(id));
    }

    @ApiOperation(value = "历史版本", notes = "旧的一笔归历史")
    @GetMapping("/history/{id}")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public AjaxResult history(@PathVariable("id") Long id) {
        return retobject(200, toponPlateCardService.history(id));
    }

    @ApiOperation(value = "已发号码台账", notes = "已发号码台账")
    @GetMapping("/numbers/{id}")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public AjaxResult numbers(@PathVariable("id") Long id) {
        return retobject(200, toponPlateCardService.numbers(id));
    }

    @Log(title = "门楼牌号段卡核对", action = "check")
    @ApiOperation(value = "核对", notes = "待核对→已核对")
    @PostMapping("/check")
    @RequiresPermissions("topon:toponPlateCard:check")
    @ResponseBody
    public AjaxResult check(Long id, String remark) {
        return success(200, "已核对", toponPlateCardService.check(id, remark));
    }

    @Log(title = "门楼牌号段卡定档", action = "finalize")
    @ApiOperation(value = "定档", notes = "已核对→已定档")
    @PostMapping("/finalize")
    @RequiresPermissions("topon:toponPlateCard:finalize")
    @ResponseBody
    public AjaxResult finalize(Long id, String remark) {
        return success(200, "已定档", toponPlateCardService.finalize(id, remark));
    }

    @Log(title = "门楼牌号段卡改属", action = "rearchive")
    @ApiOperation(value = "街巷改属", notes = "旧一笔归历史，容量档照新档案重折")
    @PostMapping("/rearchive")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult rearchive(Long id, Long newSiteId, String remark) {
        TToponDistrictDoc doc = toponDistrictDocService.selectById(newSiteId);
        toponPlateCardService.rearchive(id, newSiteId, remark);
        return success(200, doc != null ? "已改属「" + doc.getSiteName() + "」，旧一笔归历史" : "改属完成", null);
    }

    @Log(title = "门楼牌号段卡号数增减", action = "adjustQty")
    @ApiOperation(value = "号数增减", notes = "旧一笔归历史，容量档重折；起讫号随号数一并改时须咬得上")
    @PostMapping("/adjustQty")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult adjustQty(Long id, BigDecimal qty, BigDecimal fineAmt,
                                Integer oddStartNo, Integer oddEndNo, Integer evenStartNo, Integer evenEndNo,
                                String remark) {
        return success(200, "号数已增减，容量档已由系统重折",
                toponPlateCardService.adjustQty(id, qty, fineAmt,
                        oddStartNo, oddEndNo, evenStartNo, evenEndNo, remark));
    }

    @Log(title = "门楼牌号段卡路名更动", action = "renameRoad")
    @ApiOperation(value = "路名更动", notes = "坐落路名更动，旧一笔归历史")
    @PostMapping("/renameRoad")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult renameRoad(Long id, String roadName, String remark) {
        return success(200, "路名已更动，旧一笔归历史",
                toponPlateCardService.renameRoad(id, roadName, remark));
    }

    @Log(title = "门楼牌号段卡发号", action = "issue")
    @ApiOperation(value = "新编/续编发号", notes = "续过的号不许再发第二遍")
    @PostMapping("/issue")
    @RequiresPermissions("topon:toponPlateCard:issue")
    @ResponseBody
    public AjaxResult issue(Long id, Integer side, Integer plateNo,
                            @RequestParam(value = "renew", defaultValue = "false") boolean renew, String remark) {
        return success(200, renew ? "老号已续编，这是最后一遍" : "号码已发",
                toponPlateCardService.issue(id, side == null ? 0 : side, plateNo == null ? 0 : plateNo, renew, remark));
    }

    @Log(title = "载入老号台账", action = "loadLegacy")
    @ApiOperation(value = "载入老号", notes = "三十年前的老号也入册：0原号在册 1空号可续")
    @PostMapping("/loadLegacy")
    @RequiresPermissions("topon:toponPlateCard:issue")
    @ResponseBody
    public AjaxResult loadLegacy(Long id, Integer side, Integer plateNo, Integer status, Integer issueYear) {
        return success(200, "老号已载入号册",
                toponPlateCardService.loadLegacy(id, side, plateNo, status, issueYear));
    }

    @Log(title = "门楼牌号段卡并卡", action = "merge")
    @ApiOperation(value = "并卡", notes = "同来源号并成一条，账上只许剩一笔")
    @PostMapping("/merge")
    @RequiresPermissions("topon:toponPlateCard:edit")
    @ResponseBody
    public AjaxResult merge(Long keptId, Long droppedId, String remark) {
        return success(200, "已并成一条，账上只此一笔",
                toponPlateCardService.merge(keptId, droppedId, remark));
    }

    @ApiOperation(value = "年底对账", notes = "市局新编多少口径与区县归堆口径的段数须对得上")
    @GetMapping("/reconcile")
    @RequiresPermissions("topon:toponPlateCard:list")
    @ResponseBody
    public AjaxResult reconcile() {
        return retobject(200, toponPlateCardService.reconcile());
    }

    /** 两张影像随卡存：新传的落盘取路径，没传的沿用旧路径 */
    private String storeImage(MultipartFile file, String existedPath) {
        if (file == null || file.isEmpty()) {
            return existedPath;
        }
        try {
            return FileUploadUtils.upload(v2Config.getProfile(), v2Config.getUploadPath(), file,
                    com.fc.v2.util.file.MimeTypeUtils.IMAGE_EXTENSION);
        } catch (Exception e) {
            throw new com.fc.v2.common.exception.PlateCardException("image", "影像没存住：" + e.getMessage());
        }
    }
}
