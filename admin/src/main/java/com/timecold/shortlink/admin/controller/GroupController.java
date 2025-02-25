package com.timecold.shortlink.admin.controller;

import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.timecold.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.timecold.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接分组控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/short_link/admin")
public class GroupController {

    private final GroupService groupService;

    /**
     * 新增短链接分组
     */
    @PostMapping("/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam.getName());
        return Results.success();
    }
    /**
     * 查询短链接分组集合
     */
    @GetMapping("/group")
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        return Results.success(groupService.listGroup());
    }

    /**
     * 修改短链接分组名称
     */
    @PutMapping("/group")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam) {
        groupService.updateGroup(requestParam);
        return Results.success();
    }

    /**
     * 删除短链接分组
     */
    @DeleteMapping("/group/{gid}")
    public Result<Void> deleteGroup(@PathVariable("gid") Long gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }

    /**
     * 短链接分组排序
     */
    @PutMapping("/group/sort")
    public Result<Void> sortGroup(@RequestBody ShortLinkGroupSortReqDTO requestParam) {
        groupService.sortGroup(requestParam);
        return Results.success();
    }
}
