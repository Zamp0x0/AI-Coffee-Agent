package cn.sg.intelligentcustomerservice.infrastructure.interfaces;

import cn.sg.intelligentcustomerservice.application.dto.WorkflowRunCMD;
import cn.sg.intelligentcustomerservice.application.service.WorkflowApplicationService;
import cn.sg.intelligentcustomerservice.common.lang.R;
import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;

    @PostMapping("/run")
    public R<String> run(@Valid @RequestBody WorkflowRunCMD cmd) {
        log.info("WorkflowController[]run 接收到工作流执行请求: {}", JSONObject.toJSONString(cmd));
        return R.success(workflowApplicationService.run(cmd));
    }
}
