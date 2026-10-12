package com.dfw.interfaces;

import com.dfw.application.RoomService;
import com.dfw.infrastructure.JsonCodec;
import com.dfw.interfaces.dto.RoomInput;
import com.dfw.interfaces.vo.SessionVo;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

/** 房间 HTTP 接口，只负责输入输出和调用应用服务。 */
@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomService rooms;
    private final JsonCodec json;
    /** 输入应用服务和 JSON 编解码器，构造 Controller。 */
    public RoomController(RoomService rooms, JsonCodec json) { this.rooms = rooms; this.json = json; }
    /** 返回进程健康状态。 */
    @GetMapping("/health")
    public Map<String, Boolean> health() { return Map.of("ok", true); }
    /** 输入可选昵称，返回新房间身份。 */
    @PostMapping("/rooms/create")
    public SessionVo create(@RequestBody JsonNode body) { RoomInput input = InputValidation.room(body, false, json); return rooms.create(input.name); }
    /** 输入房间号和可选昵称，返回玩家身份。 */
    @PostMapping("/rooms/join")
    public SessionVo join(@RequestBody JsonNode body) { RoomInput input = InputValidation.room(body, true, json); return rooms.join(input.code, input.name); }
    /** 输入房间号，返回观战身份。 */
    @PostMapping("/rooms/spectate")
    public SessionVo spectate(@RequestBody JsonNode body) { RoomInput input = InputValidation.room(body, true, json); return rooms.spectate(input.code); }
}
