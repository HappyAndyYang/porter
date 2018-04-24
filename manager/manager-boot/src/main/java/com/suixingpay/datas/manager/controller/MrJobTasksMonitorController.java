package com.suixingpay.datas.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suixingpay.datas.manager.core.icon.MrJobMonitor;
import com.suixingpay.datas.manager.service.MrJobTasksMonitorService;
import com.suixingpay.datas.manager.web.message.ResponseMessage;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 任务泳道实时监控表 controller控制器
 *
 * @author: FairyHood
 * @date: 2018-03-07 17:26:55
 * @version: V1.0-auto
 * @review: FairyHood/2018-03-07 17:26:55
 */
@Api(description = "任务泳道实时监控表管理")
@RestController
@RequestMapping("/manager/mrjobtasksmonitor")
public class MrJobTasksMonitorController {

    @Autowired
    protected MrJobTasksMonitorService mrJobTasksMonitorService;

    @ApiOperation(value = "任务泳道实时数据(按分)", notes = "任务泳道实时数据(按分)")
    @GetMapping("/jobmonitor")
    public ResponseMessage jobMonitor(@RequestParam(value = "jobId", required = true) String jobId,
            @RequestParam(value = "swimlaneId", required = true) String swimlaneId,
            @RequestParam(value = "schemaTable", required = true) String schemaTable,
            @RequestParam(value = "intervalTime", required = true) Long intervalTime,
            @RequestParam(value = "intervalCount", required = true) Long intervalCount) {
        MrJobMonitor mrJobMonitor = mrJobTasksMonitorService.obMrJobMonitor(jobId, swimlaneId, schemaTable,
                intervalTime, intervalCount);
        return ResponseMessage.ok(mrJobMonitor);
    }

    /*
     * @PostMapping
     * 
     * @ApiOperation(value = "新增", notes = "新增") public ResponseMessage
     * add(@RequestBody MrJobTasksMonitor mrJobTasksMonitor) { Integer number =
     * mrJobTasksMonitorService.insert(mrJobTasksMonitor); return ok(number); }
     * 
     * @PutMapping("/{id}")
     * 
     * @ApiOperation(value = "修改", notes = "修改") public ResponseMessage
     * update(@PathVariable("id") Long id, @RequestBody MrJobTasksMonitor
     * mrJobTasksMonitor) { Integer number = mrJobTasksMonitorService.update(id,
     * mrJobTasksMonitor); return ok(number); }
     * 
     * @DeleteMapping("/{id}")
     * 
     * @ApiOperation(value = "删除", notes = "删除") public ResponseMessage
     * delete(@PathVariable("id") Long id) { mrJobTasksMonitorService.delete(id);
     * return ok(); }
     * 
     * @GetMapping("/{id}")
     * 
     * @ApiOperation(value = "查询明细", notes = "查询明细") public ResponseMessage
     * info(@PathVariable("id") Long id) { MrJobTasksMonitor mrJobTasksMonitor =
     * mrJobTasksMonitorService.selectById(id); return ok(mrJobTasksMonitor); }
     * 
     * @ApiOperation(value = "查询列表", notes = "查询列表")
     * 
     * @GetMapping public ResponseMessage list(@RequestParam(value = "pageNo",
     * required = false) Integer pageNo,
     * 
     * @RequestParam(value = "pageSize", required = false) Integer pageSize) {
     * Page<MrJobTasksMonitor> page = mrJobTasksMonitorService.page(new
     * Page<MrJobTasksMonitor>(pageNo, pageSize)); return ok(page); }
     */

}