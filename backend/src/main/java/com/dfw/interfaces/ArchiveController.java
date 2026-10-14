package com.dfw.interfaces;

import com.dfw.application.ArchiveService;
import com.dfw.interfaces.dto.ArchiveInput;
import com.dfw.interfaces.dto.SeatClaimInput;
import com.dfw.interfaces.vo.*;
import org.springframework.web.bind.annotation.*;

/** 固定存档码接口，所有业务请求采用 JSON POST 和独立输入输出实体。 */
@RestController
@RequestMapping("/api")
public class ArchiveController {
    private final ArchiveService archives;
    /** 输入存档服务，构造 HTTP 适配器。 */
    public ArchiveController(ArchiveService archives) { this.archives = archives; }
    /** 输入存档码与书签游标，返回档案索引。 */
    @PostMapping("/archives/query")
    public ArchiveVo query(@RequestBody ArchiveInput input) { return archives.query(input); }
    /** 输入存档码、筛选和游标，返回公开历史分页。 */
    @PostMapping("/archives/history")
    public HistoryPageVo history(@RequestBody ArchiveInput input) { return archives.history(input); }
    /** 输入存档码和保存点，返回只读公开局面。 */
    @PostMapping("/archives/snapshot")
    public ArchiveSnapshotVo snapshot(@RequestBody ArchiveInput input) { return archives.snapshot(input); }
    /** 输入玩家会话、版本与备注，返回当前标记的保存点。 */
    @PostMapping("/archives/save")
    public ArchiveSnapshotVo save(@RequestBody ArchiveInput input) { return archives.save(input); }
    /** 输入存档码、保存点、请求 ID 与角色，返回新房间会话。 */
    @PostMapping("/archives/restore")
    public SessionVo restore(@RequestBody ArchiveInput input) { return archives.restore(input); }
    /** 输入访客会话和目标角色，返回等待房主批准的申请。 */
    @PostMapping("/rooms/claim")
    public SeatClaimVo claim(@RequestBody SeatClaimInput input) { return archives.claim(input); }
}
