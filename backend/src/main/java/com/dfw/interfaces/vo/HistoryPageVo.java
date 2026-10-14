package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;
import java.util.*;
/** 按游标分页的历史事实。 */
@Getter
@Setter
public class HistoryPageVo {
    /** 本页记录，按时间倒序。 */
    public List<HistoryEntryVo> entries = new ArrayList<>();
    /** 下一页序号游标。 */
    public Long nextCursor;
}
