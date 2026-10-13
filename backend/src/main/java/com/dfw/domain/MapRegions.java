package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import static com.dfw.domain.GameError.require;

/** 独立地区地名配置，目前只用于命名，不改变事件或经济规则。 */
@Getter
@Setter
public class MapRegions {
    /** 可被分配到整片街区的地区组。 */
    public List<Region> regions;
    /** 输入无，确认地区标识唯一且地名池非空。 */
    public void validate() {
        require(regions != null && !regions.isEmpty(), "地区配置不能为空"); Set<String> ids = new HashSet<>();
        for (Region region : regions) {
            require(region != null && region.id != null && !region.id.isBlank() && ids.add(region.id) && region.name != null && !region.name.isBlank(), "地区标识或名称不可用");
            require(region.names != null && !region.names.isEmpty() && region.names.stream().allMatch(n -> n != null && !n.isBlank()) && new HashSet<>(region.names).size() == region.names.size(), "地区地名不能为空或重复");
        }
    }
    /** 一组地区展示名和地点；地点不足时由生成器添加地段编号。 */
    @Getter
    @Setter
    public static class Region {
        /** 稳定地区标识，仅作元数据保存。 */
        public String id;
        /** 地区展示名称。 */
        public String name;
        /** 同一地区可抽取的地点名。 */
        public List<String> names;
    }
}
