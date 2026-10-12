package com.dfw.interfaces;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 生产静态页面的 Vue 路由回退。 */
@Controller
public class PageController {
    /** 返回首页文件，供直接打开大厅或投屏 URL 使用。 */
    @GetMapping({"/", "/room/{code}", "/watch/{code}"})
    public String page() { return "forward:/index.html"; }
}
